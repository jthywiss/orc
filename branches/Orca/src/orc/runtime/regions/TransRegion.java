package orc.runtime.regions;

import orc.runtime.Token;
import orc.runtime.transaction.Transaction;
import orc.runtime.values.GroupCell;
import orc.trace.events.Event;

/**
 * 
 * This region encloses the tokens of a running transaction.
 * 
 * When the region closes, it instructs the transaction to
 * prepare to commit.
 * 
 * @author dkitchin
 *
 */
public class TransRegion extends Region {

	Region parent;
	Transaction trans;
	
	/* Create a new group region with the given parent and coupled group cell */
	public TransRegion(Region parent, Transaction trans) {
		this.parent = parent;
		this.parent.add(this);
		this.trans = trans;
	}
	
	protected void onClose() {
		parent.remove(this);
		trans.prepareCommit();
	}
}
