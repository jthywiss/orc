//
// HttpPatternMatchListener.java -- Java class HttpPatternMatchListener
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Aug 13, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.launch;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.console.IPatternMatchListenerDelegate;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;

/**
 * Receives messages when a TextConsole's content matches the regex
 * supplied in plugin.xml, and creates a hyperlink in the console.
 *
 * @author jthywiss
 */
public class HttpPatternMatchListener implements IPatternMatchListenerDelegate {

	protected TextConsole observedConsole;

	/**
	 * Constructs an object of class HttpPatternMatchListener.
	 */
	public HttpPatternMatchListener() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.IPatternMatchListenerDelegate#connect(org.eclipse.ui.console.TextConsole)
	 */
	@Override
	public void connect(TextConsole console) {
		observedConsole = console;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.IPatternMatchListenerDelegate#disconnect()
	 */
	@Override
	public void disconnect() {
		observedConsole = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.IPatternMatchListenerDelegate#matchFound(org.eclipse.ui.console.PatternMatchEvent)
	 */
	@Override
	public void matchFound(PatternMatchEvent event) {
		try {
			int offset = event.getOffset();
			int length = event.getLength();
			String uriText = observedConsole.getDocument().get(offset, length);
			IHyperlink link = new HttpHyperLink(observedConsole, uriText);
			observedConsole.addHyperlink(link, offset, length);
		} catch (BadLocationException e) {
		}
	}

}
