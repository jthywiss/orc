//
// Marshaller.java -- Java class Marshaller
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2008 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.trace.values;

import java.util.HashMap;
import java.util.Map;

import orc.runtime.values.Visitor;

public class Marshaller {
	/**
	 * @see #marshal(Object)
	 */
	private final Map<Object, Value> shadows = new HashMap<Object, Value>();

	/**
	 * @see #marshal(Object)
	 */
	private final Visitor<Value> visitor = new Visitor<Value>() {
		@Override
		public Value visit(final orc.runtime.values.ConsValue v) {
			return new ConsValue(marshal(v.head), (ListValue) marshal(v.tail));
		}

		@Override
		public Value visit(final orc.runtime.values.TupleValue v) {
			final Value[] values2 = new Value[v.values.length];
			for (int i = 0; i < v.values.length; ++i) {
				values2[i] = marshal(v.values[i]);
			}
			return new TupleValue(values2);
		}

		@Override
		public Value visit(final orc.runtime.values.TaggedValue v) {
			final Value[] values2 = new Value[v.values.length];
			for (int i = 0; i < v.values.length; ++i) {
				values2[i] = marshal(v.values[i]);
			}
			return new TaggedValue(v.tag.tagName, values2);
		}

		@Override
		public Value visit(final orc.runtime.values.Field v) {
			return new FieldValue(v.getKey());
		}

		@Override
		public Value visit(final Object value) {
			if (value instanceof TraceableValue) {
				return ((TraceableValue) value).marshal(Marshaller.this);
			} else if (value instanceof Boolean) {
				return new ConstantValue((Boolean) value);
			} else if (value instanceof Character) {
				return new ConstantValue((Character) value);
			} else if (value instanceof String) {
				return new ConstantValue((String) value);
			} else if (value instanceof Number) {
				return new ConstantValue((Number) value);
			} else {
				return new ObjectValue(value.getClass());
			}
		}
	};

	/**
	 * Marshal a runtime value into an immutable, serializable representation.
	 * To avoid wasting a lot of space and time by copying the same values
	 * repeatedly, the marshalled representations are cached in a weak hash map.
	 * 
	 * <p>
	 * Note that marshalling loses object identity, since equal (but different)
	 * objects share the same representation, and the same object (at different
	 * times) may use different representations.
	 */
	public Value marshal(final Object value) {
		// Check for some special singleton types, which
		// there is no need to cache explicitly
		if (value == null) {
			return NullValue.singleton;
		} else if (value instanceof orc.runtime.values.NilValue) {
			return NilValue.singleton;
		} else {
			// The value is not a singleton type, so
			// check for it in the cache and add it
			// if necessary
			Value shadow;
			synchronized (shadows) {
				if (shadows.containsKey(value)) {
					shadow = shadows.get(value);
				} else {
					shadow = Visitor.visit(visitor, value);
					shadows.put(value, shadow);
				}
			}
			return shadow;
		}
	}
}
