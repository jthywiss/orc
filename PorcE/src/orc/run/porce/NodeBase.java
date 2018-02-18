package orc.run.porce;

import java.util.function.*;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;

import orc.ast.porc.PorcAST;
import orc.run.porce.runtime.SourceSectionFromPorc;
import orc.ast.ASTWithIndex;
import orc.run.porce.HasPorcNode;
import orc.run.porce.instruments.ProfiledPorcNodeTag;
import orc.run.porce.instruments.ProfiledPorcENodeTag;
import orc.run.porce.instruments.TailTag;
import scala.Option;

public abstract class NodeBase extends Node implements HasPorcNode {
	@CompilationFinal
    private Option<PorcAST> porcNode = Option.apply(null);

	public void setPorcAST(final PorcAST ast) {
		CompilerAsserts.neverPartOfCompilation();
		porcNode = Option.apply(ast);
		section = SourceSectionFromPorc.apply(ast);
		getChildren().forEach((n) -> {
			if (n instanceof NodeBase) {
				final NodeBase e = (NodeBase) n;
				if (e.porcNode().isEmpty()) {
					e.setPorcAST(ast);
				}
			}
		});
	}

    @Override
    public Option<PorcAST> porcNode() {
        return porcNode;
    }
    
    @CompilationFinal
    private SourceSection section = null;

    @Override
    public SourceSection getSourceSection() {
        return section;
    }


    @Override
    protected void onReplace(final Node newNode, final CharSequence reason) {
        if (newNode instanceof PorcENode) {
        	PorcENode n = (PorcENode) newNode;
        	if (porcNode().isDefined()) {
	            n.setPorcAST(porcNode().get());
	        }
        	n.setTail(n.isTail);
        }
        super.onReplace(newNode, reason);
    }
    
    @Override
    public Node copy() {
    	Node n = super.copy();
    	//((NodeBase)n).porcNode = Option.apply(null);
    	return n;
    }
    
	@Override
	protected boolean isTaggedWith(Class<?> tag) {
		if (tag == TailTag.class) {
			return isTail;
		} else if (tag == ProfiledPorcNodeTag.class) {
			return porcNode().isDefined() && ProfiledPorcNodeTag.isProfiledPorcNode(porcNode().get());
		} else if (tag == ProfiledPorcENodeTag.class) {
			return ProfiledPorcENodeTag.isProfiledPorcENode(this);
		} else {
			return super.isTaggedWith(tag);
		}
	}
	
	@CompilationFinal
	protected boolean isTail = false;
	
	public void setTail(boolean v) {
		isTail = v;
	}

	@CompilerDirectives.CompilationFinal
	private int callSiteId = -1;

	protected int getCallSiteId() {
		if (callSiteId >= 0) {
			return callSiteId;
		} else {
			CompilerDirectives.transferToInterpreterAndInvalidate();
			callSiteId = findCallSiteId(this);
			return callSiteId;
		}
	}

	/**
	 * Climb the Truffle AST searching for a node with a PorcAST with an index.
	 */
	private int findCallSiteId(final Node e) {
		if (e instanceof HasPorcNode) {
			HasPorcNode pn = (HasPorcNode) e;
			if (pn.porcNode().isDefined()) {
				final PorcAST ast = pn.porcNode().get();
				if (ast instanceof ASTWithIndex && ((ASTWithIndex) ast).optionalIndex().isDefined()) {
					return ((Integer) ((ASTWithIndex) ast).optionalIndex().get()).intValue();
				}
			}
		}
		final Node p = e.getParent();
		if (p instanceof NodeBase) {
			return ((NodeBase) p).getCallSiteId();
		} else if (p != null) {
			return findCallSiteId(p);
		}
		return -1;
	}

	/**
	 * Compute a value if it is currently null. This operation is performed atomically w.r.t.
	 * the RootNode of this Truffle AST.
	 * 
	 * This function should never be called from Truffle compiled code. For some reason, it
	 * causes a really opaque truffle error (a FrameWithoutBoxing is materialized, but it's
	 * not clear why).
	 * 
	 * @param read		a function to get the current value.
	 * @param write		a function to store a computed value.
	 * @param compute	a function to compute the value when needed.
	 */
	protected <T extends Node> void computeAtomicallyIfNull(Supplier<T> read, Consumer<T> write, Supplier<T> compute) {
		CompilerDirectives.bailout("computeAtomicallyIfNull is called from compiled code. This will not work correctly.");
		atomic(() -> {
			if (read.get() == null) {
				T v = compute.get();
				// TODO: Use the new Java 9 fence when we start requiring Java 9
				// for PorcE.
				UNSAFE.fullFence();
				write.accept(v);
			}
		});
	}	
	
	protected static sun.misc.Unsafe UNSAFE;
	static {
		try {
			java.lang.reflect.Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			UNSAFE = (sun.misc.Unsafe) theUnsafe.get(null);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			throw new Error(e);
		}
	}
}
