package orc.ast.extended.pattern;

import java.util.LinkedList;
import java.util.List;

import orc.ast.simple.Call;
import orc.ast.simple.Expression;
import orc.ast.simple.Where;
import orc.ast.simple.arg.*;

public class TuplePattern extends Pattern {

	List<Pattern> args;
	
	public TuplePattern(List<Pattern> args) {
		this.args = args;
	}
	
	public Expression bind(Var u, Expression g) {
		
		for(int i = 0; i < args.size(); i++) {
			Pattern p = args.get(i);
			Expression ui = new Call(u, new Constant(i));
			g = p.bind(ui, g);
		}
		
		return g;
	}

	public Expression match(Var u) {
	
		// lift(..., pi.match( u(i) ) ,...) 
		List<Expression> es = new LinkedList<Expression>();
		for(int i = 0; i < args.size(); i++) {
			Pattern p = args.get(i);
			Expression ui = new Call(u, new Constant(i));
			es.add(p.match(ui));
		}
		Expression liftExpr = Pattern.lift(es);
		
		// u.fits
		Expression sizeExpr = new Call(u, new Field("fits"));
		
		// u.fits(n), where n is the tuple pattern size
		Var s = new Var();
		Argument n = new Constant(args.size());
		Expression fitsExpr = new Where(new Call(s, n), sizeExpr, s);
		
		// if u.fits(n) then lift(...) else none()
		return Pattern.ifexp(fitsExpr, liftExpr, new Call(Pattern.NONE)); 
	}

	public boolean strict() {
		return true;
	}
	
	public String toString() {
		return "("+orc.ast.extended.Expression.join(args, ", ")+")";
	}
}
