//
// Rtimer.java -- Java class Rtimer
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

package orc.lib.time;

import java.util.TimerTask;

import orc.TokenAPI;
import orc.error.runtime.TokenException;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.SiteAdaptor;
import orc.run.extensions.SupportForRtimer;
import orc.values.sites.compatibility.Types;
import orc.values.sites.TypedSite;
import orc.types.Type;
import orc.types.RecordType;


/**
 * Implements the RTimer site
 * @author wcook, quark, dkitchin
 */
public class Rtimer extends SiteAdaptor implements TypedSite {
    public void populateMetaDataAdaptor(final Args args, final TokenAPI caller) throws TokenException {
      long t = args.longArg(0);
      if (t == 0) vtime = 0;
    }

    private int vtime = -1;
    @Override
    public int virtualTime() {
      return vtime;
    }

	@Override
	public void callSite(final Args args, final TokenAPI caller) throws TokenException {
		String f;
		try {
			f = args.fieldName();
		} catch (final TokenException e) {
		  SupportForRtimer runtime = (SupportForRtimer)caller.runtime();
		  // default behavior is to wait
		  runtime.getTimer().schedule(new TimerTask() {
				@Override
				public void run() {
					caller.publish(signal());
				}
			}, args.longArg(0));
			return;
		}
		if (f.equals("time")) {
			caller.publish(new EvalSite() {
				@Override
				public Object evaluate(final Args evalArgs) throws TokenException {
					return new Long(System.currentTimeMillis());
				}
			});
		} else {
			throw new NoSuchMethodError(f + " in " + name());
		}
	}
	
	@Override
	public Type orcType() {
	  RecordType t = Types.dotSite(Types.function(Types.integer(), Types.signal()));
	  t = Types.addField(t, "time", Types.function(Types.integer()));
	  return t;
	}
	
}
