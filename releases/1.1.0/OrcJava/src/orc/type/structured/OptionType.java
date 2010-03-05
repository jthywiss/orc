//
// OptionType.java -- Java class OptionType
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

package orc.type.structured;

import orc.type.tycon.ImmutableContainerType;

public class OptionType extends ImmutableContainerType {

	@Override
	public String toString() {
		return "Option";
	}

}
