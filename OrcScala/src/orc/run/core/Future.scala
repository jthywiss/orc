//
// Future.scala -- Scala class/trait/object Future
// Project OrcScala
//
// $Id$
//
// Created by amp on Dec 6, 2014.
//
// Copyright (c) 2014 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core

import orc.OrcRuntime

/** A future value that can be bound or unbound or halted.
  *  
  * Note: This implementation is from the Porc implementation, so 
  * it is slightly more optimized than most of the token interpreter.
  * Specifically the states are Ints to avoid the need for objects
  * in the critial paths. The trade off is that Future contains an
  * extra couple of pointers.
  */
final class Future(val runtime: OrcRuntime) extends Blocker {
  import Future._

  var _state = Unbound
  var _value: AnyRef = null
  var _blocked: List[Blockable] = Nil

  def bind(v: AnyRef) = synchronized {
    if (_state == Unbound) {
      _state = Bound
      _value = v
      //Logger.finest(s"$this bound to $v")
      scheduleBlocked()
    }
  }
  
  def stop() = synchronized {
    if (_state == Unbound) {
      //Logger.finest(s"$this halted")
      _state = Halt
      scheduleBlocked()
    }
  }
  
  private def scheduleBlocked(): Unit = {
    for (j <- _blocked) {
      runtime.stage(j)
    }
    // We will never need the blocked list again, so clear it to allow the blackables to be collected
    _blocked = null
  }

  
  def read(blockable: Blockable) = {
    val st = synchronized  { 
      _state match {
        case Unbound => {
          blockable.blockOn(this)
          _blocked ::= blockable
        }
        case _ => {}
      }
      _state
    }
   
    st match {
      case Bound => blockable.awakeTerminalValue(_value)
      case Halt => blockable.awakeStop()
      case Unbound => {}
    }
  }
  
  def check(blockable: Blockable): Unit = {
    synchronized { _state } match {
      case Unbound => throw new AssertionError("Spurious call to Future.check.")
      case Bound => blockable.awakeTerminalValue(_value)
      case Halt => blockable.awakeStop()
    }
  }
}

object Future {
  val Unbound = 0
  val Bound = 1
  val Halt = 2
}
