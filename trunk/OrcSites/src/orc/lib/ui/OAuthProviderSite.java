package orc.lib.ui;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JOptionPane;

import kilim.Pausable;
import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;
import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.oauth.OAuthProvider;
import orc.runtime.Args;
import orc.runtime.Kilim;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.Constant;
import orc.runtime.values.Value;

import com.centerkey.utils.BareBonesBrowserLaunch;

public class OAuthProviderSite extends EvalSite {
	/**
	 * This provider uses Java UI stuff to launch a browser
	 * and prompt the user to confirm authorization.
	 */
	private static class GuiOAuthProvider extends OAuthProvider {
		public GuiOAuthProvider(String properties) throws IOException {
			super(properties);
		}

		@Override
		public OAuthAccessor authenticate(final String consumer,
				final List<OAuth.Parameter> request)
		throws Pausable, Exception {
			final OAuthAccessor accessor = oauth.newAccessor(consumer);
			Kilim.runThreaded(new Callable<Object>() {
				public Object call() throws Exception {
					oauth.setRequestToken(accessor, request);
					// prompt the user for authorization;
					// do not provide a callback URL
					String authURL = oauth.getAuthorizationURL(accessor, null)
						.toExternalForm();
					BareBonesBrowserLaunch.openURL(authURL);
					 int ok = JOptionPane.showConfirmDialog(null,
							 "Your browser should open and ask you to" +
							 " confirm authorization.\n\nPlease click Yes once" +
							 " you have confirmed authorization.");
					 if (ok != 0) throw new OAuthException("Authorization refused by user.");
					 // confirm authorization
					oauth.setAccessToken(accessor);
					return Kilim.signal;
				}
			});
			return accessor;
		}
	}
	
	@Override
	public Value evaluate(Args args) throws TokenException {
		try {
			/**
			 * This implementation of OAuthProvider 
			 */
			return new Constant(new GuiOAuthProvider(
					// force root-relative resource path
					"/" + args.stringArg(0)));
		} catch (IOException e) {
			throw new JavaException(e);
		}
	}
}
