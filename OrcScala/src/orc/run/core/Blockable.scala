//
// Blockable.scala -- Scala class/trait/object Blockable
// Project OrcScala
//
// $Id$
//
// Created by amp on Dec 14, 2012.
//
// Copyright (c) 2012 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core
import orc.Schedulable
import orc.error.OrcException

/**
  * The interface that allows Schedulables to block on Blockers and receive a value when unblocked.
  * @author amp
  */
trait Blockable extends Schedulable {
  /* The following actions are applied to Blockables to wake them.
   *
   *        P = Pruning, O = Otherwise, L = Closure, C = CallHandle
   *        These define what Blockers use what kind of awake.
   * 
   * Publish a value                 PCL
   * Publish stop                    P
   * Halt                            OC
   * Unblock and simply continue     O
   * Unblock with an exception       C
   * 
   * Tokens do all of these. Closures will only do publish of value and stop.
   */
      
  /**
   * Called to wake up the blockable without providing any information. Just wake up to whatever you where doing.
   */
  def awake() : Unit = {
    throw new Error("Awake called on blockable that does not support awake without information (This is an interpreter bug).")
  }
  
  /**
   * Called to wake up the blockable, but halt it immediately.
   */
  def halt() : Unit = {
    throw new Error("Halt called on non-haltable blockable (This is an interpreter bug).")
  }
  
  /**
   * Called by the blocker from within it's check method to notify the 
   * Blockable that it has been unblocked and to provide the value that 
   * it might was waiting on. This must only be called while 
   * executing on behalf of the Blockable.
   */
  def awakeValue(v : AnyRef) : Unit
  
  /**
   * Called by the blocker from within it's check method to notify the 
   * Blockable that it has been unblocked and notify it that the value it
   * it was waiting on is Stop (this is similar to publish(None)). 
   * This must only be called while executing on behalf of the Blockable.
   */
  def awakeStop() : Unit

  /**
   * Called by the blocker from within it's check method to notify the 
   * Blockable that it has been unblocked by an exception. This must only be called while 
   * executing on behalf of the Blockable.
   * 
   * The default implementation rethrows the exception.
   */
  def awakeException(e : OrcException) : Unit = {
    throw e;
  }
    
  /**
   * Called to tell the Blockable to block on the given blocker.
   * Called only while executing on behalf of the blockable.
   */
  def blockOn(b : Blocker) : Unit
}
