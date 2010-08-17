//
// AST.scala -- Scala class AST
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 27, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast

import orc.error.OrcException
import scala.collection.mutable.MutableList
import scala.util.parsing.input.{NoPosition, Position, Positional}

trait AST extends Positional {
  
  /**
   * Location-preserving transform
   */
  def ->[B <: AST](f: this.type => B): B = {
      val location = this.pos
      val result = f(this)
      result.pushDownPosition(location)
      result
  }
  
  /**
   * Location transfer.
   * x ->> y  is equivalent to  x -> (_ => y)
   */
  def ->>[B <: AST](that : B): B = { that.pushDownPosition(this.pos); that }
  
  /**
   * Set source location at this node and propagate
   * the change to any children without source locations.
   */
  def pushDownPosition(p : Position): Unit = {
    this.pos match {
      case NoPosition => {
        this.setPos(p)
        this.subtrees map { _.pushDownPosition(p) }
      }
      case _ => {  }
    }
  }
  
  /**
   * All AST node children of this node, as a single list
   */
  def subtrees: List[AST] = {

    def flattenAstNodes(x: Any, flatList: MutableList[AST]) {
      def isGood(y: Any): Boolean = y match {
        case _: AST => true
        case i: Iterable[_] => i.forall(isGood(_))
        case _ => false
      }
      def traverseAndAdd(z: Any) {
        z match {
          case a: AST => flatList += a
          case i: Iterable[_] => i.foreach(traverseAndAdd(_))
        }
      }
      if (isGood(x)) traverseAndAdd(x)
    }

    val goodKids = new MutableList[AST]();
    for (f <- this.productIterator) {
      if (f.isInstanceOf[AST] || f.isInstanceOf[scala.collection.Iterable[_]]) {
        flattenAstNodes(f, goodKids);
      }
    }
    goodKids.toList
  }
  def productIterator: Iterator[Any] //Subclasses that are case classes will supply automatically
}
