//
// ListType.java -- Java class ListType
// Project OrcScala
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites.compatibility.type.structured;

import orc.error.compiletime.typing.TypeException;
import orc.values.sites.compatibility.type.Type;

public class ListType extends Type {

	@Override
	public String toString() {
		return "List";
	}

	public static Type listOf(final Type T) throws TypeException {
		return new ListType();//.instance(T);
	}

}
