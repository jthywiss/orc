//
// ExternalCPSDispatch.java -- Java class ExternalCPSDispatch
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.call;

import static orc.run.porce.SpecializationConfiguration.ExternalCPSDirectSpecialization;

import orc.DirectInvoker;
import orc.Invoker;
import orc.run.porce.RuntimeProfilerWrapper;
import orc.run.porce.SpecializationConfiguration;
import orc.run.porce.runtime.CPSCallContext;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.PorcERuntime;
import orc.run.porce.runtime.TailCallException;
import orc.run.porce.runtime.Terminator;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Instrumentable;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;

public class ExternalCPSDispatch extends Dispatch {
	@Child
	protected ExternalCPSDispatch.ExternalCPSDispatchInternal internal;

	protected ExternalCPSDispatch(final PorcEExecution execution) {
		super(execution);
		internal = ExternalCPSDispatch.ExternalCPSDispatchInternal.createBare(execution);
	}

	protected ExternalCPSDispatch(final ExternalCPSDispatch orig) {
		super(orig.internal.execution);
		internal = ExternalCPSDispatch.ExternalCPSDispatchInternal.createBare(orig.internal.execution);
	}
	 
	@Override
	public void setTail(boolean v) {
		super.setTail(v);
		internal.setTail(v);
	}
    
    @Override
    public void executeDispatch(VirtualFrame frame, Object target, Object[] arguments) {
        PorcEClosure pub = (PorcEClosure) arguments[0];
        Counter counter = (Counter) arguments[1];
        Terminator term = (Terminator) arguments[2];
        internal.execute(frame, target, pub, counter, term, buildArguments(0, arguments));
    }
    
    @Override
    public void executeDispatchWithEnvironment(VirtualFrame frame, Object target, Object[] arguments) {
        PorcEClosure pub = (PorcEClosure) arguments[1];
        Counter counter = (Counter) arguments[2];
        Terminator term = (Terminator) arguments[3];
        internal.execute(frame, target, pub, counter, term, buildArguments(1, arguments));
    }
	
	@SuppressWarnings({"boxing"})
	protected static Object[] buildArguments(int offset, Object[] arguments) {
		CompilerAsserts.compilationConstant(arguments.length);
		Object[] newArguments = new Object[arguments.length - 3 - offset];
		System.arraycopy(arguments, 3 + offset, newArguments, 0, newArguments.length);
		return newArguments;
	}
	
	static ExternalCPSDispatch createBare(PorcEExecution execution) {
		return new ExternalCPSDispatch(execution);
	}

	@ImportStatic({ SpecializationConfiguration.class })
	@Introspectable
	@Instrumentable(factory = ExternalCPSDispatchInternalWrapper.class)
	public static abstract class ExternalCPSDispatchInternal extends DispatchBase {
		protected ExternalCPSDispatchInternal(final PorcEExecution execution) {
			super(execution);
		}
		
		protected ExternalCPSDispatchInternal(final ExternalCPSDispatchInternal orig) {
			super(orig.execution);
		}
	
		protected final BranchProfile exceptionProfile = BranchProfile.create();
		
		@CompilerDirectives.CompilationFinal(dimensions = 1)
		protected ValueProfile[] argumentTypeProfiles = null;
		
		@CompilerDirectives.CompilationFinal
		protected InternalCPSDispatch.InternalCPSDispatchInternal dispatchP = null;
		
		protected InternalCPSDispatch.InternalCPSDispatchInternal getDispatchP() {
			if (dispatchP == null) {
				CompilerDirectives.transferToInterpreterAndInvalidate();
				computeAtomicallyIfNull(() -> dispatchP, (v) -> dispatchP = v, () -> {
					InternalCPSDispatch.InternalCPSDispatchInternal n = insert(InternalCPSDispatch.InternalCPSDispatchInternal.createBare(true, execution));
					n.setTail(isTail);
					return n;
				});
			}
			return dispatchP;
		}

		@SuppressWarnings("boxing")
		@ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_UNROLL)
		protected Object[] profileArgumentTypes(Object[] arguments) {
			CompilerAsserts.compilationConstant(arguments.length);
			
			if (argumentTypeProfiles == null) {
			    CompilerDirectives.transferToInterpreterAndInvalidate();
				computeAtomicallyIfNull(() -> argumentTypeProfiles, (v) -> argumentTypeProfiles = v, () -> {
					ValueProfile[] n = new ValueProfile[arguments.length];
					for(int i = 0; i < n.length; i++) {
						n[i] = ValueProfile.createClassProfile();
					}
					return n;
				});
			}
			
			for(int i = 0; i < arguments.length; i++) {
				arguments[i] = argumentTypeProfiles[i].profile(arguments[i]);
			}
			
			return arguments;
		}
		
	
		public abstract void execute(VirtualFrame frame, Object target, PorcEClosure pub, Counter counter, Terminator term, Object[] arguments);
	
		@Specialization(guards = { "ExternalCPSDirectSpecialization", "invoker != null", "canInvokeWithBoundary(invoker, target, profileArgumentTypes(arguments))" }, limit = "ExternalDirectCallMaxCacheSize")
		public void specificDirect(final VirtualFrame frame, final Object target, PorcEClosure pub, Counter counter, Terminator term, final Object[] arguments,
				@Cached("getDirectInvokerWithBoundary(target, arguments)") DirectInvoker invoker) {
			// DUPLICATION: This code is duplicated (mostly) in ExternalDirectDispatch.specific.
			try {
				final Object v;
				try {
					v = invokeDirectWithBoundary(invoker, target, profileArgumentTypes(arguments));
				} finally {
					RuntimeProfilerWrapper.traceExit(RuntimeProfilerWrapper.CallDispatch, getCallSiteId());
				}
				getDispatchP().execute(frame, pub, new Object[] { pub.environment, v });
			} catch (final TailCallException e) {
				throw e;
			} catch (final Throwable e) {
				exceptionProfile.enter();
				execution.notifyOfException(e, this);
				counter.haltToken();
			}
			// Token: All exception handlers halt the token that was passed to this
			// call. Calls are not allowed to keep the token if they throw an
			// exception.
		}
		
		@Specialization(guards = { "isNotDirectInvoker(invoker) || !ExternalCPSDirectSpecialization", "canInvokeWithBoundary(invoker, target, profileArgumentTypes(arguments))" }, limit = "ExternalCPSCallMaxCacheSize")
		public void specific(final VirtualFrame frame, final Object target, PorcEClosure pub, Counter counter, Terminator term, final Object[] arguments,
				@Cached("getInvokerWithBoundary(target, arguments)") Invoker invoker) {
			// Token: Passed to callContext from arguments.
			final CPSCallContext callContext = new CPSCallContext(execution, pub, counter, term, getCallSiteId());
	
			try {
				callContext.begin();
				invokeWithBoundary(invoker, callContext, target, profileArgumentTypes(arguments));
			} catch (final TailCallException e) {
				throw e;
			} catch (final Throwable e) {
                exceptionProfile.enter();
                execution.notifyOfException(e, this);
                counter.haltToken();
            } finally {
				RuntimeProfilerWrapper.traceExit(RuntimeProfilerWrapper.CallDispatch, getCallSiteId());
			}
		}
	
		@Specialization(replaces = { "specific", "specificDirect" })
		public void universal(final VirtualFrame frame, final Object target, PorcEClosure pub, Counter counter, Terminator term, final Object[] arguments,
				@Cached("createBinaryProfile()") ConditionProfile isDirectProfile) {
			final Invoker invoker = getInvokerWithBoundary(target, arguments);
			if (ExternalCPSDirectSpecialization && isDirectProfile.profile(invoker instanceof DirectInvoker)) {
				specificDirect(frame, target, pub, counter, term, arguments, (DirectInvoker) invoker);
			} else {
				specific(frame, target, pub, counter, term, arguments, invoker);
			}
		}
	
		static ExternalCPSDispatchInternal createBare(PorcEExecution execution) {
			return ExternalCPSDispatchFactory.ExternalCPSDispatchInternalNodeGen.create(execution);
		}
	
		/* Utilties */
		
		protected static boolean isNotDirectInvoker(final Invoker invoker) {
			return !(invoker instanceof DirectInvoker);
		}
	
		protected Invoker getInvokerWithBoundary(final Object target, final Object[] arguments) {
			return getInvokerWithBoundary(execution.runtime(), target, arguments);
		}
	
		protected DirectInvoker getDirectInvokerWithBoundary(final Object target, final Object[] arguments) {
			Invoker invoker = getInvokerWithBoundary(execution.runtime(), target, arguments);
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
	
		@TruffleBoundary(allowInlining = true, transferToInterpreterOnException = false)
		protected static void invokeWithBoundary(final Invoker invoker, final CPSCallContext callContext,
				final Object target, final Object[] arguments) {
			invoker.invoke(callContext, target, arguments);
		}
	
		@TruffleBoundary(allowInlining = true, transferToInterpreterOnException = false)
		protected static Object invokeDirectWithBoundary(final DirectInvoker invoker, final Object target,
				final Object[] arguments) {
			return invoker.invokeDirect(target, arguments);
		}
	}

}
