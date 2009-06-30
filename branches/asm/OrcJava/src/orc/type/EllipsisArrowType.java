package orc.type;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.type.ground.Top;

public class EllipsisArrowType extends Type {

	public Type repeatedArgType;
	public Type resultType;
		
	public EllipsisArrowType(Type repeatedArgType, Type resultType) {
		this.repeatedArgType = repeatedArgType; 
		this.resultType = resultType;
	}

	protected ArrowType makeArrow(int arity) {

		List<Type> argTypes = new LinkedList<Type>();
		
		for(int i = 0; i < arity; i++) {
			argTypes.add(repeatedArgType);
		}

		return new ArrowType(argTypes, resultType);
	}

		
	public boolean subtype(Type that) throws TypeException {
		
		if (that instanceof Top) { return true; }
		
		if (that instanceof EllipsisArrowType) {
			EllipsisArrowType thatEA = (EllipsisArrowType)that;
			
			return (  thatEA.repeatedArgType.subtype(this.repeatedArgType)
				   && this.resultType.subtype(thatEA.resultType) );
		}
		else if (that instanceof ArrowType) {
			ArrowType thatArrow = (ArrowType)that;
			ArrowType thisArrow = makeArrow(thatArrow.argTypes.size());
			
			return thisArrow.subtype(thatArrow);
		}
		else {
			return false;
		}
	}
		

	public Type join(Type that) throws TypeException {	
			
		if (that instanceof EllipsisArrowType) {
			EllipsisArrowType thatEA = (EllipsisArrowType)that;

			Type joinRAT = repeatedArgType.meet(thatEA.repeatedArgType);
			Type joinRT = resultType.join(thatEA.resultType);
			return new EllipsisArrowType(joinRAT, joinRT);
		}
		else if (that instanceof ArrowType) {
			ArrowType thatArrow = (ArrowType)that;
			ArrowType thisArrow = makeArrow(thatArrow.argTypes.size());

			return thisArrow.join(thatArrow);
		}
		else {
			return Type.TOP;
		}
	}
		
	public Type meet(Type that) throws TypeException {	
		
		if (that instanceof EllipsisArrowType) {
			EllipsisArrowType thatEA = (EllipsisArrowType)that;

			Type joinRAT = repeatedArgType.join(thatEA.repeatedArgType);
			Type joinRT = resultType.meet(thatEA.resultType);
			return new EllipsisArrowType(joinRAT, joinRT);
		}
		else if (that instanceof ArrowType) {
			ArrowType thatArrow = (ArrowType)that;
			ArrowType thisArrow = makeArrow(thatArrow.argTypes.size());

			return thisArrow.meet(thatArrow);
		}
		else {
			return Type.TOP;
		}
	}
		
		
	public Type call(List<Type> args) throws TypeException {
			
		for (Type T : args) {
			if (!(T.subtype(repeatedArgType))) {
				throw new SubtypeFailureException(T, repeatedArgType);
			}
		}
				
		return resultType;
	}
	
	public Set<Integer> freeVars() {
		
		Set<Integer> vars = repeatedArgType.freeVars();
		vars.addAll(resultType.freeVars());
		
		return vars;
	}
		
		
	public String toString() {
			
		StringBuilder s = new StringBuilder();
			
		s.append('(');
		s.append(repeatedArgType);
		s.append("... -> ");
		s.append(resultType);
		s.append(')');
			
		return s.toString();
	}
	
}
