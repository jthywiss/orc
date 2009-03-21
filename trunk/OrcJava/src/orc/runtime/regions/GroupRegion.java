package orc.runtime.regions;

import orc.runtime.Token;
import orc.runtime.values.GroupCell;
import orc.trace.events.Event;

public class GroupRegion extends Region {

	Region parent;
	GroupCell cell;
	
	/* Create a new group region with the given parent and coupled group cell */
	public GroupRegion(Region parent, GroupCell cell) {
		this.parent = parent;
		this.cell = cell;
		this.parent.add(this);
		cell.region = this;
	}
	
	protected void onClose() {
		cell.kill();
		parent.remove(this);
	}
}
