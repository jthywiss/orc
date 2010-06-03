//
// TupleType.java -- Java class TupleType
// Project OrcJava
//
// $Id: TupleType.java 1502 2010-02-03 06:25:53Z jthywissen $
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.sites.compatibility.type.structured;

import java.util.LinkedList;
import java.util.List;

import orc.sites.compatibility.type.Type;

@SuppressWarnings("hiding")
public class TupleType extends Type {

	public List<Type> items;

	public TupleType(final List<Type> items) {
		this.items = items;
	}

	/* Convenience function for constructing pairs */
	public TupleType(final Type a, final Type b) {
		this.items = new LinkedList<Type>();
		this.items.add(a);
		this.items.add(b);
	}

	@Override
	public String toString() {

		final StringBuilder s = new StringBuilder();

		s.append('(');
		for (int i = 0; i < items.size(); i++) {
			if (i > 0) {
				s.append(", ");
			}
			s.append(items.get(i));
		}
		s.append(')');

		return s.toString();
	}
}
