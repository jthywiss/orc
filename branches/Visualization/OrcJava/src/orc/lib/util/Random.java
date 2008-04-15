package orc.lib.util;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.sites.PartialSite;
import orc.runtime.values.Constant;
import orc.runtime.values.Value;

public class Random extends PartialSite {

	java.util.Random rnd;
	
	public Random() {
		rnd = new java.util.Random();
	}
	
	@Override
	public Value evaluate(Args args) throws OrcRuntimeTypeException {
		
		int limit = args.intArg(0);
		
		if (limit > 0) {
			return new Constant(rnd.nextInt(limit));
		}
		else {
			return null;
		}
	}

}
