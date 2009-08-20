package orc.ast.oil.expression;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import orc.ast.oil.Visitor;
import orc.ast.oil.expression.argument.Var;
import orc.ast.sites.JavaSite;
import orc.ast.sites.OrcSite;
import orc.env.Env;
import orc.error.Locatable;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.nodes.Pub;
import orc.type.Type;

/**
 * Base class for the portable (.oil, for Orc Intermediate Language) abstract syntax tree.
 * 
 * @author dkitchin
 *
 */

public abstract class Expr {
	/* Typechecking */
	
	/* Given a context, infer this expression's type */
	public abstract Type typesynth(Env<Type> ctx, Env<Type> typectx) throws TypeException;
	
	
	/* Check that this expression has type t in the given context. 
	 * 
	 * Some expressions will always have inferred types, so
	 * the default checking behavior is to infer the type and make
	 * sure that the inferred type is a subtype of the checked type.
	 */
	public void typecheck(Type T, Env<Type> ctx, Env<Type> typectx) throws TypeException {
		Type S = typesynth(ctx, typectx);
		if (!S.subtype(T)) {
			throw new SubtypeFailureException(S,T);
		}
	}

	
	
	/**
	 * Find the set of free variables in this expression. 
	 * 
	 * @return 	The set of free variables.
	 */
	public final Set<Var> freeVars() {
		Set<Integer> indices = new TreeSet<Integer>();
		this.addIndices(indices, 0);
		
		Set<Var> vars = new TreeSet<Var>();
		for (Integer i : indices) {
			vars.add(new Var(i));
		}
		
		return vars;
	}
	
	/**
	 * If this expression has any indices which are >= depth,
	 * add (index - depth) to the index set accumulator. The depth 
	 * increases each time this method recurses through a binder.
	 * 
	 * The default implementation is to assume the expression
	 * has no free variables, and thus do nothing. Expressions
	 * which contain variables or subexpressions override this
	 * behavior.
	 * 
	 * @param indices   The index set accumulator.
	 * @param depth    The minimum index for a free variable.
	 */
	public abstract void addIndices(Set<Integer> indices, int depth);
	
	public abstract <E> E accept(Visitor<E> visitor);
	
	public abstract orc.ast.oil.xml.Expression marshal() throws CompilationException;
}