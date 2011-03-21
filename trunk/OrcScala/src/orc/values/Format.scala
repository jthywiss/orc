//
// Format.scala -- Scala object Format
// Project OrcScala
//
// $Id: Format.scala 2228 2010-12-07 19:13:50Z jthywissen $
//
// Created by dkitchin on Jul 10, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.values

/**
 * 
 * Format values (given as Scala's type AnyRef)
 * 
 * A value which can be written in an Orc program is formatted as a string which
 * the parser would parse as an expression evaluating to that value.
 * 
 * A value which cannot be written in an Orc program is given back in some
 * suitable pseudo-syntax.
 *
 * @author dkitchin
 */
object Format {
  
  def formatValue(v: Any): String =
    v match {
      case null => "null"
      case l: List[_] => "[" + formatSequence(l) + "]"
      case s: String => unparseString(s)
      case orcv: OrcValue => orcv.toOrcSyntax()
      case other => other.toString()
    }

  // For Java callers:
  def formatValueR(v: AnyRef): String = formatValue(v)
  
  def formatSequence(vs : List[_]) = 
    vs match {
      case Nil => ""
      case _ => ( vs map { formatValue } ) reduceRight { _ + ", " + _ } 
    }
  
  def unparseString(s : String) = {
    "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\f", "\\f").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\""
  }
  
}
