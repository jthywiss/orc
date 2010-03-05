//
// Prompt.java -- Java class Prompt
// Project Orchard
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.orchard;

import orc.error.runtime.SiteException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.sites.Site;

/**
 * Ask the user a question and return their response.
 * This is designed to interact with the Orchard services
 * so that Orchard clients can handle the prompt.
 * @author quark
 */
public class Prompt extends Site {
	/**
	 * Interface implemented by an engine which can handle
	 * the Prompt site.
	 * @author quark
	 */
	public interface Promptable {
		public void prompt(String message, PromptCallback callback);
	}

	public interface PromptCallback {
		public void respondToPrompt(String response);

		public void cancelPrompt();
	}

	@Override
	public void callSite(final Args args, final Token caller) throws TokenException {
		final OrcEngine engine = caller.getEngine();
		final String prompt = args.stringArg(0);
		if (!(engine instanceof Promptable)) {
			caller.error(new SiteException("This Orc engine does not support the Prompt site."));
		}
		((Promptable) engine).prompt(prompt, new PromptCallback() {
			public void respondToPrompt(final String response) {
				caller.resume(response);
			}

			public void cancelPrompt() {
				caller.die();
			}
		});
	}
}
