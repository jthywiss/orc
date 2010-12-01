//
// UMinus.java -- Java class UMinus
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

package orc.lib.math;

import java.math.BigDecimal;
import java.math.BigInteger;

import orc.error.runtime.TokenException;
import orc.types.Type;
import orc.values.sites.TypedSite;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.Args.NumericUnaryOperator;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.Types;

/**
 * @author dkitchin
 *
 */
@SuppressWarnings({ "boxing", "synthetic-access" })
public class UMinus extends EvalSite implements TypedSite {
	private static final MyOperator op = new MyOperator();

	private static final class MyOperator implements NumericUnaryOperator<Number> {
		@Override
		public Number apply(final BigInteger a) {
			return a.negate();
		}

		@Override
		public Number apply(final BigDecimal a) {
			return a.negate();
		}

		@Override
		public Number apply(final int a) {
			return -a;
		}

		@Override
		public Number apply(final long a) {
			return -a;
		}

		@Override
		public Number apply(final byte a) {
			return -a;
		}

		@Override
		public Number apply(final short a) {
			return -a;
		}

		@Override
		public Number apply(final double a) {
			return -a;
		}

		@Override
		public Number apply(final float a) {
			return -a;
		}
	}

	@Override
	public Object evaluate(final Args args) throws TokenException {
		return Args.applyNumericOperator(args.numberArg(0), op);
	}

	@Override
	public Type orcType() {
      return Types.overload(
               Types.function(Types.integer(), Types.integer()),
               Types.function(Types.number(), Types.number())
             );
  }
}
