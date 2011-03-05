//
// Ceil.java -- Java class Ceil
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

import orc.error.runtime.TokenException;
import orc.types.Type;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.Types;
import java.math.BigInteger;
import java.math.BigDecimal;

public class Ceil extends EvalSite {

  public static BigInteger ceil(BigDecimal d) {
    if (d.signum() >= 0) {
      try {
        // d has no fractional part
        return d.toBigIntegerExact();
      }
      catch (ArithmeticException e) {
        // d has a fractional part
        return d.add(BigDecimal.ONE).toBigInteger();
      }
    }
    else {
      return Floor.floor(d.negate()).negate();
    }
  }

  @Override
  public Object evaluate(final Args args) throws TokenException {
      final Number n = args.numberArg(0);
      if (   n instanceof BigInteger
          || n instanceof Integer
          || n instanceof Long
          || n instanceof Short
          || n instanceof Byte) {
        return n;
      }
      else {
        BigDecimal d = (n instanceof BigDecimal ? (BigDecimal)n : new BigDecimal(n.doubleValue()));
        return ceil(d);
      }
  }
}
