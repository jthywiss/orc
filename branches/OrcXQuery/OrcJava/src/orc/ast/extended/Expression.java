package orc.ast.extended;

import orc.ast.simple.arg.Argument;

/**
 * Base class for the extended abstract syntax tree.
 * 
 * Extended expressions are produced by the parser with minimal processing. 
 * They desugar to a simplified set of expressions.
 * 
 * @author dkitchin, wcook
 */

public abstract class Expression {

	/**
	 * Simplify an expression which occurs in a call (non-nested) position.
	 * 
	 * @return The simplified expression
	 */
	public abstract orc.ast.simple.Expression simplify();
	
	/**
	 * Simplify an expression which occurs in an argument (nested) position.
	 * 
	 * Returns an Arg, which produces an Argument for the simplified call,
	 * and may also introduce a binder, in the case of a nested expression.
	 * 
	 * The default behavior is to act as a nested expression and produce
	 * a complexArg. Expressions which can validly occur in arg position on
	 * their own without a binder will override this behavior.
	 * 
	 * @return The argified form of the expression
	 */
	protected Arg argify()
	{
		return new complexArg(new orc.ast.simple.arg.Var(), this.simplify());
	}
	
	
	
	/**
	 * Variant type returned by the argify method.
	 */
	interface Arg 
	{ 
		/**
		 * Extracts the Argument component of the Arg.
		 * @return A simplified argument
		 */
		orc.ast.simple.arg.Argument asArg();
		/**
		 * Wraps a binder around the given expression if needed.
		 * @param e The target for the binder
		 * @return The wrapped expression
		 */
		orc.ast.simple.Expression bind(orc.ast.simple.Expression e);	
	}
	
	/**
	 * 
	 * A simpleArg embeds an argument, and does not introduce a binder.
	 * 
	 * @author dkitchin
	 *
	 */
	class simpleArg implements Arg
	{
		orc.ast.simple.arg.Argument a;
		
		
		public simpleArg(orc.ast.simple.arg.Argument a)
		{
			this.a = a;
		}
		
		public Argument asArg() {
			return a;
		}

		public orc.ast.simple.Expression bind(orc.ast.simple.Expression e) {
			return e;
		}
		
	}
	/**
	 * 
	 * A complexArg embeds a variable name and an expression. It represents
	 * the simplification of a nested expression: a new variable name is placed 
	 * in the argument position, and the entire call is wrapped in a where expression
	 * which evaluates the nested expression in parallel and binds it to that
	 * variable.
	 * 
	 * For example,
	 * 
	 * M(x, f | g)
	 * 
	 * becomes
	 * 
	 * M(x,y) where y in (f | g)
	 * 
	 * In this case, asArg returns Var(y) and bind(e) returns (e where y in (f|g))
	 * 
	 * @author dkitchin
	 *
	 */
	class complexArg implements Arg
	{
		orc.ast.simple.arg.Var v;
		orc.ast.simple.Expression nested;
		
		public complexArg(orc.ast.simple.arg.Var v, orc.ast.simple.Expression nested)
		{
			this.v = v;
			this.nested = nested;
		}
		
		public Argument asArg() {
			return v;
		}

		public orc.ast.simple.Expression bind(orc.ast.simple.Expression e) {
			return new orc.ast.simple.Where(e,nested,v);
		}
	}
	
}