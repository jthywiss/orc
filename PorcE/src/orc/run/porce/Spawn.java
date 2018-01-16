
package orc.run.porce;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;

import orc.run.porce.call.Dispatch;
import orc.run.porce.call.InternalCPSDispatch;
import orc.run.porce.runtime.CallClosureSchedulable;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.Terminator;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.PorcERuntime;

@NodeChild(value = "c", type = Expression.class)
@NodeChild(value = "t", type = Expression.class)
@NodeChild(value = "computation", type = Expression.class)
@Introspectable
public abstract class Spawn extends Expression {
    private final PorcEExecution execution;
	private final boolean mustSpawn;
    
    protected Spawn(boolean mustSpawn, PorcEExecution execution) {
		this.mustSpawn = mustSpawn;
		this.execution = execution;
	}

    @Specialization(guards = { "!shouldInlineSpawn(computation)" } )
    public PorcEUnit spawn(final VirtualFrame frame, final Counter c, final Terminator t, final PorcEClosure computation) {
		t.checkLive();
		if (CompilerDirectives.inInterpreter() && computation.body.getRootNode() instanceof PorcERootNode) {
			//Logger.info(() -> "Spawning call: " + computation + ", body =  " + computation.body.getRootNode() + " (" + computation.body.getRootNode().getClass() + "), getTimePerCall() = " + computation.getTimePerCall());
			((PorcERootNode)computation.body.getRootNode()).incrementSpawn();
		}
		execution.runtime().schedule(CallClosureSchedulable.apply(computation, execution));
        return PorcEUnit.SINGLETON;
    }

    @Specialization(guards = { "shouldInlineSpawn(computation)" }, replaces = { "spawn" })
    public PorcEUnit inline(final VirtualFrame frame, final Counter c, final Terminator t, final PorcEClosure computation, 
    		@Cached("makeCall()") Dispatch call, @Cached("create()") BranchProfile spawnProfile) {
    	// Here we can inline spawns speculatively if we have not done that too much on this stack.
    	// This is very heuristic and may cause load imbalance problems in some cases.
		
    	if (PorcERuntime.incrementAndCheckStackDepth()) {
    		// This check should not go in shouldInlineSpawn because it has side effects and I don't think we can guarantee that guards are not called multiple times.
			try {
				call.executeDispatch(frame, computation, new Object[] {});
			} finally {
				PorcERuntime.decrementStackDepth();
			}
			return PorcEUnit.SINGLETON;
    	} else {
    		spawnProfile.enter();
    		return spawn(frame, c, t, computation);
    	}
    }

    // This duplication of "spawn" allows this node to specialize to only inline and then switch back to both later by adding this specialization.
    @Specialization(guards = { "!shouldInlineSpawn(computation)" } )
    public PorcEUnit spawnAfterInline(final VirtualFrame frame, final Object c, final Terminator t, final PorcEClosure computation) {
		return spawn(frame, (Counter)c, t, computation);
    }

    private static final boolean allowSpawnInlining = PorcERuntime.allowSpawnInlining();
    private static final boolean allowAllSpawnInlining = PorcERuntime.allowAllSpawnInlining();
    
	protected boolean shouldInlineSpawn(final PorcEClosure computation) {
		return allowSpawnInlining && (!mustSpawn || allowAllSpawnInlining) &&
			computation.getTimePerCall() < SpecializationConfiguration.InlineAverageTimeLimit;
	}
    
    protected Dispatch makeCall() {
		Dispatch n = insert(InternalCPSDispatch.create(true, execution, isTail));
		n.setTail(isTail);
		return n;
    }

    public static Spawn create(final Expression c, final Expression t, final boolean mustSpawn, final Expression computation, final PorcEExecution execution) {
        return SpawnNodeGen.create(mustSpawn, execution, c, t, computation);
    }
}
