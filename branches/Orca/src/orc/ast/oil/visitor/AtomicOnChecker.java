//
// ExceptionsOnChecker.java -- Java class ExceptionsOnChecker
// Project OrcJava
//
// $Id$
//
// Created by matsuoka on Sep 7, 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.oil.visitor;

import java.util.LinkedList;

import orc.ast.oil.expression.Atomic;
import orc.ast.oil.expression.Expression;
import orc.ast.oil.expression.WithLocation;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;

/**
 * 
 *
 * @author dkitchin
 */
public class AtomicOnChecker extends Walker {

	private final LinkedList<CompilationException> problems = new LinkedList<CompilationException>();
	private SourceLocation location;

	public static void check(final Expression expr) throws CompilationException {
		final AtomicOnChecker checker = new AtomicOnChecker();
		expr.accept(checker);
		if (checker.problems.size() > 0) {
			throw checker.problems.getFirst();
		}
	}

	@Override
	public Void visit(final Atomic atomicExpr) {
		final CompilationException e = new CompilationException("'atomic' expression found, but atomic keyword is not enabled.");
		e.setSourceLocation(location);
		problems.add(e);
		return null;
	}

	@Override
	public Void visit(final WithLocation expr) {
		SourceLocation oldlocation = this.location;
		this.location = expr.location;
		this.enter(expr);
		expr.body.accept(this);
		this.leave(expr);
		this.location = oldlocation;
		return null;
	}

}
