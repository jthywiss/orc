/**
 * 
 */
package orc.lib.str;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 *
 * Print arguments, converted to strings, in sequence, each followed by newlines.
 *
 */
public class Println extends Site {
	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		for(int i = 0; i < args.size(); i++) {
			caller.println(args.stringArg(i));
		}
		if (args.size() == 0) {
			caller.println("");
		}
		caller.resume(Value.signal());
	}
}
