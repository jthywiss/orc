//
// ComparisonSite.java -- Java class ComparisonSite
// Project OrcScala
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.comp;

import java.math.BigDecimal;
import java.math.BigInteger;

import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.Args.NumericBinaryOperator;
import orc.values.sites.compatibility.Types;
import orc.values.sites.TypedSite;
import orc.types.Type;


/**
 * @author quark
 */
@SuppressWarnings("synthetic-access")
public abstract class ComparisonSite extends EvalSite implements TypedSite {
	private static class MyOperator implements NumericBinaryOperator<Integer> {
		@Override
		public Integer apply(final BigInteger a, final BigInteger b) {
			return Integer.valueOf(a.compareTo(b));
		}

		@Override
		public Integer apply(final BigDecimal a, final BigDecimal b) {
			return Integer.valueOf(a.compareTo(b));
		}

		@Override
		public Integer apply(final int a, final int b) {
			return Integer.valueOf(a - b);
		}

		@Override
		public Integer apply(final long a, final long b) {
			return Integer.valueOf((int) (a - b));
		}

		@Override
		public Integer apply(final byte a, final byte b) {
			return Integer.valueOf(a - b);
		}

		@Override
		public Integer apply(final short a, final short b) {
			return Integer.valueOf(a - b);
		}

		@Override
		public Integer apply(final double a, final double b) {
			return Integer.valueOf(Double.compare(a, b));
		}

		@Override
		public Integer apply(final float a, final float b) {
			return Integer.valueOf(Float.compare(a, b));
		}
	}

	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
    @Override
	public Object evaluate(final Args args) throws TokenException {
		final Object arg0 = args.getArg(0);
		final Object arg1 = args.getArg(1);
		try {
			if (arg0 instanceof Number && arg1 instanceof Number) {
				final int a = Args.applyNumericOperator((Number) arg0, (Number) arg1, new MyOperator()).intValue();
				return Boolean.valueOf(compare(a));
			} else {
			    @SuppressWarnings("unchecked")
				final int a = ((Comparable<Object>) arg0).compareTo(arg1);
				return Boolean.valueOf(compare(a));
			}
		} catch (final ClassCastException e) {
			throw new JavaException(e); // TODO: Make more specific
		}
	}

	abstract public boolean compare(int a);

	@Override
	public Type orcType() {
		return Types.function(Types.top(), Types.top(), Types.bool());
	}
}
