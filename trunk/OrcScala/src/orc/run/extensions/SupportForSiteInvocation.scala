//
// SupportForSiteInvocation.scala -- Scala class/trait/object SupportForSiteInvocation
// Project OrcScala
//
// $Id: SupportForSiteInvocation.scala 2376 2011-01-25 00:34:19Z dkitchin $
//
// Created by dkitchin on Jan 24, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import orc.InvocationBehavior
import orc.Handle
import orc.values.sites.Site
import orc.error.OrcException
import orc.error.runtime.JavaException

/**
 * 
 *
 * @author dkitchin
 */
trait SupportForSiteInvocation extends InvocationBehavior {  
  override def invoke(h: Handle, v: AnyRef, vs: List[AnyRef]) {
    v match {
      case (s: Site) => 
        try {
          s.call(vs, h)
        }
        catch {
          case e: OrcException => h !! e
          case e: InterruptedException => throw e
          case e: Exception => h !! new JavaException(e) //FIXME: This seems risky
        }
      case _ => super.invoke(h, v, vs)
    }
  }
}