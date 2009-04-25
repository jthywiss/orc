package orc.type.tycon;

import java.util.List;

import orc.env.Env;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UnrepresentableTypeException;
import orc.type.Type;

/**
 * 
 * A type-level representation of a user-defined type alias with
 * type parameters. Essentially this is a user-defined type constructor;
 * the variances of the type parameters are inferred automatically
 * from the aliased type itself.
 * 
 * @author dkitchin
 *
 */
public class PolymorphicAliasedType extends Tycon {

	public List<Variance> inferredVariances;
	public Type type;
	
	public PolymorphicAliasedType(Type type, List<Variance> inferredVariances) {
		this.inferredVariances = inferredVariances;
		this.type = type;
	}
	
	/* Parametric type aliases instantiate to the aliased type,
	 * with the appropriate substitutions.
	 * 
	 * (non-Javadoc)
	 * @see orc.type.Tycon#instance(java.util.List)
	 */
	public Type instance(List<Type> params) throws TypeException {
		
		Env<Type> subctx = new Env<Type>();
		for (Type t : params) {
			subctx = subctx.extend(t);
		}
		
		return type.subst(subctx);
	}

	@Override
	public orc.ast.oil.xml.type.Type marshal() throws UnrepresentableTypeException {
		return new orc.ast.oil.xml.type.PolymorphicTypeAlias(type.marshal(), inferredVariances.size());
	}
}
