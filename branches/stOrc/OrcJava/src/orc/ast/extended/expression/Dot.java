package orc.ast.extended.expression;

import orc.ast.extended.Visitor;
import orc.ast.simple.expression.WithLocation;
import orc.error.compiletime.CompilationException;

/**
 * 
 * A dot expression (e.g "C.put(4)"). 
 *     
 * This is interpreted as a chain of calls:
 * 
 *     C(`put`)(4)
 *     
 * where `put` is a special Field object.
 * 
 * @author dkitchin
 *
 */

public class Dot extends Expression {

	public Expression target;
	public String field;
	
	public Dot(Expression target, String field)
	{
		this.target = target;
		this.field = field;
	}
	
	@Override
	public orc.ast.simple.expression.Expression simplify() throws CompilationException {
		Call call = new Call(target, new Field(field));
		call.setSourceLocation(getSourceLocation());
		return call.simplify();
	}

	public String toString() {
		return target + "." + field;
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}