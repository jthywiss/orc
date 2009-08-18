package orc.ast.extended;

import orc.ast.simple.WithLocation;
import orc.error.compiletime.CompilationException;

public class Parallel extends Expression {

	public Expression left;
	public Expression right;

	public Parallel(Expression left, Expression right)
	{
		this.left = left;
		this.right = right;
	}
	
	@Override
	public orc.ast.simple.Expression simplify() throws CompilationException {
		return new WithLocation(
				new orc.ast.simple.Parallel(left.simplify(), right.simplify()),
				getSourceLocation());
	}
	
	public String toString() {
		return "(" + left + " | " + right + ")";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
