//
// DotSite.java -- Java class DotSite
// Project OrcJava
//
// $Id: DotSite.java 1502 2010-02-03 06:25:53Z jthywissen $
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites.compatibility;

import java.util.Map;
import java.util.TreeMap;

import orc.TokenAPI;
import orc.error.runtime.MessageNotUnderstoodException;
import orc.error.runtime.TokenException;
import orc.error.runtime.UncallableValueException;

/**
 * Dot-accessible sites should extend this class and declare their Orc-available
 * methods using <code>addMembers()</code>. The code is forward-compatible with many possible
 * optimizations on the field lookup strategy.
 * 
 * A dot site may also have a default behavior which allows it to behave like
 * a normal site. If its argument is not a message, it displays that 
 * default behavior, if implemented. If there is no default behavior, it
 * raises a type error.
 * 
 * @author dkitchin
 */
public abstract class DotSite extends SiteAdaptor  {

	Map<String, Object> methodMap;

	public DotSite() {
		methodMap = new TreeMap<String, Object>();
		this.addMembers();
	}

	@Override
	public void callSite(final Args args, final TokenAPI t) throws TokenException {

		String f;

		// Check if the argument is a message
		try {
			f = args.fieldName();
		} catch (final TokenException e) {
			// If not, invoke the default behavior and return.
			defaultTo(args, t);
			return;
		}

		// If it is a message, look it up.
		final Object m = getMember(f);
		if (m != null) {
			t.publish(object2value(m));
		} else {
			throw new MessageNotUnderstoodException(f);
		}
	}

	Object getMember(final String f) {
		return methodMap.get(f);
	}

	protected abstract void addMembers();

	protected void addMember(final String f, final Object s) {
		methodMap.put(f, s);
	}

	protected void defaultTo(final Args args, final TokenAPI token) throws TokenException {
		throw new UncallableValueException("This dot site has no default behavior; it only responds to messages.");
	}

}
