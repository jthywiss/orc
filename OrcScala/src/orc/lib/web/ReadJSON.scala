//
// OrcJSON.scala -- Scala class/trait/object OrcJSON
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Oct 25, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.web

import scala.util.parsing.combinator.JavaTokenParsers
import scala.util.parsing.combinator.syntactical._
import scala.util.parsing.combinator.lexical._

import orc.values.sites.{PartialSite, UntypedSite}
import orc.values.OrcRecord
import orc.error.runtime.{ArgumentTypeMismatchException, ArityMismatchException}

/**
 * 
 * JSON reader, converting a JSON string to an Orc value.
 *
 * @author dkitchin
 */
class ReadJSON extends PartialSite with UntypedSite {

  def evaluate(args: List[AnyRef]): Option[AnyRef] = {
    args match {
      case List(s: String) => OrcJSONParser.parse(s)
      case List(z) => throw new ArgumentTypeMismatchException(0, "String", z.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
    }
  }
  
}

/**
 *  Modified version of Scala's JSON parser, returning Orc values instead.
 * 
 *  @author dkitchin
 *  @author Derek Chen-Becker <"java"+@+"chen-becker"+"."+"org">
 *  
 */

//object OrcJSONLexical extends StdLexical with RegexParsers {
//  override type Elem = Char
//  
//  def numberToken = """[-]?(([1-9][0-9]*)|0)([.][0-9]+)?([Ee][+-]?(([1-9][0-9]*)|0))?""".r ^^ { NumericLit(_) }
//  
//  override def token = numberToken | super.token
//  
//  reserved ++= List("true", "false", "null")
//  delimiters ++= List("{", "}", "[", "]", ":", ",")
//}

object OrcJSONParser extends JavaTokenParsers {

    //override val lexical = OrcJSONLexical
    
    // Define the grammar
    def root       = jsonObj | jsonArray
    def jsonObj    = ("{" ~> repsep(objEntry, ",") <~ "}") ^^ { new OrcRecord(_) } 
    def jsonArray  = "[" ~> repsep(value, ",") <~ "]"
    def objEntry   = stringLiteral ~ (":" ~> value) ^^ { case x ~ y => (x, y) }
    def value: Parser[AnyRef] = (
        jsonObj 
      | jsonArray 
      | "true" ^^^ java.lang.Boolean.TRUE 
      | "false" ^^^ java.lang.Boolean.FALSE 
      | "null" ^^^ null
      | floatingPointNumber ^^ { BigDecimal(_) }
      | stringLiteral
    )
    
    def parse(json: String): Option[AnyRef] = {
      parseAll[AnyRef](root, new java.io.StringReader(json)) match {
        case Success(v,_) => Some(v)
        case n: NoSuccess => {
          System.err.println("JSON parsing failed: " + n.msg)
          System.err.println(n.next.pos.longString)
          None
        }
      }
    }
    
}
