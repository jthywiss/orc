//
// ReadXML.scala -- Scala class ReadXML
// Project OrcScala
//
// $Id: ReadXML.scala 2228 2010-12-07 19:13:50Z jthywissen $
//
// Created by dkitchin on Nov 17, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.xml

import orc.values.sites.TotalSite1
import orc.values.sites.UntypedSite
import scala.xml.Node
import orc.error.runtime.ArgumentTypeMismatchException


/**
 * 
 *
 * @author dkitchin
 */
class WriteXML extends TotalSite1 with UntypedSite {
  
  def eval(arg: AnyRef): AnyRef = {
    arg match {
      case xml: Node => { xml.toString }
      case z => throw new ArgumentTypeMismatchException(0, "scala.xml.Node", z.getClass().toString())
    }
  }
  
}