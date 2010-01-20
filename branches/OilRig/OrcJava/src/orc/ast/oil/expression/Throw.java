package orc.ast.oil.expression;

import java.util.Set;

import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.TokenException;
import orc.runtime.Token;
import orc.type.Type;
import orc.type.TypingContext;
import orc.ast.oil.ContextualVisitor;
import orc.ast.oil.TokenContinuation;
import orc.ast.oil.Visitor;
import orc.ast.oil.expression.argument.Argument;
import orc.ast.oil.expression.argument.Variable;

public class Throw extends Expression {
	
	public Expression exception;
	
	public Throw(Expression e){
		exception = e;
	}
	
	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		exception.addIndices(indices, depth);
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
	
	public <E,C> E accept(ContextualVisitor<E,C> cvisitor, C initialContext) {
		return cvisitor.visit(this, initialContext);
	}
	
	@Override
	public Type typesynth(TypingContext ctx) throws TypeException {
		/* TODO: thrown type = join of thrown type and this synthesized type */ 
		exception.typesynth(ctx);
		
		// throw e : Bot, so long as e is typable.
		return Type.BOT;
	}
	
	public orc.ast.xml.expression.Expression marshal() throws CompilationException {
		return new orc.ast.xml.expression.Throw(exception.marshal());
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.expression.Expression#populateContinuations()
	 */
	@Override
	public void populateContinuations() {
		TokenContinuation K = new TokenContinuation() {
			
			public void execute(Token t) {
				Object o = t.getResult();
				
				try {
					t.throwException(o);
				}
				catch (TokenException e) {
					t.error(e);
				}
			}
		};
		exception.setPublishContinuation(K);
		exception.populateContinuations();
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.expression.Expression#enter(orc.runtime.Token)
	 */
	@Override
	public void enter(Token t) {
		exception.enter(t);
	}
}
