//
// Parallel.java -- Java class Parallel
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.oil.expression;

import java.util.Set;

import orc.ast.oil.ContextualVisitor;
import orc.ast.oil.Visitor;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.TokenLimitReachedError;
import orc.runtime.Token;
import orc.type.Type;
import orc.type.TypingContext;

public class Parallel extends Expression {

	public Expression left;
	public Expression right;

	public Parallel(final Expression left, final Expression right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public void addIndices(final Set<Integer> indices, final int depth) {
		left.addIndices(indices, depth);
		right.addIndices(indices, depth);
	}

	@Override
	public String toString() {
		return "(" + left.toString() + " | " + right.toString() + ")";
	}

	@Override
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}

	public <E, C> E accept(final ContextualVisitor<E, C> cvisitor, final C initialContext) {
		return cvisitor.visit(this, initialContext);
	}

	@Override
	public Type typesynth(final TypingContext ctx) throws TypeException {

		final Type L = left.typesynth(ctx);
		final Type R = right.typesynth(ctx);
		return L.join(R);
	}

	@Override
	public void typecheck(final TypingContext ctx, final Type T) throws TypeException {

		left.typecheck(ctx, T);
		right.typecheck(ctx, T);
	}

	@Override
	public orc.ast.xml.expression.Expression marshal() throws CompilationException {
		return new orc.ast.xml.expression.Parallel(left.marshal(), right.marshal());
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.expression.Expression#populateContinuations()
	 */
	@Override
	public void populateContinuations() {
		left.setPublishContinuation(getPublishContinuation());
		right.setPublishContinuation(getPublishContinuation());
		setPublishContinuation(null);
		left.populateContinuations();
		right.populateContinuations();
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.expression.Expression#enter(orc.runtime.Token)
	 */
	@Override
	public void enter(final Token t) {
		Token forked;
		try {
			forked = t.fork();
		} catch (final TokenLimitReachedError e) {
			t.error(e);
			return;
		}
		left.enter(t.move(left));
		right.enter(forked.move(right));
	}
}
