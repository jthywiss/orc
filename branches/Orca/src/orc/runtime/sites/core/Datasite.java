package orc.runtime.sites.core;

import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.PartialSite;
import orc.runtime.transaction.Transaction;
import orc.runtime.values.TaggedValue;
import orc.runtime.values.TupleValue;

public final class Datasite extends DotSite {

	public String tagName;
	
	public Datasite(String tagname) {
		this.tagName = tagname;
	}
	
	@Override
	protected void addMembers() {
		addMember("?", new PartialSite() {
			@Override
			public Object evaluate(Args args) throws TokenException {
				return deconstruct(args.getArg(0));
			}
		});
	}
	
	@Override
	protected void defaultTo(Args args, Token token, Transaction transaction) throws TokenException {
		token.resume(new TaggedValue(this, args.asArray()));
	}

	public Object deconstruct(Object arg) throws TokenException {
		if (arg instanceof TaggedValue) {
			TaggedValue v = (TaggedValue)arg;
			if (v.tag == this) {
				return Let.condense(v.values);
			}
		}
		return null;
	}
	
	public String toString() {
		return tagName;
	}
}
