package orc.ast.extended.expression;

import orc.ast.extended.Visitor;
import orc.ast.simple.WithLocation;
import orc.error.compiletime.CompilationException;

public class Otherwise extends Expression {

	public Expression left;
	public Expression right;

	public Otherwise(Expression left, Expression right)
	{
		this.left = left;
		this.right = right;
	}
	
	@Override
	public orc.ast.simple.expression.Expression simplify() throws CompilationException {
		return new WithLocation(
				new orc.ast.simple.expression.Otherwise(left.simplify(), right.simplify()),
				getSourceLocation());
	}
	
	public String toString() {
		return "(" + left + " ; " + right + ")";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
