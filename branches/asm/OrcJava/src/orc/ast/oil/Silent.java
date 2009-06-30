package orc.ast.oil;

import java.util.HashSet;
import java.util.Set;

import orc.ast.oil.xml.Expression;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.runtime.nodes.Node;
import orc.type.Type;

public class Silent extends Expr {

	public String toString() {
		return "stop";
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}

	@Override
	public Type typesynth(Env<Type> ctx, Env<Type> typectx) {
		return Type.BOT;
	}
	
	@Override
	public void typecheck(Type t, Env<Type> ctx, Env<Type> typectx) {
		// Do nothing. Silent checks against all types.
	}

	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		return;
	}

	@Override
	public Expression marshal() throws CompilationException {
		return new orc.ast.oil.xml.Silent();
	}
}
