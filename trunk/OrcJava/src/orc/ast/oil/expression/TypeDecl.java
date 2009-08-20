package orc.ast.oil.expression;

import java.util.Set;

import orc.ast.oil.Visitor;
import orc.ast.oil.xml.Expression;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.nodes.Node;
import orc.type.Type;

/**
 * 
 * Bind a type in the given scope.
 * 
 * @author dkitchin
 *
 */
public class TypeDecl extends Expr {

	public Type type;
	public Expr body;
	
	public TypeDecl(Type type, Expr body) {
		this.type = type;
		this.body = body;
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
		return body.typesynth(ctx, typectx.extend(actualType));
	}

	public void typecheck(Type T, Env<Type> ctx, Env<Type> typectx) throws TypeException {
		Type actualType = type.subst(typectx);
		body.typecheck(T, ctx, typectx.extend(actualType));
	}

	@Override
	public Expression marshal() throws CompilationException {
		return new orc.ast.oil.xml.TypeDeclaration(type.marshal(), body.marshal());
	}
}
