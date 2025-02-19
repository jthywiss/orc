//
// Closure.scala -- Scala class Closure
// Project OrcScala
//
// Created by dkitchin on Aug 12, 2011.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.core

import orc.OrcRuntime
import orc.ast.oil.nameless.Def

/** A closure that both resolves itself and represents the closure itself. This should
  * be scheduled when it is created.
  *
  * @author dkitchin, amp
  */
class Closure(
    private[run] val index: Int,
    val closureGroup: ClosureGroup)
  extends ResolvableCollectionMember[Def](index, closureGroup) {

  def code: Def = definition

  override def toString = super.toString + s"(${code.optionalVariableName.getOrElse("")}, ${code.body.sourceTextRange}, $closureGroup, $index)"

}

class ClosureGroup(
    _defs: List[Def],
    _lexicalContext: List[Binding],
    runtime: OrcRuntime,
    clock: Option[VirtualClock])
  extends ResolvableCollection[Def, Closure](_defs, _lexicalContext, runtime, clock) {
  def buildMember(i: Int) = new Closure(i, this)
}
