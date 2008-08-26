package orc.trace.events;

import orc.trace.TokenTracer.BeforeTrace;

/**
 * Leaving the left side of a semicolon combinator.
 * 
 * @author quark
 */
public class BeforeEvent extends Event implements BeforeTrace {
	@Override
	public String getType() {
		return "before";
	}
}
