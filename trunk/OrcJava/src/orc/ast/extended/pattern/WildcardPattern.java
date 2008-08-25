package orc.ast.extended.pattern;

import orc.ast.simple.Call;
import orc.ast.simple.Expression;
import orc.ast.simple.arg.Var;

public class WildcardPattern extends Pattern {
	
	public Expression bind(Var u, Expression g) {
		return g;
	}

	public Expression match(Var u) {
		return new Call(Pattern.SOME, u);
	}

	public boolean strict() {
		return false;
	}

	@Override
	public void process(Var fragment, PatternSimplifier visitor) {
		// Do nothing.
	}

	
	public String toString() {
		return "_";
	}

}
