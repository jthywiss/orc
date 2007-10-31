package orc.orcx;

import orc.runtime.values.Value;

/**
 * 
 * An Orc value encapsulating an XML document node generated by XQuery. 
 * 
 * @author dkitchin
 *
 */

public class XMLValue extends Value {

	int nodeID;
	
	public XMLValue(int nodeID) {
		this.nodeID = nodeID;
	}
	
	public int getID() {
		return this.nodeID;
	}
	
	public String toString() {
		return "XML node with ID [" + this.nodeID + "]";
	}
	
}
