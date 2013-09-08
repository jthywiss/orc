//
// OrcVersion.java -- Java class OrcVersion
// Project Orchard
//
// $Id$
//
// Created by jthywiss on Jan 20, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.orchard;

import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.TokenException;
import orc.orchard.OrchardProperties;
import orc.types.Type;
import orc.values.sites.TypedSite;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.Types;

/**
 * Returns a name, version, URL, and copyright string for Orchard and Orc.
 *
 * @author jthywiss
 */
public class OrcVersion extends EvalSite implements TypedSite {

	/* (non-Javadoc)
	 * @see orc.values.sites.compatibility.EvalSite#evaluate(orc.values.sites.compatibility.Args)
	 */
	@Override
	public Object evaluate(Args args) throws TokenException {
		if (args.size() != 0) {
			throw new ArityMismatchException(0, args.size());
		}
		return OrchardProperties.getProperty("war.manifest.Implementation-Title") + ' ' +
			OrchardProperties.getProperty("war.manifest.Implementation-Version") +
			" rev. " + OrchardProperties.getProperty("war.manifest.SVN-Revision") + '\n' +
			orc.Main.orcImplName() + ' ' + orc.Main.orcVersion() + '\n' +
			orc.Main.orcURL() + '\n' + orc.Main.orcCopyright();
	}

	@Override
	public Type orcType() {
		return Types.function(Types.integer());
	}

}
