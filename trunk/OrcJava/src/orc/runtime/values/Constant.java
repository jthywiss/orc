/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import orc.runtime.Token;
import orc.runtime.sites.java.ObjectProxy;

/**
 * A value container for an arbitrary Java object, which includes Orc literal
 * atomic values.
 * 
 * <p>
 * This has overrides to support treating collections and arrays as lists (i.e.
 * list pattern matches will work on them).
 * 
 * @author wcook, dkitchin, quark
 */
public class Constant extends Value {
	private static final long serialVersionUID = 1L;
	Object value;

	public Constant(Object value) {
		this.value = value;
	}

	public Object getValue()
	{
		return value;
	}
	
	public String toString() {
		return value.toString();
	}
	
	/**
	 * A Java value used in call position becomes a proxied object.
	 */
	public Callable forceCall(Token t)
	{
		return new ObjectProxy(value);
	}

	@Override
	public orc.orchard.oil.Value marshal() {
		// Check if the value corresponds to an atomic
		// type which will be serializable by JAXB;
		// otherwise the value is unrepresentable
		// in Oil.
		if (value instanceof String) {
		} else if (value instanceof Boolean) {
		} else if (value instanceof Number) {
		} else if (value instanceof Character) {
		} else if (value instanceof	java.util.Calendar) {
		} else if (value instanceof	java.util.Date) {
		} else if (value instanceof	javax.xml.namespace.QName) {
		} else if (value instanceof	java.net.URI) {
		} else if (value instanceof	javax.xml.datatype.Duration) {
		} else if (value instanceof	java.util.UUID) {
		} else {
			return new orc.orchard.oil.UnrepresentableValue(value.toString());
		}
		return new orc.orchard.oil.Constant(value);
	}
	
	/**
	 * Produce a new iterator for this value.
	 * Throws an exception if the value is not iterable.
	 */
	private Iterator asIterator() throws ClassCastException {
		try {
			return ((Iterable)value).iterator();
		} catch (ClassCastException _) {
			return Arrays.asList((Object[])value).iterator();
		}
	}

	@Override
	public Value head() {
		return new Constant(asIterator().next());
	}

	@Override
	public Value tail() {
		// Copy the remainder of the constant to an Orc list.
		// This seems awfully inefficient, but I can't think
		// of a better way to do it without being able to clone
		// iterators.
		ListValue l = new NilValue();
		Iterator i = asIterator();
		LinkedList<Value> tmpList = new LinkedList<Value>();
		i.next();
		while (i.hasNext()) tmpList.add(new Constant(i.next()));
		return ListValue.make(tmpList);
	}

	@Override
	public boolean isCons() {
		try {
			Iterator i = asIterator();
			return i.hasNext();
		} catch (ClassCastException e) {
			return false;
		}
	}

	@Override
	public boolean isNil() {
		try {
			Iterator i = asIterator();
			return !i.hasNext();
		} catch (ClassCastException e) {
			return false;
		}
	}
}
