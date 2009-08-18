package orc.ast.extended.pattern;

import java.util.LinkedList;
import java.util.List;

import orc.ast.extended.Visitor;
import orc.ast.simple.Call;
import orc.ast.simple.Expression;
import orc.ast.simple.Parallel;
import orc.ast.simple.Sequential;
import orc.ast.simple.Where;
import orc.ast.simple.WithLocation;
import orc.ast.simple.arg.Constant;
import orc.ast.simple.arg.Field;
import orc.ast.simple.arg.Var;
import orc.error.compiletime.PatternException;

public class ConsPattern extends Pattern {

	public Pattern h;
	public Pattern t;
	
	public ConsPattern(Pattern h, Pattern t) {
		
		this.h = h;
		this.t = t;
	}
	
//	public Expression bind(Var u, Expression g) {
//	
//		g = h.bind(new Call(Pattern.HEAD, u), g);
//		g = t.bind(new Call(Pattern.TAIL, u), g);
//		return g;
//	}
//
//	public Expression match(Var u) {
//				
//		Var w = new Var();
//		Var vh = new Var();
//		Var vt = new Var();
//		
//		// Cons(vh,vt)
//		Var z = new Var();
//		Expression finalExpr = new Call(Pattern.CONS, vh, vt);
//		
//		// t.match w(1) -some(vt)-> ...
//		Expression tailExpr = Pattern.opbind(t.match(new Call(w, new Constant(1))), vt, finalExpr);
//		
//		// h.match w(0) -some(vh)-> (... >u> isSome(u))
//		Expression headExpr = Pattern.opbind(h.match(new Call(w, new Constant(0))), vh, Pattern.filter(tailExpr));
//		
//		// isCons(u) -some(w)-> (... >u> isSome(u))
//		Expression topExpr = Pattern.opbind(new Call(Pattern.ISCONS, u), w, Pattern.filter(headExpr));
//		
//		return topExpr;
//	}

	@Override
	public void process(Var fragment, PatternSimplifier visitor)
			throws PatternException {
		
		Var pair = new Var();
		visitor.assign(pair, new WithLocation(
				Pattern.trycons(fragment),
				getSourceLocation()));
		visitor.require(pair);
		
		Var head = new Var();
		visitor.assign(head, Pattern.nth(pair, 0));
		h.process(head, visitor);
		
		Var tail = new Var();
		visitor.assign(tail, Pattern.nth(pair, 1));
		t.process(tail, visitor);
	}
	
	public String toString() {
		return "(" + h + ":" + t +")";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
