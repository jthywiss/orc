/**
 * 
 */
package orc.runtime.sites;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.values.Value;

/**
 * Abstract class for sites with a total and immediate semantics: evaluate the arguments and
 * return a value without blocking and without affecting the Orc engine. Essentially, subclasses
 * of this class represent sites without any special concurrent behavior.
 * 
 * Subclasses must implement the method evaluate, which takes an argument list and returns
 * a single value.
 * 
 * @author dkitchin
 *
 */
public abstract class EvalSite extends Site {

	@Override
	public void callSite(Args args, Token caller) throws OrcRuntimeTypeException {

		caller.resume(evaluate(args));
	}
	
	abstract public Value evaluate(Args args) throws OrcRuntimeTypeException;

}
