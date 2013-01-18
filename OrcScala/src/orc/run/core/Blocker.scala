//
// Blocker.scala -- Scala trait Blocker
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Aug 12, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core

/**
 * The trait for objects that can be blocked on. 
 * @author dkitchin
 */
trait Blocker {
  /**
   * When a Blockable blocked on this resource is scheduled,
   * it performs this check to observe any changes in
   * the state of this resource. 
   * 
   * This should call Blockable#awake(AnyRef) to notify the
   * Blockable.
   */
  def check(t: Blockable): Unit
}
