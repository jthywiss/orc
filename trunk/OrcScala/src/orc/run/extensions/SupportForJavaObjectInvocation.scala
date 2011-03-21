//
// SupportForJavaObjectInvocation.scala -- Scala class/trait/object SupportForJavaObjectInvocation
// Project OrcScala
//
// $Id: SupportForJavaObjectInvocation.scala 2581 2011-03-21 07:41:38Z dkitchin $
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
import orc.values.OrcValue
import orc.Handle
import orc.values.sites.JavaCall

/**
 * 
 *
 * @author dkitchin
 */
trait SupportForJavaObjectInvocation extends InvocationBehavior {
  
  override def invoke(h: Handle, v: AnyRef, vs: List[AnyRef]) { 
    v match {
      case v : OrcValue => super.invoke(h, v, vs)
      case _ => {
        val successful = JavaCall(v, vs, h)
        if (!successful) { super.invoke(h, v, vs) }
      }
    }
  }

}
