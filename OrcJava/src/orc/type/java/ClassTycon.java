package orc.type.java;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import orc.ast.oil.arg.Arg;
import orc.env.Env;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.tycon.Tycon;
import orc.type.tycon.Variance;

public class ClassTycon extends Tycon {

	public Class cls;
	
	public ClassTycon(Class cls) {
		this.cls = cls;
	}
	
	@Override
	public boolean subtype(Type that) throws TypeException {

		// All tycons are subtypes of Top
		if (that.isTop()) { return true; }
		
		if (that instanceof ClassTycon) {
			ClassTycon ct = (ClassTycon)that;
			
			// If this is not a generic class, just check Java subtyping.
			if (cls.getTypeParameters().length == 0) {
				return ct.cls.isAssignableFrom(cls);
			}
			
			// Otherwise, check for class equality.
			return ct.cls.equals(cls);
		}
		
		return false;
	}

	@Override
	public List<Variance> variances() throws TypeException {
		/* 
		 * All Java type parameters should be considered invariant, to be safe.
		 */
		List<Variance> vs = new LinkedList<Variance>();
		for(int i = 0; i < cls.getTypeParameters().length; i++) {
			vs.add(Variance.INVARIANT);
		}
		return vs;
	}

	public Type makeCallableInstance(List<Type> params) {		
		return new CallableJavaInstance(cls, Type.makeJavaCtx(cls, params));
	}
	
	public String toString() {
		return cls.getName().toString();
	}

}





class CallableJavaInstance extends Type {
	
	Class cls;
	Map<java.lang.reflect.TypeVariable, Type> javaCtx;
	
	public CallableJavaInstance(Class cls, Map<java.lang.reflect.TypeVariable, Type> javaCtx) {
		this.cls = cls;
		this.javaCtx = javaCtx;
	}
	
	@Override
	public Type call(Env<Type> ctx, Env<Type> typectx, List<Arg> args,
			List<Type> typeActuals) throws TypeException {
				
		String f = Arg.asField(args);
		
		if (f != null) {
			List<Method> matchingMethods = new LinkedList<Method>();
			for (Method m : cls.getMethods()) {
				if (m.getName().equals(f)) 
				{
					matchingMethods.add(m);	
				}
			}

			if (!matchingMethods.isEmpty()) {
				return Type.fromJavaMethods(matchingMethods, javaCtx);
			}
			else {
				// No method matches. Try fields.
				for (java.lang.reflect.Field fld : cls.getFields()) {
					if (fld.getName().equals(f)) {
						return Type.fromJavaType(fld.getGenericType(), javaCtx);
					}
				}
				
				// Neither a method nor a field
				throw new TypeException("'" + f + "' is not a member of " + cls.getName());
			}
		} 
		else {
			throw new TypeException("Java objects have no default site behavior. Use a method call.");
		}
		
		
	}
	
}
