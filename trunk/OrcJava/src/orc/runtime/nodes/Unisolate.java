/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import orc.runtime.IsolatedGroup;
import orc.runtime.Token;
import orc.runtime.regions.IsolatedRegion;

/**
 * @author quark
 */
public class Unisolate extends Node {
	private static final long serialVersionUID = 1L;
	public Node body;
	public Unisolate(Node body) {
		this.body = body;
	}

	public void process(Token t) {
		IsolatedRegion r = (IsolatedRegion)t.getRegion();
		t.setRegion(r.getParent());
		IsolatedGroup g = (IsolatedGroup)t.getGroup();
		t.setGroup(g.getParent());
		t.move(body).activate();
	}
}
