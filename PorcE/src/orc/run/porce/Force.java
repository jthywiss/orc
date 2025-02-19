//
// Force.java -- Truffle node Force
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import static orc.run.porce.SpecializationConfiguration.InlineForceHalted;
import static orc.run.porce.SpecializationConfiguration.InlineForceResolved;

import orc.FutureState;
import orc.run.porce.call.Dispatch;
import orc.run.porce.profiles.ResettableBranchProfile;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.Join;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.Terminator;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;

public class Force {
    public static boolean isNonFuture(final Object v) {
	return !(v instanceof orc.Future);
    }

    public static Dispatch createCall(PorcEExecution execution) {
        Dispatch n = Dispatch.createInternal(execution);
        n.forceInline();
        return n;
    }

    @SuppressWarnings("serial")
    private static final class ValueAvailable extends ControlFlowException {
	public final Object value;

	public ValueAvailable(final Object value) {
	    this.value = value;
	}
    }

    protected static final class HandleFuture {
	final ResettableBranchProfile boundFuture = ResettableBranchProfile.create();
	final ResettableBranchProfile unboundFuture = ResettableBranchProfile.create();
	final ResettableBranchProfile haltFuture = ResettableBranchProfile.create();
	private final ConditionProfile orcFuture = ConditionProfile.createBinaryProfile();
	private final ConditionProfile porcEFuture = ConditionProfile.createBinaryProfile();
	private final ConditionProfile nonFuture = ConditionProfile.createBinaryProfile();

	public Object handleFuture(PorcENode self, final Object future) {
	    CompilerAsserts.compilationConstant(self);
	    CompilerAsserts.compilationConstant(this);
	    CompilerAsserts.compilationConstant(nonFuture);

	    try {
		if (InlineForceResolved && nonFuture.profile(isNonFuture(future))) {
		    throw new ValueAvailable(future);
		} else if (porcEFuture.profile(future instanceof orc.run.porce.runtime.Future)) {
		    final Object state = ((orc.run.porce.runtime.Future) future).getInternal();
		    if (InlineForceResolved && !(state instanceof orc.run.porce.runtime.FutureConstants.Sentinel)) {
			if (boundFuture.enter()) {
			    unboundFuture.reset();
			}
			throw new ValueAvailable(state);
		    } else {
			unboundFuture.enter();
			if (InlineForceHalted && state == orc.run.porce.runtime.FutureConstants.Halt) {
			    return orc.run.porce.runtime.FutureConstants.Halt;
			} else {
			    return orc.run.porce.runtime.FutureConstants.Unbound;
			}
		    }
		} else if (orcFuture.profile(future instanceof orc.Future)) {
		    final FutureState state = ((orc.Future) future).get();
		    if (InlineForceResolved && state instanceof orc.FutureState.Bound) {
			if (boundFuture.enter()) {
			    unboundFuture.reset();
			}
			throw new ValueAvailable(((orc.FutureState.Bound) state).value());
		    } else {
			unboundFuture.enter();
			if (InlineForceHalted && state == orc.run.porce.runtime.FutureConstants.Orc_Stopped) {
			    return orc.run.porce.runtime.FutureConstants.Halt;
			} else {
			    return orc.run.porce.runtime.FutureConstants.Unbound;
			}
		    }
		} else if (!InlineForceResolved || !InlineForceHalted) {
                    if (isNonFuture(future)) {
                        return future;
                    } else {
                        return orc.run.porce.runtime.FutureConstants.Unbound;
                    }
		} else {
		    throw InternalPorcEError.unreachable(self);
		}
	    } catch (final ValueAvailable e) {
		return e.value;
	    }
	}

	@Override
	public String toString() {
	    StringBuilder sb = new StringBuilder();
	    sb.append("HandleFuture(");
	    sb.append("nonFuture=");
	    sb.append(nonFuture);
	    sb.append(",porcEFuture=");
	    sb.append(porcEFuture);
	    sb.append(",orcFuture=");
	    sb.append(orcFuture);
	    sb.append(",boundFuture=");
	    sb.append(boundFuture);
	    sb.append(",unboundFuture=");
	    sb.append(unboundFuture);
	    sb.append(",haltFuture=");
	    sb.append(haltFuture);
	    sb.append(")");
	    return sb.toString();
	}

	public static HandleFuture create() {
	    return new HandleFuture();
	}
    }

    @NodeChild(value = "p", type = Expression.class)
    @NodeChild(value = "c", type = Expression.class)
    @NodeChild(value = "t", type = Expression.class)
    public static class New extends Expression {
	private final PorcEExecution execution;
	private final int nFutures;

	public New(PorcEExecution execution, final int nFutures) {
	    this.execution = execution;
	    this.nFutures = nFutures;
	}

	@Specialization
	public Object run(final PorcEClosure p, final Counter c, final Terminator t) {
	    final Object[] values = new Object[nFutures + 1];
	    return new Join(p, c, t, values, execution);
	}

	public static New create(final Expression p, final Expression c, final Expression t, final int nFutures,
		final PorcEExecution execution) {
	    return ForceFactory.NewNodeGen.create(execution, nFutures, p, c, t);
	}
    }

    @NodeChild(value = "join", type = Expression.class)
    @NodeChild(value = "future", type = Expression.class)
    @ImportStatic({ Force.class })
    @Introspectable
    public static class Future extends Expression {
	protected final PorcEExecution execution;
	private final int index;


	public Future(PorcEExecution execution, int index) {
	    this.execution = execution;
	    this.index = index;
	}

	@Specialization(guards = { "isNonFuture(future)" })
	public PorcEUnit nonFuture(final Join join, final Object future) {
	    join.set(index, future);
	    return PorcEUnit.SINGLETON;
	}

	@Specialization
	public PorcEUnit porceFuture(final Join join, final orc.run.porce.runtime.Future future,
		@Cached("create()") HandleFuture handleFuture) {
	    Object v = handleFuture.handleFuture(this, future);

	    if (v == orc.run.porce.runtime.FutureConstants.Halt) {
		handleFuture.haltFuture.enter();
		join.setHaltedST();
	    } else if (v == orc.run.porce.runtime.FutureConstants.Unbound) {
		handleFuture.unboundFuture.enter();
		join.force(index, future);
	    } else {
		handleFuture.boundFuture.enter();
		join.set(index, v);
	    }

	    return PorcEUnit.SINGLETON;
	}

	@Specialization(replaces = { "porceFuture" })
	public PorcEUnit unknown(final Join join, final orc.Future future) {
	    join.force(index, future);
	    return PorcEUnit.SINGLETON;
	}

	public static Future create(final Expression join, final Expression future, final int index, PorcEExecution execution) {
	    return ForceFactory.FutureNodeGen.create(execution, index, join, future);
	}
    }

    @NodeChild(value = "join", type = Expression.class)
    @Introspectable
    @ImportStatic({ SpecializationConfiguration.class, Force.class })
    public static abstract class Finish extends Expression {
	protected static final boolean TRUE = true;

	protected final PorcEExecution execution;

	public Finish(PorcEExecution execution) {
	    this.execution = execution;
	}

	@Specialization(guards = { "join.isBlocked()", "TRUE" })
	public PorcEUnit blocked(final VirtualFrame frame,
	        final Join join,
	        @Cached("create(1, execution)") FlushAllCounters flushAllCounters) {
	    // Flush positive counters because this may trigger our continuation to execute in another thread.
	    flushAllCounters.execute(frame);
	    join.finishBlocked();
	    return PorcEUnit.SINGLETON;
	}

	@Specialization(guards = { "InlineForceResolved", "join.isResolved()" }, replaces = { "blocked" })
	public PorcEUnit resolved(final VirtualFrame frame, final Join join,
	        @Cached("createCall(execution)") Dispatch call) {
	    ensureTail(call);
	    call.executeDispatchWithEnvironment(frame, join.p(), join.values());
	    return PorcEUnit.SINGLETON;
	}

	@Specialization(guards = { "InlineForceHalted", "join.isHalted()" }, replaces = { "blocked" })
	public PorcEUnit halted(final VirtualFrame frame, final Join join,
                @Cached("create(execution)") HaltToken.KnownCounter haltToken) {
	    haltToken.execute(frame, join.c());
	    return PorcEUnit.SINGLETON;
	}

	@Specialization(guards = { "join.isBlocked()" })
	public PorcEUnit blockedAgain(final VirtualFrame frame,
	        final Join join,
	        @Cached("create(1, execution)") FlushAllCounters flushAllCounters) {
	    return blocked(frame, join, flushAllCounters);
	}

	@Specialization(guards = { "!InlineForceResolved || !InlineForceHalted" })
	public PorcEUnit fallback(final Join join) {
	    join.finish();
	    return PorcEUnit.SINGLETON;
	}

	public static Finish create(final Expression join, final PorcEExecution execution) {
	    return ForceFactory.FinishNodeGen.create(execution, join);
	}
    }

    @NodeChild(value = "p", type = Expression.class)
    @NodeChild(value = "c", type = Expression.class)
    @NodeChild(value = "t", type = Expression.class)
    @NodeChild(value = "future", type = Expression.class)
    @ImportStatic({ Force.class })
    @Introspectable
    public static abstract class SingleFuture extends Expression {
	@Child
	protected Dispatch call;

	protected final PorcEExecution execution;

	protected SingleFuture(final PorcEExecution execution) {
	    super();
	    this.execution = execution;
	    this.call = createCall(execution);
	}

	protected abstract Expression getC();

	@Override
	public void setTail(boolean v) {
	    super.setTail(v);
	    call.setTail(v);
	}

	protected HaltToken createHaltToken() {
	    return HaltToken.create((Expression)getC().deepCopy(), execution);
	}

	@Specialization
	public Object run(final VirtualFrame frame, final PorcEClosure p, final Counter c, final Terminator t,
		final Object _future,
		@Cached("create()") HandleFuture handleFuture,
		@Cached("createClassProfile()") ValueProfile futureTypeProfile,
		@Cached("createHaltToken()") HaltToken haltToken,
		@Cached("create(1, execution)") FlushAllCounters flushAllCounters) {
	    Object future = futureTypeProfile.profile(_future);
	    Object v = handleFuture.handleFuture(this, future);

	    if (v == orc.run.porce.runtime.FutureConstants.Halt) {
		handleFuture.haltFuture.enter();
		haltToken.execute(frame);
	    } else if (v == orc.run.porce.runtime.FutureConstants.Unbound) {
		handleFuture.unboundFuture.enter();
		// Force flushes because p could be called in another thread at any time.
		flushAllCounters.execute(frame);
		((orc.Future) future).read(new orc.run.porce.runtime.SingleFutureReader(p, c, t, execution));
		// ((orc.run.porce.runtime.Future) future).read(new orc.run.porce.runtime.SingleFutureReader(p, c, t, execution));
	    } else {
		handleFuture.boundFuture.enter();
		call.dispatch(frame, p, v );
	    }

	    return PorcEUnit.SINGLETON;
	}

	public static SingleFuture create(final Expression p, final Expression c, final Expression t,
		final Expression future, final PorcEExecution execution) {
	    return ForceFactory.SingleFutureNodeGen.create(execution, p, c, t, future);
	}
    }
}
