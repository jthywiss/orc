package orc.type.structured;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.oil.expression.argument.Argument;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.TypingContext;

/**
 * A composite type supporting ad-hoc polymorphic calls.
 * 
 * Contains a list of types; when this type is used in call position,
 * it will be typechecked using each type in the list sequentially until
 * one succeeds.
 * 
 * 
 * @author dkitchin
 *
 */
public class MultiType extends Type {

	List<Type> alts;
	
	
	public MultiType(List<Type> alts) {
		this.alts = alts;
	}
	
	// binary case
	public MultiType(Type A, Type B) {
		this.alts = new LinkedList<Type>();
		alts.add(A);
		alts.add(B);
	}
	
	public boolean subtype(Type that) throws TypeException {
		
		for(Type alt : alts) {
			if (alt.subtype(that)) return true;
		}
		
		return false;
	}
	
	public Type call(TypingContext ctx, List<Argument> args, List<Type> typeActuals) throws TypeException {
		
		for(Type alt : alts) {
			try {
				return alt.call(ctx, args, typeActuals);
			}
			catch (TypeException e) {}
		}
		
		// TODO: Make this more informative
		throw new TypeException("Typing failed for call; no alternatives matched in " + this + ".");
	}
	
	public String toString() {
		
		StringBuilder s = new StringBuilder();
		
		s.append('(');
		for (int i = 0; i < alts.size(); i++) {
			if (i > 0) { s.append(" & "); }
			s.append(alts.get(i));
		}
		s.append(')');
		
		return s.toString();
	}
	
	public Set<Integer> freeVars() {
		return Type.allFreeVars(alts);
	}
}