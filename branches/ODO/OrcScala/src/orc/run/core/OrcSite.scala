//
// OrcSite.scala -- Scala classes OrcSite, OrcSiteCallGroup, and OrcSiteCallTarget
// Project OrcScala
//
// $Id$
//
// Created by amp on Dec 3, 2014.
//
// Copyright (c) 2014 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core

import java.lang.ref.{ PhantomReference, ReferenceQueue }
import orc.OrcEvent
import orc.ast.oil.nameless.Site

/** Collect all collectable OrcSites. Halting their declarations.
  *
  * This is not strictly allowed by the semantics, but without it sites are never
  * deleted. Think of this as an approximation of semantics up to the limitations
  * of a machine with finite memory.
  *
  * For this to catch unused OrcSites the JVM GC must run occationally. So this
  * object runs a thread that forces a GC every 2 seconds and continuously waits
  * for OrcSites to be collected.
  */
object OrcSiteCallTarget {
  private val queue = new ReferenceQueue[OrcSite]

  val FORCED_GC_PERIOD = 2000

  val collectorThread = new Thread() {
    setDaemon(true)
    start()

    override def run() = {
      while (true) {
        queue.remove(FORCED_GC_PERIOD) match {
          case null =>
            System.gc()
          case ref: OrcSiteCallTarget =>
            ref.halt()
          case _ =>
            throw new AssertionError("Found non-OrcSiteCallTarget reference in OrcSiteCallTarget.queue")
        }
      }
    }
  }

  def create(s: OrcSite) = new OrcSiteCallTarget(s)
}

class OrcSiteCallTarget(site: OrcSite) extends PhantomReference[OrcSite](site, OrcSiteCallTarget.queue) with GroupMember {
  override val nonblocking = true

  val group = site.group

  private var _isLive = true

  group.add(this)

  def halt() = if (isLive) {
    group.remove(this)
    clear()
  }

  def isLive = synchronized { _isLive }

  def kill(): Unit = synchronized {
    _isLive = false
  }

  // TODO: Implement suspension if needed.
  def suspend(): Unit = {}
  def resume(): Unit = {}

  def notifyOrc(event: OrcEvent) = { group.notifyOrc(event) }

  def checkAlive(): Boolean = isLive && group.checkAlive()

  def run() {
    if (group.isKilled()) { kill() }
  }
}

/** An OrcSite value.
  *
  * This value is also part of the Group tree to prevent the group containing the
  * site declaration from halting.
  *
  * @author amp
  */
class OrcSite(val code: Site,
  val group: Group,
  val clock: Option[VirtualClock]) {
  OrcSiteCallTarget.create(this)

  private var _context: List[Binding] = null

  def context = _context
  def context_=(ctx: List[Binding]) = {
    assert(_context == null)
    _context = ctx
  }
}

/** A OrcSiteCallGroup is the group associated with a call to an Orc site.
  *
  * @author amp
  */
class OrcSiteCallGroup(parent: Group, handle: OrcSiteCallHandle) extends Subgroup(parent) {
  def publish(t: Token, v: Option[AnyRef]) = synchronized {
    // This should never receive a stop. Just let it throw if it does. 
    handle.publishNonterminal(v.get)
    // Halt the token which sent this publication. It cannot do anything else.
    t.halt()
  }

  override def kill() = synchronized {
    handle.halt()
    super.kill()
  }

  def onHalt() = synchronized {
    handle.halt()
    parent.remove(this)
  }
}

/** A call handle specific to Orc site calls.
  *
  * @author amp
  */
class OrcSiteCallHandle(caller: Token) extends CallHandle(caller) {
}
