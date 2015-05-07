/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import orc.error.Located;
import orc.error.Locatable;
import orc.error.SourceLocation;
import orc.runtime.Token;
import java.io.*;

/**
 * Abstract base class for compile nodes
 * @author wcook
 */
public abstract class Node implements Serializable {
	/**
	 * The process method is the fundamental operation in the execution engine.
	 * It is called to perform the action of the node on a token.
	 * @param t      input token being processed 
	 */
	public abstract void process(Token t);
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
	
	public String toString() {
		return getClass().getSimpleName();
	}
	
	/** Does this node kill the incoming token? */
	public boolean isTerminal() { return false; }
}
