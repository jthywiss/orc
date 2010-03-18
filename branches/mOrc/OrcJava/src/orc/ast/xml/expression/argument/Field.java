//
// Field.java -- Java class Field
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

package orc.ast.xml.expression.argument;

import javax.xml.bind.annotation.XmlAttribute;

import orc.error.compiletime.CompilationException;

public class Field extends Argument {
	@XmlAttribute(required = true)
	public String name;

	public Field() {
	}

	public Field(final String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return super.toString() + "(" + name + ")";
	}

	@Override
	public orc.ast.oil.expression.argument.Argument unmarshal() throws CompilationException {
		return new orc.ast.oil.expression.argument.Field(name);
	}
}
