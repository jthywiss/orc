package orc.ast.extended;

import java.util.LinkedList;
import java.util.List;

import orc.ast.extended.pattern.Pattern;
import orc.ast.simple.arg.Var;

public class Lambda extends Expression {

	public List<Pattern> formals;
	public Expression body;
	
	public Lambda(List<Pattern> formals, Expression body) {
		this.formals = formals;
		this.body = body;
	}

	@Override
	public orc.ast.simple.Expression simplify() {
		// TODO Auto-generated method stub
		
		Var f = new Var();
		
		List<Var> params = new LinkedList<Var>();
		for(Pattern p : formals) {
			params.add(new Var());
		}
		
		Clause c = new Clause(formals, body);
		
		orc.ast.simple.Expression lambody = c.simplify(params,new orc.ast.simple.Silent());
		
		List<orc.ast.simple.Definition> defs = new LinkedList<orc.ast.simple.Definition>();
		defs.add(new orc.ast.simple.Definition(f,params,lambody));
		
		return new orc.ast.simple.Defs(defs, new orc.ast.simple.Let(f));		
	}

	public String toString() {
		return "(lambda (" + join(formals, ", ") + ") = " + body + ")";
	}
}
