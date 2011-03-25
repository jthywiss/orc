package orc.ast.simple.arg;

import orc.runtime.values.Value;


/**
 * Named (implicitly, free) variables. All such variables embed a String key.
 * Equivalence on these variables is equality of the embedded string.
 * 
 * Like normal Vars, these occur in argument position. However, since they
 * can never be bound at runtime, they compile to dead nodes.
 * 
 * The subst method on simplified expressions can only substitute for
 * a named variable.
 * 
 * @author dkitchin
 */

public class NamedVar extends Argument implements Comparable<NamedVar> {
	
	String key;
	
	public NamedVar(String key)
	{
		this.key = key;
	}
	
	public Value asValue()
	{
		throw new Error("Free variable " + key + " can never be bound to a value.");
	}

	public int compareTo(NamedVar f) {
		String s = this.key;
		String t = f.key;
		return s.compareTo(t);
	}
	
	public boolean equals(Object o) {
		
		if (o instanceof NamedVar)
		{
			return (this.compareTo((NamedVar)o) == 0);
		}
		else
		{
			return this.equals(o);
		}
	}
}