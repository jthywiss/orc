//
// Greater.java -- Java class Greater
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

package orc.lib.comp;

/**
 * @author dkitchin
 *
 */
public class Greater extends ComparisonSite {
	@Override
	public boolean compare(final int a) {
		return a > 0;
	}
}
