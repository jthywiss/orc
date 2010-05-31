//
// Sites.scala -- Scala traits Site, PatialSite, and UntypedSite
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 28, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.sites

import orc.oil.Value
import orc.oil.nameless.Type
import orc.oil.nameless.Bot
import orc.TokenAPI

trait Site extends Value {
  def call(args: List[Value], token: TokenAPI): Unit
  def name: String
  def orcType(argTypes: List[Type]): Type
  def instantiate() = this
}

trait PartialSite extends Site {
  def call(args: List[Value], token: TokenAPI) {
    evaluate(args) match {
      case Some(v) => token.publish(v)
      case None => token.halt
    }
  }

  def evaluate(args: List[Value]): Option[Value]
}

trait UntypedSite extends Site {
  def orcType(argTypes: List[Type]): Type = Bot()
}
