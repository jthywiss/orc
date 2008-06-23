package orc.error;

/**
 * 
 * Error conditions that should never occur. The occurrence of such
 * an error at runtime indicates the violation of some language
 * invariant.
 * 
 * @author dkitchin
 *
 */

public class OrcError extends Error {

	public OrcError(String message) {
		super(message);
	}

}
