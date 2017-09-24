
package orc.run.porce.call;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;

import orc.CaughtEvent;
import orc.DirectInvoker;
import orc.Invoker;
import orc.error.runtime.ExceptionHaltException;
import orc.error.runtime.HaltException;
import orc.run.porce.SpecializationConfiguration;
import orc.run.porce.runtime.CPSCallContext;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecutionRef;
import orc.run.porce.runtime.PorcERuntime;
import orc.run.porce.runtime.TailCallException;
import orc.run.porce.runtime.Terminator;

@ImportStatic({ SpecializationConfiguration.class })
public abstract class ExternalCPSDispatch extends Dispatch {
	protected ExternalCPSDispatch(final PorcEExecutionRef execution) {
		super(execution);
	}

	@CompilerDirectives.CompilationFinal(dimensions = 1)
	protected final BranchProfile[] exceptionProfiles = new BranchProfile[] { BranchProfile.create(),
			BranchProfile.create(), BranchProfile.create() };
	
	@CompilerDirectives.CompilationFinal
	protected InternalCPSDispatch dispatchP = null;
	
	protected InternalCPSDispatch getDispatchP() {
		if (dispatchP == null) {
			CompilerDirectives.transferToInterpreterAndInvalidate();
			computeAtomicallyIfNull(() -> dispatchP, (v) -> dispatchP = v, () -> {
				InternalCPSDispatch n = InternalCPSDispatch.createBare(execution);
				n.setTail(isTail);
				return n;
			});
		}
		return dispatchP;
	}
	
	// FIXME: PERFORMANCE: There are three calls to buildArguments here. They
	// each allocate a new array and they cannot be eliminated by the compiler.
	// Somehow I need to move these checks into the body so I can compute only
	// once.
	//
	// The problem is that if I do that I can no longer use the automated
	// caching which is a significant development cost. Maybe I could eliminate
	// the new array compute on each call and then the compiler could eliminate
	// the repeated copy. The problem is that the allocated array would need to
	// be per CALL not per specialization-callsite. This means we cannot use
	// Cached.
	//
	// It may be possible to use a special child node which the specializations
	// can access as an argument. The problem is I don't know how to make sure
	// arguments is passed down from the parent call.

	@Specialization(guards = {"invoker != null", "canInvokeWithBoundary(invoker, target, buildArguments(arguments))" }, limit = "ExternalDirectCallMaxCacheSize")
	public void specificDirect(final VirtualFrame frame, final Object target, final Object[] arguments,
			@Cached("getDirectInvokerWithBoundary(target, buildArguments(arguments))") DirectInvoker invoker) {
		PorcEClosure pub = (PorcEClosure) arguments[0];
		Counter counter = (Counter) arguments[1];

		// DUPLICATION: This code is duplicated (mostly) in ExternalDirectDispatch.specific.
		try {
			final Object v = invokeDirectWithBoundary(invoker, target, buildArguments(arguments));
			getDispatchP().executeDispatch(frame, pub, new Object[] { v });
		} catch (final TailCallException e) {
			throw e;
		} catch (final ExceptionHaltException e) {
			exceptionProfiles[0].enter();
			execution.get().notifyOrcWithBoundary(new CaughtEvent(e.getCause()));
			counter.haltToken();
		} catch (final HaltException e) {
			exceptionProfiles[1].enter();
			counter.haltToken();
		} catch (final Exception e) {
			exceptionProfiles[2].enter();
			execution.get().notifyOrcWithBoundary(new CaughtEvent(e));
			counter.haltToken();
		}
		// Token: All exception handlers halt the token that was passed to this
		// call. Calls are not allowed to keep the token if they throw an
		// exception.
	}

	// FIXME: PERFORMANCE: See specificDirect FIXME.
	
	@Specialization(guards = {
			"canInvokeWithBoundary(invoker, target, buildArguments(arguments))" }, limit = "ExternalCPSCallMaxCacheSize")
	public void specific(final VirtualFrame frame, final Object target, final Object[] arguments,
			@Cached("getInvokerWithBoundary(target, buildArguments(arguments))") Invoker invoker) {
		PorcEClosure pub = (PorcEClosure) arguments[0];
		Counter counter = (Counter) arguments[1];
		Terminator term = (Terminator) arguments[2];

		// Token: Passed to callContext from arguments.
		final CPSCallContext callContext = new CPSCallContext(execution.get(), pub, counter, term, getCallSiteId());

		try {
			callContext.begin();
			invokeWithBoundary(invoker, callContext, target, buildArguments(arguments));
		} catch (final TailCallException e) {
			throw e;
		} catch (final ExceptionHaltException e) {
			exceptionProfiles[0].enter();
			execution.get().notifyOrcWithBoundary(new CaughtEvent(e.getCause()));
			counter.haltToken();
		} catch (final HaltException e) {
			exceptionProfiles[1].enter();
			counter.haltToken();
		} catch (final Exception e) {
			exceptionProfiles[2].enter();
			execution.get().notifyOrcWithBoundary(new CaughtEvent(e));
			counter.haltToken();
		}
	}

	@Specialization(replaces = { "specific", "specificDirect" })
	public void universal(final VirtualFrame frame, final Object target, final Object[] arguments,
			@Cached("createBinaryProfile()") ConditionProfile isDirectProfile) {
		final Invoker invoker = getInvokerWithBoundary(target, buildArguments(arguments));
		if (isDirectProfile.profile(invoker instanceof DirectInvoker)) {
			specificDirect(frame, target, arguments, (DirectInvoker) invoker);
		} else {
			specific(frame, target, arguments, invoker);
		}
	}

	static ExternalCPSDispatch createBare(PorcEExecutionRef execution) {
		return ExternalCPSDispatchNodeGen.create(execution);
	}

	/* Utilties */

	protected static Object[] buildArguments(Object[] arguments) {
		CompilerAsserts.compilationConstant(arguments.length);
		Object[] newArguments = new Object[arguments.length - 3];
		System.arraycopy(arguments, 3, newArguments, 0, newArguments.length);
		return newArguments;
	}

	protected Invoker getInvokerWithBoundary(final Object target, final Object[] arguments) {
		return getInvokerWithBoundary(execution.get().runtime(), target, arguments);
	}

	protected DirectInvoker getDirectInvokerWithBoundary(final Object target, final Object[] arguments) {
		Invoker invoker = getInvokerWithBoundary(execution.get().runtime(), target, arguments);
		if (invoker instanceof DirectInvoker) {
			return (DirectInvoker) invoker;
		} else {
			return null;
		}
	}
	
	@TruffleBoundary(allowInlining = true)
	protected static Invoker getInvokerWithBoundary(final PorcERuntime runtime, final Object target,
			final Object[] arguments) {
		return runtime.getInvoker(target, arguments);
	}

	@TruffleBoundary(allowInlining = true)
	protected static boolean canInvokeWithBoundary(final Invoker invoker, final Object target,
			final Object[] arguments) {
		return invoker.canInvoke(target, arguments);
	}

	@TruffleBoundary(allowInlining = true, throwsControlFlowException = true)
	protected static void invokeWithBoundary(final Invoker invoker, final CPSCallContext callContext,
			final Object target, final Object[] arguments) {
		invoker.invoke(callContext, target, arguments);
	}

	@TruffleBoundary(allowInlining = true, throwsControlFlowException = true)
	protected static Object invokeDirectWithBoundary(final DirectInvoker invoker, final Object target,
			final Object[] arguments) {
		return invoker.invokeDirect(target, arguments);
	}
}