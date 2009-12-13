package orc.ast.oil.expression;

import java.util.Set;

import orc.ast.oil.ContextualVisitor;
import orc.ast.oil.Visitor;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.nodes.Leave;
import orc.runtime.nodes.Node;
import orc.type.Type;
import orc.type.TypingContext;

public class Otherwise extends Expression {

	public Expression left;
	public Expression right;
	
	public Otherwise(Expression left, Expression right)
	{
		this.left = left;
		this.right = right;
	}

	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		left.addIndices(indices, depth);
		right.addIndices(indices, depth);
	}
	
	public String toString() {
		return "(" + left.toString() + " ; " + right.toString() + ")";
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
	
	public <E,C> E accept(ContextualVisitor<E,C> cvisitor, C initialContext) {
		return cvisitor.visit(this, initialContext);
	}
	
	
	@Override
	public Type typesynth(TypingContext ctx) throws TypeException {
		Type L = left.typesynth(ctx);
		TypingContext rctx = ctx;
		if (L.isSecurityLabeled()) {
			rctx = rctx.addControlFlowDependency(L.asSecurityLabeledType().label);
		}
		Type R = right.typesynth(rctx);
		return L.join(R);
	}

	
	@Override
	public void typecheck(TypingContext ctx, Type T) throws TypeException {
		left.typecheck(ctx, T);
		right.typecheck(ctx, T);
	}

	@Override
	public orc.ast.xml.expression.Expression marshal() throws CompilationException {
		return new orc.ast.xml.expression.Otherwise(left.marshal(), right.marshal());
	}
}
