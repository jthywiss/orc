//
// PorcENodeClassExecutionProfiler.scala -- Scala class and object PorcENodeClassExecutionProfiler
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.instruments

import java.io.{ OutputStreamWriter, PrintWriter }
import java.util.HashMap

import scala.collection.JavaConverters.{ asScalaSetConverter, collectionAsScalaIterableConverter }

import orc.run.porce.instruments.ProfilerUtils.ProfilerBase
import orc.util.{ CsvWriter, ExecutionLogOutputStream }

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Env
import com.oracle.truffle.api.nodes.Node
import org.graalvm.polyglot.Engine

class PorcENodeClassExecutionProfiler(env: Env) extends ProfilerBase {
  import ProfilerUtils._

  @TruffleBoundary @noinline
  def dispose(): Unit = {
    val out = ExecutionLogOutputStream("porce-class-profile-dispose", "csv", "PorcE profile dump")
    if (out.isDefined) {
      val pout = new PrintWriter(new OutputStreamWriter(out.get))
      dump(pout)
    }
  }


  @TruffleBoundary @noinline
  def dump(out: PrintWriter): Unit = synchronized {
    //val out = new PrintWriter(env.out())
    val csv = new CsvWriter(out.write(_))
    csv.writeHeader(Seq("Class [class]", "Hits [hits]", "Self Time (ns) [self]", "Total Time (ns) [total]"))
    for (entry <- nodeCounts.entrySet().asScala) {
      val k = entry.getKey();
      val count = entry.getValue();
      if (count.getHits() > 0) {
        csv.writeRow(Seq(k.getName.toString(), count.getHits(), count.getSelfTime(), count.getTime()))
      }
    }
    out.flush();
  }

  @TruffleBoundary @noinline
  def reset(): Unit = synchronized {
    for (c <- nodeCounts.values().asScala) {
      c.reset()
    }
  }

  val nodeCounts = new HashMap[Class[_], Counter]();

  @TruffleBoundary @noinline
  def getCounter(n: Class[_]): Counter = synchronized {
    nodeCounts.computeIfAbsent(n, (_) => new Counter())
  }
}

object PorcENodeClassExecutionProfiler {
  /** Finds profiler associated with given engine. There is at most one profiler associated with
    * any {@link PolyglotEngine}. One can access it by calling this static method.
    */
  def get(engine: Engine): PorcENodeClassExecutionProfiler = {
    val instrument = engine.getInstruments().get(PorcENodeClassExecutionProfilerInstrument.ID);
    if (instrument == null) {
      throw new IllegalStateException();
    }
    return instrument.lookup(classOf[PorcENodeClassExecutionProfiler]);
  }

  val KEY = PorcENodeClassExecutionProfiler;

  def nonTrivialNode(n: Node): Boolean = {
    n match {
      case _: orc.run.porce.Read.Argument => false
      case _: orc.run.porce.Read.Local => false
      case _: orc.run.porce.Read.Closure => false
      case _: orc.run.porce.Write.Local => false
      case _ =>
        true
    }
  }
}
