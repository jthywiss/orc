/**
 * 
 */
package orc.runtime.sites.core;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.PassedByValueSite;
import orc.runtime.values.*;

/**
 * @author dkitchin
 *
 */
public class Not extends EvalSite implements PassedByValueSite {
	
	@Override
	public Value evaluate(Args args) throws OrcRuntimeTypeException {
		return new Constant(!args.boolArg(0));
	}

}
