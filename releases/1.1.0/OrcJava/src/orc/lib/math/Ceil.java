//
// Ceil.java -- Java class Ceil
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.math;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.type.Type;
import orc.type.structured.ArrowType;

public class Ceil extends EvalSite {

	@Override
	public Object evaluate(final Args args) throws TokenException {
		final Number n = args.numberArg(0);
		final int i = n.intValue();
		return n.equals(i) ? i : i + 1;
	}

	@Override
	public Type type() {
		return new ArrowType(Type.NUMBER, Type.INTEGER);
	}
}