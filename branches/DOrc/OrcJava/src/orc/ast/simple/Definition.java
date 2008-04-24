package orc.ast.simple;

import java.util.List;
import java.util.Set;

import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.runtime.nodes.Node;

/**
 * 
 * A unit of syntax that encapsulates an expression definition. 
 * 
 * Groups of mutually recursive definitions are scoped in the simplified abstract syntax tree by a Def.
 * 
 * @author dkitchin
 *
 */

public class Definition {

	public Var name;
	public List<Var> formals;
	public Expression body;
	
	/**
	 * Note that the constructor takes a bound Var as a name parameter. This is because the
	 * binding of expression names occurs at the level of mutually recursive groups, not at
	 * the level of the individual definitions.
	 * 
	 * @param name
	 * @param formals
	 * @param body
	 */
	public Definition(Var name, List<Var> formals, Expression body)
	{
		this.name = name;
		this.formals = formals;
		this.body = body;
	}
	
	public Definition subst(Argument a, NamedVar x) {
		return new Definition(name, formals, body.subst(a, x));
	}
	
	// Does not validly compute the set of free vars if this definition is in a mutually recursive group.
	// That determination must be accounted for elsewhere.
	public Set<Var> vars()
	{
		Set<Var> freeset = body.vars();
		freeset.remove(name);
		freeset.removeAll(formals);
		return freeset;
	}
	
	
	public orc.runtime.nodes.Definition compile() {
		
		Node newbody = body.compile(new orc.runtime.nodes.Return());
		
		return new orc.runtime.nodes.Definition(name, formals, newbody);
	}
	
}
