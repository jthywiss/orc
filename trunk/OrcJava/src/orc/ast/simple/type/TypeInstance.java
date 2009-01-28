package orc.ast.simple.type;

import java.util.LinkedList;
import java.util.List;

import orc.env.Env;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;
import orc.type.TypeApplication;
import orc.type.TypeVariable;

/**
 * A type instantiation with explicit type parameters: T[T,..,T]
 * 
 * @author dkitchin
 *
 */
public class TypeInstance extends Type {

	public String name;
	public List<Type> params;
	
	public TypeInstance(String name, List<Type> params) {
		this.name = name;
		this.params = params;
	}
	
	@Override
	public orc.type.Type convert(Env<String> env) {
		 
		List<orc.type.Type> ts = new LinkedList<orc.type.Type>();
		for (Type t : params) {
			ts.add(t.convert(env));
		}
		return new TypeApplication(new TypeVariable(env.search(name)), ts);
	}
	
	
	public String toString() {
		
		StringBuilder s = new StringBuilder();
		
		s.append(name);
		s.append('[');
		for (int i = 0; i < params.size(); i++) {
			if (i > 0) { s.append(", "); }
			s.append(params.get(i));
		}
		s.append(']');
		
		return s.toString();
	}	
	
}
