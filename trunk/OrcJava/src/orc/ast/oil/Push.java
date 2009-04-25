package orc.ast.oil;

import java.util.Set;

import orc.ast.oil.xml.Expression;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.nodes.Assign;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Unwind;
import orc.type.Type;

public class Push extends Expr {

	public Expr left;
	public Expr right;
	
	/* An optional variable name, used for documentation purposes.
	 * It has no operational purpose, since the expression is already
	 * in deBruijn index form. 
	 */
	public String name;
	
	public Push(Expr left, Expr right, String name)
	{
		this.left = left;
		this.right = right;
		this.name = name;
	}
	
	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		left.addIndices(indices,depth); 
		right.addIndices(indices,depth+1); // Push binds a variable on the right
	}

	public String toString() {
		return "(" + left.toString() + " >> " + right.toString() + ")";
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
	
	@Override
	public Type typesynth(Env<Type> ctx, Env<Type> typectx) throws TypeException {
		Env<Type> rctx = ctx.clone();
		rctx.add(left.typesynth(ctx, typectx));
		return right.typesynth(rctx, typectx);
	}

	@Override
	public void typecheck(Type T, Env<Type> ctx, Env<Type> typectx) throws TypeException {
		Env<Type> rctx = ctx.clone();
		rctx.add(left.typesynth(ctx, typectx));
		right.typecheck(T, rctx, typectx);
	}

	@Override
	public Expression marshal() throws CompilationException {
		return new orc.ast.oil.xml.Push(left.marshal(), right.marshal(), name);
	}
}
