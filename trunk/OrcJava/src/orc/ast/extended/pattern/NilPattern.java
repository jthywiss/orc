package orc.ast.extended.pattern;

import orc.ast.extended.Visitor;
import orc.ast.simple.Call;
import orc.ast.simple.Expression;
import orc.ast.simple.WithLocation;
import orc.ast.simple.arg.Var;
import orc.error.compiletime.PatternException;
import xtc.util.Utilities;

public class NilPattern extends Pattern {
	
//	public Expression bind(Var u, Expression g) {
//		return g;
//	}
//
//	public Expression match(Var u) {
//		
//		return new Call(Pattern.ISNIL, u);
//	}

	@Override
	public void process(Var fragment, PatternSimplifier visitor)
			throws PatternException {
		
		Var nilp = new Var();
		visitor.assign(nilp, new WithLocation(
				Pattern.trynil(fragment),
				getSourceLocation()));
		visitor.require(nilp);
	}
	
	public String toString() {
		return "[]";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
