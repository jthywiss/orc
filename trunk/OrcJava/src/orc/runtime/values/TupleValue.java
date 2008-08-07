/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;

/**
 * A tuple value container
 * @author wcook, quark
 */
public class TupleValue extends EvalSite implements Iterable<Object> {
	Object[] values;
	public TupleValue() {
		this.values = new Object[0];
	}
	public TupleValue(Object v) {
		this.values = new Object[1];
		this.values[0] = v;
	}
	public TupleValue(Object v, Object w) {
		this.values = new Object[2];
		this.values[0] = v;
		this.values[1] = w;
	}
	public TupleValue(List<Object> values) {
		this.values = new Object[values.size()];
		this.values = values.toArray(this.values);
	}
	public TupleValue(Object[] values) {
		this.values = values;
	}
	public Object evaluate(Args args) throws TokenException	{
		// TODO: Generalize this treatment of dot sites.
		try { 
			String s = args.fieldName();
			if (s.equals("fits")) {
				return new FitSite(values.length);
			}
		} catch (TokenException e) {
			// do nothing
		}
		return values[args.intArg(0)];
	}

	static class FitSite extends EvalSite {
		int size;
		public FitSite(int size) {
			this.size = size;
		}
		public Object evaluate(Args args) throws TokenException {
			return args.intArg(0) == this.size;
		}
	}
	
	public Object at(int i) {
		return values[i];
	}
	
	public int size() {
		return values.length;
	}

	public String toString() {
		return format('(', values, ", ", ')');
	}
	
	public static String format(char left, Object[] items, String sep, char right) {
		StringBuffer buf = new StringBuffer();
		buf.append(left);
		for (int i = 0; i < items.length; ++i) {
			if (i > 0) buf.append(sep);
			buf.append(items[i].toString());
		}
		buf.append(right);
		return buf.toString();
	}
	public List<Object> asList() {
		return Arrays.asList(values);
	}
	public Iterator<Object> iterator() {
		return asList().iterator();
	}
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}