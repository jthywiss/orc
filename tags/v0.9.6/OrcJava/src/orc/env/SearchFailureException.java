package orc.env;

/**
 * 
 * Any exception generated by Orc, during compilation, loading, or execution.
 * Though sites written in Java will sometimes produce java-level exceptions,
 * those exceptions are wrapped in a subclass of OrcException to localize and
 * isolate failures (see LocalException, JavaException).
 * 
 * @author dkitchin
 *
 */
public class SearchFailureException extends EnvException {
	
	public SearchFailureException(String message) {
		super(message);
	}

	public SearchFailureException(String message, Throwable cause) {
		super(message, cause);
	}
	
}