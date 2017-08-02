package orc.run.porce;

import java.util.concurrent.locks.Lock;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import orc.CaughtEvent;
import orc.DirectInvoker;
import orc.ErrorInvoker;
import orc.Invoker;
import orc.error.runtime.ExceptionHaltException;
import orc.error.runtime.HaltException;
import orc.run.porce.ExternalCPSCall.Specific;
import orc.run.porce.ExternalCPSCall.Universal;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.PCTHandle;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecutionRef;
import orc.run.porce.runtime.PorcERuntime;
import orc.run.porce.runtime.Terminator;

class ExternalDirectCallBase extends CallBase {
	public ExternalDirectCallBase(Expression target, Expression[] arguments, PorcEExecutionRef execution) {
		super(target, arguments, execution);
	}

	protected Object[] buildArgumentValues(VirtualFrame frame) {
		final Object[] argumentValues = new Object[arguments.length];
		executeArguments(argumentValues, 0, 0, frame);
		return argumentValues;
	}

	protected DirectInvoker getInvokerWithBoundary(final Object t, final Object[] argumentValues) {
		return (DirectInvoker) getInvokerWithBoundary(getRuntime(), t, argumentValues);
	}

	@TruffleBoundary(allowInlining = true)
	protected static Invoker getInvokerWithBoundary(final PorcERuntime runtime, final Object t,
			final Object[] argumentValues) {
		return runtime.getInvoker(t, argumentValues);
	}

	@TruffleBoundary(allowInlining = true)
	protected static boolean canInvokeWithBoundary(final Invoker invoker, final Object t,
			final Object[] argumentValues) {
		return invoker.canInvoke(t, argumentValues);
	}

	@TruffleBoundary(allowInlining = true)
	protected static Object invokeDirectWithBoundary(final DirectInvoker invoker, final Object t,
			final Object[] argumentValues) {
		return invoker.invokeDirect(t, argumentValues);
	}
}

public class ExternalDirectCall extends ExternalDirectCallBase {
	private int cacheSize = 0;
	private static int cacheMaxSize = 4;

	public ExternalDirectCall(Expression target, Expression[] arguments, PorcEExecutionRef execution) {
		super(target, arguments, execution);
	}

	public Object execute(VirtualFrame frame) {
		CompilerDirectives.transferToInterpreterAndInvalidate();

		Object t = executeTargetObject(frame);
		Object[] argumentValues = buildArgumentValues(frame);
		CallBase n;

		try {
			DirectInvoker invoker = getInvokerWithBoundary(t, argumentValues);

			Lock lock = getLock();
			lock.lock();
			try {
				if (!(invoker instanceof ErrorInvoker) && cacheSize < cacheMaxSize) {
					cacheSize++;
					n = new Specific((Expression) target.copy(), invoker, copyExpressionArray(arguments),
							(CallBase) this.copy(), execution);
					replace(n, "Speculate on target closure.");
				} else {
					n = replaceWithUniversal();
				}
			} finally {
				lock.unlock();
			}
		} catch (Exception e) {
			execution.get().notifyOrc(new CaughtEvent(e));
			replaceWithUniversal();
			throw HaltException.SINGLETON();
		}
		return n.execute(frame);
	}

	private CallBase replaceWithUniversal() {
		CallBase n = new Universal(target, arguments, execution);
		findCacheRoot(this).replace(n,
				"Invoker cache too large or error getting invoker. Falling back to universal invocation.");
		return n;
	}

	private static CallBase findCacheRoot(CallBase n) {
		if (n.getParent() instanceof Specific) {
			return findCacheRoot((Specific) n.getParent());
		} else {
			return n;
		}
	}

	@Override
	public NodeCost getCost() {
		return NodeCost.UNINITIALIZED;
	}

	public static ExternalDirectCall create(Expression target, Expression[] arguments, PorcEExecutionRef execution) {
		return new ExternalDirectCall(target, arguments, execution);
	}

	public static class Specific extends ExternalDirectCallBase {
		private final DirectInvoker invoker;

		@Child
		protected Expression notMatched;

		public Specific(Expression target, DirectInvoker invoker, Expression[] arguments, Expression notMatched,
				PorcEExecutionRef execution) {
			super(target, arguments, execution);
			this.invoker = invoker;
			this.notMatched = notMatched;
		}

		public Object execute(VirtualFrame frame) {
			Object t = executeTargetObject(frame);
			Object[] argumentValues = buildArgumentValues(frame);

			if (canInvokeWithBoundary(invoker, t, argumentValues)) {
				try {
					return invokeDirectWithBoundary(invoker, t, argumentValues);
				} catch (ExceptionHaltException e) {
					execution.get().notifyOrc(new CaughtEvent(e.getCause()));
					throw HaltException.SINGLETON();
				} catch (HaltException e) {
					throw e;
				} catch (Exception e) {
					execution.get().notifyOrc(new CaughtEvent(e));
					throw HaltException.SINGLETON();
				}
			} else {
				return notMatched.execute(frame);
			}
		}

		@Override
		public NodeCost getCost() {
			return NodeCost.POLYMORPHIC;
		}
	}

	public static class Universal extends ExternalDirectCallBase {
		public Universal(Expression target, Expression[] arguments, PorcEExecutionRef execution) {
			super(target, arguments, execution);
		}

		@ExplodeLoop
		public Object execute(VirtualFrame frame) {
			Object t = executeTargetObject(frame);
			Object[] argumentValues = buildArgumentValues(frame);

			try {
				DirectInvoker invoker = getInvokerWithBoundary(t, argumentValues);
				return invokeDirectWithBoundary(invoker, t, argumentValues);
			} catch (ExceptionHaltException e) {
				execution.get().notifyOrc(new CaughtEvent(e.getCause()));
				throw HaltException.SINGLETON();
			} catch (HaltException e) {
				throw e;
			} catch (Exception e) {
				execution.get().notifyOrc(new CaughtEvent(e));
				throw HaltException.SINGLETON();
			}
		}

		@Override
		public NodeCost getCost() {
			return NodeCost.MEGAMORPHIC;
		}
	}
}