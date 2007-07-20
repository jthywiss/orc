/**
 * 
 */
package orc.lib.bool;

import orc.runtime.sites.EvalSite;
import orc.runtime.values.*;

/**
 * @author dkitchin
 *
 */
public abstract class BoolBinopSite extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Value evaluate(Tuple args) {
		
		return new Constant(compute(args.boolArg(0), args.boolArg(1)));
	}

	abstract public boolean compute(boolean a, boolean b);
	
}
