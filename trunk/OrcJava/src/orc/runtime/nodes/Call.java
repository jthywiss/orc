/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import java.util.LinkedList;
import java.util.List;

import orc.ast.oil.arg.Arg;
import orc.error.Debuggable;
import orc.error.OrcException;
import orc.error.SourceLocation;
import orc.error.TokenException;
import orc.runtime.Token;
import orc.runtime.values.Callable;
import orc.runtime.values.Future;

/**
 * Compiled node for a call (either a site call or a definition call)
 * @author wcook
 */
public class Call extends Node {
	private static final long serialVersionUID = 1L;
	Arg caller;
	List<Arg> args;
	Node next;

	public Call(Arg caller, List<Arg> args, Node next) {
		this.caller = caller;
		this.args = args;
		this.next = next;	
	}

	/** 
	 * Looks up the function to be called, then creates a call
	 * token using the argument expressions.
	 * @see orc.runtime.nodes.Node#process(orc.runtime.Token, orc.runtime.OrcEngine)
	 */
	public void process(Token t) {
		
		try {

			Callable target = t.call(caller);

			/** 
			 * target is null if the caller is still unbound, in which
			 * case the calling token will be activated when the
			 * caller value becomes available. Thus, we simply
			 * return and wait for the token to enter the process
			 * method again.
			 */
			if (target == null) { return; }

			/**
			 * Collect all of the environment's bindings for these args.
			 * Note that some of them may still be unbound, since we are
			 * not forcing the futures.
			 */
			List<Future> actuals = new LinkedList<Future>();

			for (Arg a : args)
			{
				actuals.add(t.lookup(a));
			}

			target.createCall(t, actuals, next);
			
		} catch (TokenException e) {
			t.error(e);
		}
	}
}
