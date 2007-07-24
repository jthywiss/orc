/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

import java.util.List;

import orc.runtime.Token;
import orc.runtime.nodes.Node;

/**
 * Callable objects include sites and definitions
 * @author wcook
 */
public interface Callable {

	/**
	 * Create a call to a callable value
	 * @param label		name (used for debugging)
	 * @param caller	token for which the call is being made: points to the call node
	 * @param args		argument list
	 * @param nextNode	next node after the call node, to which the result should be sent
	 * @param engine	Orc engine
	 */
	void createCall(Token caller, List<Future> args, Node nextNode);


}