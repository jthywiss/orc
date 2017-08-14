//
// SupportForDOrc.scala -- Scala class SupportForDOrc
// Project PorcE
//
// Created by jthywiss on Dec 21, 2015.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.distrib

import orc.{ Handle, Invoker }
import orc.run.Orc
import orc.run.porce.runtime.CPSCallResponseHandler

/** Adds facilities for distributed Orc calls to an Orc runtime engine.
  *
  * @author jthywiss
  */
trait SupportForDOrc extends Orc {
  override def getInvoker(target: AnyRef, arguments: Array[AnyRef]) = {
    if (!target.isInstanceOf[RemoteRef] && !arguments.exists(_.isInstanceOf[RemoteRef]) &&
      !target.isInstanceOf[LocationPolicy] && !arguments.exists(_.isInstanceOf[LocationPolicy])) {
      /* Assumption: PorcE will get a new invoker later if this call site is later
       * used with target or an arg that is a RemoteRef or has a LocationPolicy. */
      super.getInvoker(target, arguments)
    } else {
      new DOrcInvoker(target, arguments, super.getInvoker(target, arguments))
    }
  }
}

class DOrcInvoker(target: AnyRef, arguments: Array[AnyRef], localInvoker: Invoker) extends Invoker {

  def invoke(callResponseReceiver: Handle, target: AnyRef, arguments: Array[AnyRef]) {

    def pickLocation(ls: Set[PeerLocation]) = ls.head

    //Logger.entering(getClass.getName, "invoke", Seq(target.getClass.getName, target, arguments))

    //TODO: These are safe casts, but it's ugly.  Should there be a nice interface here?  Don't want to expose Execution on the orc.Handle interface, though.
    val dOrcExecution: DOrcExecution = callResponseReceiver.asInstanceOf[CPSCallResponseHandler].execution.asInstanceOf[DOrcExecution]

    val intersectLocs = (arguments map dOrcExecution.currentLocations).fold(dOrcExecution.currentLocations(target)) { _ & _ }
    if (!(intersectLocs contains dOrcExecution.runtime.here)) {
      orc.run.distrib.Logger.finest(s"siteCall($target,$arguments): intersection of current locations=$intersectLocs")
      val candidateDestinations = {
        if (intersectLocs.nonEmpty) {
          intersectLocs
        } else {
          val intersectPermittedLocs = (arguments map dOrcExecution.permittedLocations).fold(dOrcExecution.permittedLocations(target)) { _ & _ }
          if (intersectPermittedLocs.nonEmpty) {
            intersectPermittedLocs
          } else {
            throw new NoLocationAvailable(target +: arguments.toSeq)
          }
        }
      }
      orc.run.distrib.Logger.finest(s"candidateDestinations=$candidateDestinations")
      val destination = pickLocation(candidateDestinations)
      dOrcExecution.sendToken(???, destination)
    } else {
      localInvoker.invoke(callResponseReceiver, target, arguments)
    }
  }

  def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
    localInvoker.canInvoke(target, arguments)
  }

}
