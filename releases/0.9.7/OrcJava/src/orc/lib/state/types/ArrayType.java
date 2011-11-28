package orc.lib.state.types;

import java.util.List;

import orc.error.compiletime.typing.TypeException;
import orc.type.ArrowType;
import orc.type.DotType;
import orc.type.ListType;
import orc.type.MutableContainerType;
import orc.type.Type;

public class ArrayType extends MutableContainerType {

	public String toString() {
		return "Array";
	}
	
	public Type makeCallableInstance(List<Type> params) throws TypeException {
		Type T = params.get(0);
		
		/* Default behavior is element reference retrieval */
		Type RefOfT = (new RefType()).instance(T);
		DotType arrayType = new DotType(new ArrowType(Type.INTEGER, RefOfT));

		Type ArrayOfT = (new ArrayType()).instance(T);
		arrayType.addField("get", new ArrowType(Type.INTEGER, T));
		arrayType.addField("set", new ArrowType(Type.INTEGER, T, Type.TOP));
		arrayType.addField("slice", new ArrowType(Type.INTEGER, Type.INTEGER, ArrayOfT));
		arrayType.addField("length", new ArrowType(Type.INTEGER));
		arrayType.addField("fill", new ArrowType(T, Type.TOP));
		return arrayType;
	}
	
}