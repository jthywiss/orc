package orc.ast.extended.pattern;

import orc.ast.extended.Visitor;
import orc.ast.extended.type.Type;
import orc.ast.simple.argument.NamedVar;
import orc.ast.simple.argument.Var;
import orc.ast.simple.expression.Expression;
import orc.error.compiletime.PatternException;

/**
 * 
 * A pattern with a type ascription.
 * 
 * FIXME: The pattern simplifier will occasionally ignore type ascriptions because they
 * are not ascribed to attachments.
 * 
 * @author dkitchin
 *
 */
public class TypedPattern extends Pattern {

	public Pattern p;
	public Type t;
	
	public TypedPattern(Pattern p, Type t) {
		this.p = p;
		this.t = t;
	}

	public boolean strict() {
		
		return p.strict();
	}

	@Override
	public void process(Var fragment, PatternSimplifier visitor)
			throws PatternException {
		
		visitor.ascribe(fragment, t);
		p.process(fragment, visitor);
	}
	
	public String toString() {
		return "(" + p + " :: " + t +")";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
