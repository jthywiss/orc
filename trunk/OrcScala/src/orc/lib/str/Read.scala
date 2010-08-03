//
// Read.scala -- Scala object Read
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Jun 9, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.str

import orc.values.sites.TotalSite
import orc.values.sites.UntypedSite
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ArityMismatchException
import orc.compile.parse.OrcLiteralParser
import orc.error.compiletime.ParsingException
import orc.compile.ext.Expression
import orc.compile.ext.Constant
import orc.compile.ext.ListExpr
import orc.compile.ext.TupleExpr
import orc.values.OrcTuple

object Read extends TotalSite with UntypedSite {
  def evaluate(args: List[AnyRef]): AnyRef = {
    val parsedValue = args match {
      case List(s: String) => {
        OrcLiteralParser(s) match {
          case r: OrcLiteralParser.SuccessT[_] => r.get.asInstanceOf[Expression]
          case n: OrcLiteralParser.NoSuccess   => throw new ParsingException(n.msg+" when reading \""+s+"\"")
        }
      }
      case List(a) => throw new ArgumentTypeMismatchException(0, "String", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
    }
    convertToOrcValue(parsedValue)
  }
  def convertToOrcValue(v: Expression): AnyRef = v match {
    case Constant(v) => v
    case ListExpr(vs) => vs map convertToOrcValue
    case TupleExpr(vs) => OrcTuple(vs map convertToOrcValue)
    case mystery => throw new ParsingException("Don't know how to convert a "+mystery.getClass().getName()+" to an Orc value")
  }
}
