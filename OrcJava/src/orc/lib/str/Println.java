/**
 * 
 */
package orc.lib.str;

import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.*;

/**
 * @author dkitchin
 *
 * Print arguments, converted to strings, in sequence, each followed by newlines.
 *
 */
public class Println extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Value evaluate(Args args) throws TokenException {
		
		for(int i = 0; i < args.size(); i++)
		{
			System.out.println(args.stringArg(i));
		}
		
		if (args.size() == 0) { System.out.println(); }
		
		return signal();
	}

}
