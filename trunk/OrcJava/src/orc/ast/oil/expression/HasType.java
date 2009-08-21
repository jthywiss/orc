package orc.ast.oil.expression;

import java.util.Set;

import orc.ast.oil.Visitor;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.nodes.Node;
import orc.type.Type;


/**
 * 
 * An expression with an ascribed type.
 * 
 * @author dkitchin
 *
 */
public class HasType extends Expression {

	public Expression body;
	public Type type;
	public boolean checkable; // set to false if this is a type assertion, not a type ascription
	
	public HasType(Expression body, Type type, boolean checkable) {
		this.body = body;
		this.type = type;
		this.checkable = checkable;
	}

	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}

	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		body.addIndices(indices, depth);
	}

	@Override
	public Type typesynth(Env<Type> ctx, Env<Type> typectx) throws TypeException {
		
		Type actualType = type.subst(typectx);
		
		/* If this ascription can be checked, check it */ 
		if (checkable) {
			body.typecheck(actualType, ctx, typectx);
		}
		/* If not, it is a type assertion, so we do not check it. */
	
		return actualType;
	}

	@Override
	public orc.ast.xml.expression.Expression marshal() throws CompilationException {
		return new orc.ast.xml.expression.HasType(body.marshal(),
				type.marshal(), checkable);
	}
}