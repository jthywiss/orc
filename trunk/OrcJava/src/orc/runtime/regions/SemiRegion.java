package orc.runtime.regions;

import orc.runtime.Token;

public class SemiRegion extends Region {

	Region parent;
	Token t;
	
	/* Create a new group region with the given parent and coupled group cell */
	public SemiRegion(Region parent, Token t) {
		this.parent = parent;
		this.t = t;
		this.parent.add(this);
	}
	
	protected void reallyClose(Token closer) {
		if (t != null) {
			t.getTracer().after(closer.getTracer().before());
			t.setPending();
			t.activate();
		}
		parent.remove(this, closer);
	}

	public Region getParent() {
		return parent;
	}

	public void cancel() {
		if (t != null) {
			t.die();
			t = null;
		}
	}
}
