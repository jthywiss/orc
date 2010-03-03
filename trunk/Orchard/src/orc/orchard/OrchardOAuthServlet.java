//
// OrchardOAuthServlet.java -- Java class OrchardOAuthServlet
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

package orc.orchard;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kilim.Mailbox;
import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.server.OAuthServlet;
import orc.runtime.Kilim;
import orc.runtime.OrcEngine;

public class OrchardOAuthServlet extends HttpServlet {
	public final static String MAILBOX = "orc.orchard.OrchardOAuthServlet.MAILBOX";

	public static String getCallbackURL(final OAuthAccessor accessor, final Mailbox mbox, final OrcEngine globals) throws IOException {
		accessor.setProperty(MAILBOX, mbox);
		final String key = globals.addGlobal(accessor);
		// FIXME: we should figure out the callback URL
		// automatically from the servlet context
		return OAuth.addParameters(accessor.consumer.callbackURL, "k", key);
	}

	public void receiveAuthorization(final HttpServletRequest request) throws IOException, OAuthException {
		final OAuthMessage requestMessage = OAuthServlet.getMessage(request, null);
		requestMessage.requireParameters(OAuth.OAUTH_TOKEN, "k");

		final OAuthAccessor accessor = (OAuthAccessor) OrcEngine.globals.remove(requestMessage.getParameter("k"));
		if (accessor == null) {
			return;
		}
		
		if (!accessor.requestToken.equalsIgnoreCase(requestMessage.getParameter(OAuth.OAUTH_TOKEN))) {
			System.err.println("OrchardOAuthServlet: token mismatch: received " + requestMessage.getParameter(OAuth.OAUTH_TOKEN) + ", but expected " + accessor.requestToken);
			throw new OAuthException("OrchardOAuthServlet: token mismatch");
		}
		
		final String verifier = requestMessage.getParameter("oauth_verifier"); 
		if (verifier != null) {
			accessor.setProperty("oauth_verifier", verifier);
		}
		
		final Mailbox mbox = (Mailbox) accessor.getProperty(MAILBOX);
		if (mbox == null) {
			return;
		}
		System.out.println("OrchardOAuthServlet: approving " + accessor.requestToken);
		mbox.putb(Kilim.signal);
	}

	@Override
	protected void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		try {
			receiveAuthorization(request);
		} catch (final OAuthException e) {
			throw new ServletException(e);
		}
		final PrintWriter out = response.getWriter();
		out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		out.write("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
		out.write("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n");
		out.write("<head>\n");
		out.write("<meta http-equiv=\"Content-Type\" content=\"text/xhtml+xml; charset=UTF-8\" />\n");
		out.write("<title>Authorization Received</title>\n");
		out.write("</head>\n");
		out.write("<body>\n");
		out.write("<h1>Authorization Received</h1>\n");
		out.write("<h2>Thank you, you may now close this window.</h2>\n");
		out.write("</body>\n");
		out.write("</html>\n");
		out.close();
	}
}
