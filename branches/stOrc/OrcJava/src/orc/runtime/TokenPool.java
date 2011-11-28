package orc.runtime;

import orc.error.runtime.TokenLimitReachedError;

/**
 * Implement a token pool to avoid allocating
 * and freeing a lot of tokens.
 * @author quark
 */
public final class TokenPool {
	/** How many tokens are available before we hit the pool size limit. */
	private int available;
	
	/** What is the limit for this pool? */
	private int limit;
	
	/**
	 * Create a new pool with the given bound.
	 * If bound &lt; 0 then the pool is unlimited. 
	 */
	public TokenPool(int bound) {
		this.available = bound;
		this.limit = bound;
	}
	
	/** Create and return a new, uninitialized token. */
	public synchronized Token newToken() throws TokenLimitReachedError {
		if (available == 0) {
			throw new TokenLimitReachedError(limit);
		} else if (available > 0) {
			--available;
		}
		return new Token();
	}
	
	/** Free a token */
	public synchronized void freeToken(Token token) {
		if (available >= 0) {
			++available;
		}
		token.free();
	}
}