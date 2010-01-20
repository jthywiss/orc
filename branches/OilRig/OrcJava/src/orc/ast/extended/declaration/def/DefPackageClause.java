//
// DefPackageClause.java -- Java class DefPackageClause
// Project OrcJava
//
// $Id$
//
// Created by amshali on Jan 17, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.extended.declaration.def;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import orc.ast.extended.declaration.Declaration;
import orc.ast.extended.declaration.DefsDeclaration;
import orc.ast.extended.expression.Call;
import orc.ast.extended.expression.Declare;
import orc.ast.extended.expression.Expression;
import orc.ast.extended.expression.HasType;
import orc.ast.extended.expression.Literal;
import orc.ast.extended.expression.Name;
import orc.ast.extended.expression.Otherwise;
import orc.ast.extended.expression.Parallel;
import orc.ast.extended.expression.Sequential;
import orc.ast.extended.expression.Stop;
import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.pattern.TypedPattern;
import orc.ast.extended.type.Type;
import orc.error.compiletime.CompilationException;

/**
 * 
 * 
 * @author amshali
 */
public class DefPackageClause
    extends DefMemberClause {

	/**
	 * Constructs an object of class DefPackageClause.
	 * 
	 * @param name
	 * @param formals
	 * @param body
	 * @param resultType
	 */
	public DefPackageClause(String name, List<List<Pattern>> formals, Expression body,
	    Type resultType) {
		super(name, formals, body, resultType);
	}

	public DefPackageClause(String name, List<List<Pattern>> formals, Expression body,
	    Type resultType, Boolean exported) {
		super(name, formals, body, resultType, exported);

	}

	@Override
	public void extend(AggregateDef adef) throws CompilationException {
		List<Pattern> phead = formals.get(0);
		List<Pattern> newformals = new LinkedList<Pattern>();
		List<Type> argTypes = new LinkedList<Type>();

		for (Pattern p : phead) {
			/* Strip a toplevel type ascription from every argument pattern */
			if (p instanceof TypedPattern) {
				TypedPattern tp = (TypedPattern) p;
				argTypes.add(tp.t);
				newformals.add(tp.p);
			} else {
				newformals = phead;

				/*
				 * There is at least one argument with a missing annotation. Request
				 * inference.
				 */
				argTypes = null;

				break;
			}
		}
		if (argTypes != null) {
			adef.setArgTypes(argTypes);
		}

		Expression newbody = body;
		if (formals.size() > 1) {
			List<List<Pattern>> ptail = formals.subList(1, formals.size());
			if (resultType != null) {
				newbody = new HasType(newbody, resultType);
			}
			newbody = Expression.uncurry(ptail, newbody);
		}

		if (resultType != null) {
			adef.setResultType(resultType);
		}

		if (newbody instanceof Declare) {
			makeNewBody((Declare) newbody, new ArrayList<String>());
		}
		// ///////////
		adef.addClause(new Clause(newformals, newbody));

		adef.addLocation(getSourceLocation());

	}

	private void makeNewBody(Declare declare, List<String> exportedDefs) {
		Declaration defs = declare.d;
    Expression e = declare.e;
		if (defs instanceof DefsDeclaration) {
			for (DefMember d : ((DefsDeclaration) defs).defs) {
				if (d instanceof DefMemberClause && ((DefMemberClause) d).exported) {
					exportedDefs.add(d.name);
				}
			}
		}
		if (e instanceof Declare) {
			makeNewBody((Declare) e, exportedDefs);
		} else {
			List<Expression> recordArgs = makeRecordArgs(exportedDefs);
			Call recordCall = new Call(new Name("Record"), recordArgs);
			Otherwise otherwise = new Otherwise(
			    new Sequential(e, new Stop()), recordCall);
			declare.e = otherwise;
		}
	}

	private List<Expression> makeRecordArgs(List<String> exportedDefs) {
		List<Expression> args = new ArrayList<Expression>();
		for (String s : exportedDefs) {
			args.add(new Literal(s));
			args.add(new Call(new Name("Site"), new Name(s)));
		}
		return args;
	}

}
