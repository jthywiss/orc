//
// OrcEngine.scala -- Scala class OrcEngine
// Project OrcScala
//
// $Id$
//
// Created by amshali on May 26, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package test.orc

import orc.run.Orc
import orc.run.StandardOrcExecution
import orc.oil.nameless.Expression
import orc.values.Value
import orc.values.sites.Site
import java.lang._

class OrcEngine {

  val out = new StringBuffer("")

  val orcRuntime = new StandardOrcExecution {
    override def emit(v: Value) { println(v); out.append(v.toOrcSyntax()+"\n") }
    override def expressionPrinted(s: String) { print(s); out.append(s) }
    override def caught(e: Throwable) {
      // TODO: Make this less simplistic (while keeping it robust)
      out.append("Error!\n") 
      super.caught(e)
    }
  }

  def getOut() : StringBuffer = out

  def run(e : Expression) {
    orcRuntime.run(e)
    orcRuntime.waitUntilFinished
  }

}
