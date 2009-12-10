package orc.ast.oil.expression;

import java.util.Set;

import orc.ast.oil.ContextualVisitor;
import orc.ast.oil.Visitor;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Store;
import orc.runtime.nodes.Unwind;
import orc.type.Type;
import orc.type.TypingContext;

public class Pruning extends Expression {

	public Expression left;
	public Expression right;
	
	/* An optional variable name, used for documentation purposes.
	 * It has no operational purpose, since the expression is already
	 * in deBruijn index form. 
	 */
	public String name;
	
	public Pruning(Expression left, Expression right, String name)
	{
		this.left = left;
		this.right = right;
		this.name = name;
	}
	
	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		left.addIndices(indices,depth+1); // Pull binds a variable on the left
		right.addIndices(indices,depth);
	}

	public String toString() {
		return "(" + left.toString() + " << " + right.toString() + ")";
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
		TypingContext ctx2 = ctx.clone(); // Fresh TypingContext for lhs

		Type rtype = right.typesynth(ctx);
		return left.typesynth(ctx2.bindVar(rtype));
	}

	@Override
	public void typecheck(TypingContext ctx, Type T) throws TypeException {
		Type rtype = right.typesynth(ctx);
		left.typecheck(ctx.bindVar(rtype), T);
	}

	@Override
	public orc.ast.xml.expression.Expression marshal() throws CompilationException {
		return new orc.ast.xml.expression.Pruning(left.marshal(), right.marshal(), name);
	}
}
