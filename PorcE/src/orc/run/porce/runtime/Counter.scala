//
// Counter.scala -- Scala class Counter
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime

import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.io.StringWriter
import java.io.PrintWriter
import java.util.concurrent.LinkedBlockingDeque
import scala.annotation.elidable
import orc.run.porce.Logger
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import orc.ast.porc
import com.oracle.truffle.api.Truffle
import com.oracle.truffle.api.frame.FrameInstance
import orc.run.porce.PorcENode
import com.oracle.truffle.api.RootCallTarget
import com.oracle.truffle.api.nodes.{ Node => TruffleNode }
import orc.run.porce.HasPorcNode

object Counter {
  // Due to inlining, changing this will likely require a full rebuild.
  val tracingEnabled = false

  val liveCounters = new LinkedBlockingDeque[Counter]()

  def exceptionString(e: Exception) = {
    val ss = new StringWriter()
    e.printStackTrace(new PrintWriter(ss))
    val s = ss.toString()
    s.dropWhile(_ != ':')
  }

  @TruffleBoundary
  def getPorcStackTrace(): Seq[porc.PorcAST] = {
    var b = Seq.newBuilder[porc.PorcAST]
    def appendIfPorc(n: TruffleNode) = n match {
      case n: HasPorcNode =>
        n.porcNode match {
          case Some(n) =>
            b += n
          case None =>
            Logger.fine(s"Found PorcE node without Porc node: $n")
        }
      case null =>
        {}
      case n =>
        Logger.fine(s"Found unknown node: $n")
    }
    Truffle.getRuntime.iterateFrames((f: FrameInstance) => {
      appendIfPorc(f.getCallNode)
      f.getCallTarget match {
        case c: RootCallTarget =>
          appendIfPorc(c.getRootNode)
        case t =>
          Logger.fine(s"Found unknown target: $t")
      }
    })
    b.result()
  }

  val leavesOnly = true

  @TruffleBoundary
  def report() = {
    if (Counter.tracingEnabled && Logger.julLogger.isLoggable(Level.FINE)) {
      import scala.collection.JavaConverters._

      val allCounters = liveCounters.asScala
      lazy val parentCounters = allCounters.collect({
        case c: CounterNestedBase =>
          c.parent
      }).toSet
      lazy val leafCounters = allCounters.filterNot(parentCounters)
      val counters = if (leavesOnly) leafCounters else allCounters

      Logger.fine(s"========================= Counter Report; showing ${counters.size} of ${allCounters.size} counters")
      Logger.fine(counters.map(c => s"$c: log size = ${c.log.size}, count = ${c.count.get}").mkString("\n"))
      for (c <- counters) {
        Logger.fine(s"$c:\n${c.log.asScala.map(exceptionString(_)).mkString("---------------\n")}")
      }
    } else {
      Logger.warning(s"Cannot report Counter information if FINE is not loggable in ${Logger.julLogger.getName} or Counter.tracingEnabled == false")
    }
  }

  @elidable(elidable.ASSERTION)
  @TruffleBoundary
  def addCounter(c: Counter) = {
    if (Counter.tracingEnabled && Logger.julLogger.isLoggable(Level.FINE)) {
      liveCounters.add(c)
    }
  }

  @elidable(elidable.ASSERTION)
  @TruffleBoundary
  def removeCounter(c: Counter) = {
    if (Counter.tracingEnabled && Logger.julLogger.isLoggable(Level.FINE)) {
      liveCounters.remove(c)
    }
  }
}

/**
 * @author amp
 */
abstract class Counter {
  @elidable(elidable.ASSERTION)
  val log = if (Counter.tracingEnabled) new LinkedBlockingDeque[Exception]() else null

  @elidable(elidable.ASSERTION)
  @TruffleBoundary
  private final def logChange(s: => String) = {
    if (Counter.tracingEnabled && Logger.julLogger.isLoggable(Level.FINE)) {
      val stack = Counter.getPorcStackTrace().map(n => {
        val rangeStr = n.sourceTextRange.map(_.lineContentWithCaret).getOrElse("")
        val nodeStr = n.toString().take(80)
        s"$rangeStr\n$nodeStr"
      }).mkString("\n---vvv---\n")
      log.add(new Exception(s"$s in Porc stack:\n$stack"))
    }
  }
  logChange(s"Init to 1")

  Counter.addCounter(this)
  // */

  /*
  val log: LinkedBlockingDeque[Exception] = null
  @inline
  private def logChange(s: => String) = {
  }
  // */

  /**
   * The number of executions that are either running or pending.
   *
   * This functions similarly to a reference count and this halts when count
   * reaches 0.
   */
  val count = new AtomicInteger(1)
  @volatile
  var isDiscorporated = false

  def setDiscorporate() = {
    assert(count.get() > 0)
    isDiscorporated = true
  }

  def discorporateToken() = {
    setDiscorporate()
    haltToken()
  }

  /**
   * Decrement the count and check for overall halting.
   *
   * If we did halt call onContextHalted().
   */
  def haltToken(): Unit = {
    val n = count.decrementAndGet()
    if (Counter.tracingEnabled) {
      logChange(s"- Down to $n")
      if (n < 0) {
        Counter.report()
      }
      assert(n >= 0, s"Halt is not allowed on already stopped Counters: $this")
    }
    if (n == 0) {
      if (Counter.tracingEnabled) {
        Counter.removeCounter(this)
      }
      onContextHalted()
    }
  }

  /**
   * Increment the count.
   */
  def newToken(): Unit = {
    val n = count.getAndIncrement()
    if (Counter.tracingEnabled) {
      logChange(s"+ Up from $n")
      if (n <= 0) {
        Counter.report()
      }
      assert(n > 0, s"Spawning is not allowed once we go to zero count. No zombies allowed!!! $this")
    }
  }

  /**
   * Called when this whole context has halted.
   */
  def onContextHalted(): Unit
}

/**
 * @author amp
 */
abstract class CounterNestedBase(val parent: Counter) extends Counter {
  /**
   * Called when this whole context has halted.
   */
  def onContextHalted(): Unit = {
    if (isDiscorporated)
      parent.setDiscorporate()
    // Token: from parent
    parent.haltToken()
  }

  override def toString() = {
    s"${super.toString()}($parent)"
  }
}

/**
 * @author amp
 */
final class CounterNested(execution: PorcEExecution, parent: Counter, haltContinuation: PorcEClosure) extends CounterNestedBase(parent) {
  /**
   * Called when this whole context has halted.
   */
  override def onContextHalted(): Unit = {
    if (!isDiscorporated) {
      // Token: from parent
      execution.runtime.scheduleOrCall(parent, () => {
        haltContinuation.callFromRuntime()
      })
      // Not calling super since the token has already been given away.
    } else {
      super.onContextHalted()
    }
  }
}
