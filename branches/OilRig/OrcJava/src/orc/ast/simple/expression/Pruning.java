package orc.ast.simple.expression;

import java.util.Set;

import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.type.FreeTypeVariable;
import orc.ast.simple.type.Type;
import orc.ast.simple.type.TypeVariable;
import orc.env.Env;
import orc.error.compiletime.CompilationException;

public class Pruning extends Expression {

	Expression left;
	Expression right;
	Variable v;
	
	public Pruning(Expression left, Expression right, Variable v)
	{
		this.left = left;
		this.right = right;
		this.v = v;
	}
	
	@Override
	public Expression subst(Argument a, FreeVariable x) 
	{
		return new Pruning(left.subst(a,x), right.subst(a,x), v);
	}
	
	/* (non-Javadoc)
	 * @see orc.ast.simple.expression.Expression#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Expression subst(Type T, FreeTypeVariable X) {
		return new Pruning(left.subst(T,X), right.subst(T,X), v);
	}

	public Set<Variable> vars() {
		
		Set<Variable> s = left.vars();
		s.addAll(right.vars());
		s.remove(v);
		return s;
	}

	@Override
	public orc.ast.oil.expression.Expression convert(Env<Variable> vars, Env<TypeVariable> typevars) throws CompilationException {
		
		Env<Variable> newvars = vars.clone();
		newvars.add(v);
		
		return new orc.ast.oil.expression.Pruning(left.convert(newvars, typevars), right.convert(vars, typevars), v.name);
	}
	
	public String toString() {
		return "(" + left + " <" + v + "< " + right + ")";
	}
	
	
}
