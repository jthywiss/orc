package orc.ast.oil;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.oil.arg.Arg;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.runtime.nodes.Node;

public class Call extends Expr {

	public Arg callee;
	public List<Arg> args;
	
	public Call(Arg callee, List<Arg> args)
	{
		this.callee = callee;
		this.args = args;
	}
	
	/* Binary call constructor */
	public Call(Arg callee, Arg arga, Arg argb)
	{
		this.callee = callee;
		this.args = new LinkedList<Arg>();
		this.args.add(arga);
		this.args.add(argb);
	}
	
	/* Unary call constructor */
	public Call(Arg callee, Arg arg)
	{
		this.callee = callee;
		this.args = new LinkedList<Arg>();
		this.args.add(arg);
	}
	
	/* Nullary call constructor */
	public Call(Arg callee)
	{
		this.callee = callee;
		this.args = new LinkedList<Arg>();
	}
	

	@Override
	public Node compile(Node output) {
		orc.runtime.nodes.Call c = new orc.runtime.nodes.Call(callee, args, output);
		return c;
	}

	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		
		callee.addIndices(indices, depth);
		for (Arg arg : args) {
			arg.addIndices(indices, depth);
		}
	}
	
	public String toString() {
		
		String arglist = " ";
		for (Arg a : args) {
			arglist += a + " ";
		}
	
		return callee.toString() + "(" + arglist + ")";
	}
	
	@Override
	public orc.orchard.oil.Expression marshal() {
		LinkedList<orc.orchard.oil.Argument> arguments
			= new LinkedList<orc.orchard.oil.Argument>();
		for (Arg a : args) {
			arguments.add(a.marshal());
		}
		return new orc.orchard.oil.Call(callee.marshal(), arguments.toArray(new orc.orchard.oil.Argument[]{}));
	}
}
