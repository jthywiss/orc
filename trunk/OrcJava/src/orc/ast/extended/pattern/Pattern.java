package orc.ast.extended.pattern;

import java.util.LinkedList;
import java.util.List;

import orc.ast.simple.Call;
import orc.ast.simple.Expression;
import orc.ast.simple.Let;
import orc.ast.simple.Parallel;
import orc.ast.simple.Semi;
import orc.ast.simple.Sequential;
import orc.ast.simple.Silent;
import orc.ast.simple.Where;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.Constant;
import orc.ast.simple.arg.Field;
import orc.ast.simple.arg.Site;
import orc.ast.simple.arg.Var;
import orc.error.compiletime.NonlinearPatternException;
import orc.error.compiletime.PatternException;
import orc.error.Locatable;
import orc.error.SourceLocation;


/**
 * 
 * Base interface for the abstract syntax of patterns.
 * 
 * Patterns exist only in the extended abstract syntax. They desugar into a
 * series of operations which terminate in variable bindings.
 * 
 * @author dkitchin
 * 
 */

public abstract class Pattern implements Locatable {
	private SourceLocation location = SourceLocation.UNKNOWN;

	/* Patterns are assumed to be strict unless set otherwise */
	public boolean strict() {
		return true;
	}
	
	/** 
	 * Visit a pattern recursively, creating two products:
	 * 
	 * An expression that will examine a value to determine
	 * whether it matches a pattern, building an output tuple
	 * of all value fragments which will be bound to variables.
	 * 
	 * An expression transformer that will examine such an
	 * output tuple and bind its elements to variables in
	 * a given expression.
	 * 
	 * @param fragment  A variable holding the current fragment of the value to be matched
	 * @param visitor   A visitor object which accumulates an expression and a transformer
	 * @throws PatternException  
	 */
	public abstract void process(Var fragment, PatternSimplifier visitor) throws PatternException;
	

	/**
	 * 
	 * A different entry point for process, taking only a source variable.
	 * Creates a new visitor, visits the pattern, and then returns that visitor. 
	 * @throws PatternException 
	 */
	public PatternSimplifier process(Var fragment) throws PatternException {
		PatternSimplifier pv = new PatternSimplifier();
		process(fragment, pv);
		return pv;
	}
	
	
	
	
	
	
	/* Sites often used in pattern matching */
	protected static Argument IF = new Site(orc.ast.sites.Site.IF);
	protected static Argument EQUAL = new Site(orc.ast.sites.Site.EQUAL);
	protected static Argument SOME = new Site(orc.ast.sites.Site.SOME);
	protected static Argument NONE = new Site(orc.ast.sites.Site.NONE);
	public static Argument TRYSOME = new Site(orc.ast.sites.Site.ISSOME);
	public static Argument TRYNONE = new Site(orc.ast.sites.Site.ISNONE);
	
	/* This site might be replaced by a special message */
	protected static Argument TRYCONS = new Site(orc.ast.sites.Site.TRYCONS);
	
	/* This site might not be necessary */
	protected static Argument TRYNIL = new Site(orc.ast.sites.Site.TRYNIL);
	
	/**
	 * 
	 * Construct an expression comparing two arguments. 
	 * The result expression returns a signal if the arguments are equal,
	 * and remains silent otherwise.
	 * 
	 * @param s  An argument to compare
	 * @param t  An argument to compare
	 * @return   An expression publishing a signal if s=t, silent otherwise
	 */
	public static Expression compare(Argument s, Argument t) {

		Var b = new Var();
		
		// s = t
		Expression test = new Call(EQUAL, s, t);
		
		// (s = t) >b> if(b)
		Expression comp = new Sequential(test, new Call(IF,b), b);
		
		return comp;
	}
	
	
	/**
	 * Construct an expression which publishes the ith element
	 * of tuple s. 
	 * 
	 * @param s An argument bound to a tuple
	 * @param i An index into a tuple (starting at 0)
	 * @return  An expression publishing s(i)
	 */
	public static Expression nth(Argument s, int i) {
		return new Call(s,new Constant(i));
	}
	
	/**
	 * 
	 * Constructs an expression which will try to deconstruct an
	 * argument as if it were a list. It publishes (h,t) if the 
	 * argument s is viewable as a list h:t, and remains silent
	 * otherwise.
	 * 
	 * @param s 
	 */
	public static Expression trycons(Argument s) {
		// TODO: Make trycons a special message
		return new Call(TRYCONS, s);
	}
	
	/**
	 * 
	 * Constructs an expression which tests whether the argument
	 * s can be treated as an empty list (nil). If so, it
	 * publishes a signal; otherwise it remains silent.
	 * 
	 * @param s
	 */
	public static Expression trynil(Argument s) {
		// TODO: Make trynil the inverse of trycons
		return new Call(TRYNIL, s);
	}

	/**
	 * 
	 * Construct an expression to determine whether the argument
	 * s may be viewed as a tuple of size n. If so, the expression
	 * publishes a signal; otherwise it remains silent.
	 * 
	 * @param s  Argument to test
	 * @param n  Target arity
	 */
	
	public static Expression trysize(Argument s, int n) {
		// TODO: Make this field name a special constant
		
		// s.fits
		Expression fits = new Call(s, new Field("fits"));
		
		// m(n)
		Var m = new Var();
		Expression invoke = new Call(m, new Constant(n));
		
		// s.fits >m> m(n)
		return new Sequential(fits, invoke, m); 
	}
	
	/**
	 * 
	 * Construct an expression which tries to find the inverse
	 * of the site m, and apply it to s. While the result will 
	 * depend on the site itself, as a guideline the inverse 
	 * should obey the following specification:
	 * 
	 * Let i = unapply(m,s).
	 * If s = m(x) for some x, then i(s) = x.
	 * Otherwise, i(s) remains silent.
	 * 
	 * Currently we find the inverse via a special message.
	 * 
	 * @param m  The site to unapply
	 * @param s  Argument to the inversion
	 */
	public static Expression unapply(Argument m, Argument s) {
		// TODO: Make this field name a special constant
		return new Call(m, new Field("?"));
	}
	
	
	/**
	 * Lifts a partial function to a total function, using
	 * the ; combinator to detect a refusal to respond,
	 * and publishing optional values instead of values.
	 * 
	 * <code> some(x) ; none </code>
	 * 
	 * @param x
	 */
	public static Expression lift(Var x) {
		return new Semi(new Call(SOME, x), new Call(NONE));
	}
	
	
	/**
	 * Constructs an optional case statement.
	 * 
	 * <code>
	 * case arg of
	 *   some(s) -> succ
	 * |  none   -> fail
	 * </code>
	 * 
	 * @param arg
	 * @param s
	 * @param succ
	 * @param fail
	 */
	public static Expression caseof(Var arg, Var s, Expression succ, Expression fail) {
				
		// trySome(arg) >s> succ
		Expression someb = new Sequential(new Call(TRYSOME, arg), succ, s);
		
		// tryNone(arg) >> fail
		Expression noneb = new Sequential(new Call(TRYNONE, arg), fail, new Var());
		
		// trySome... | tryNone...
		return new Parallel(someb, noneb);
	}	
	
	public void setSourceLocation(SourceLocation location) {
		this.location = location;
	}

	public SourceLocation getSourceLocation() {
		return location;
	}
}
