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
 * Print arguments, converted to strings, in sequence.
 *
 */
public class Print extends Site {
	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < args.size(); i++) {
			sb.append(args.stringArg(i));
		}
		caller.print(sb.toString(), false);
		caller.resume(Value.signal());
	}
}
