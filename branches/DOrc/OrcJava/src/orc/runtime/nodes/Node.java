/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import orc.error.Debug;
import orc.runtime.Token;
import java.io.*;

/**
 * Abstract base class for compile nodes
 * @author wcook
 */
public abstract class Node extends Debug implements Serializable {
	
	/**
	 * The process method is the fundamental operation in the execution engine.
	 * It is called to perform the action of the node on a token.
	 * @param t      input token being processed 
	 */
	public abstract void process(Token t);
}
