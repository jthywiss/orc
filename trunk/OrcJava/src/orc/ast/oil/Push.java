package orc.ast.oil;

import java.util.Set;

import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.runtime.nodes.Assign;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Unwind;

public class Push extends Expr {

	Expr left;
	Expr right;
	
	public Push(Expr left, Expr right)
	{
		this.left = left;
		this.right = right;
	}
	
	@Override
	public Node compile(Node output) {
		return left.compile(new Assign(right.compile(new Unwind(output))));
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
	public orc.orchard.oil.Expression marshal() {
		return new orc.orchard.oil.Push(left.marshal(), right.marshal());
	}
}
