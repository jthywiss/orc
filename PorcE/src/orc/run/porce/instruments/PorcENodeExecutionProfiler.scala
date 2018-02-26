package orc.run.porce.instruments

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import java.util.HashMap
import orc.ast.porc.PorcAST
import scala.collection.JavaConverters._
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Env
import com.oracle.truffle.api.vm.PolyglotEngine
import java.io.PrintWriter
import orc.util.CsvWriter
import orc.util.ExecutionLogOutputStream
import java.io.OutputStreamWriter
import orc.run.porce.instruments.ProfilerUtils.ProfilerBase
import com.oracle.truffle.api.nodes.Node
import orc.run.porce.PorcENode
import orc.run.porce.HasPorcNode

class PorcENodeExecutionProfiler(env: Env) extends ProfilerBase {
  import ProfilerUtils._

  @TruffleBoundary(allowInlining = true) @noinline
  def dispose(): Unit = {
    val out = ExecutionLogOutputStream("porce-profile-dispose", "csv", "Porc profile dump")
    if (out.isDefined) {
      val pout = new PrintWriter(new OutputStreamWriter(out.get))
      dump(pout)
    }
  }
  
  
  @TruffleBoundary(allowInlining = true) @noinline
  def dump(out: PrintWriter): Unit = synchronized {
    //val out = new PrintWriter(env.out())
    val csv = new CsvWriter(out.write(_))
    csv.writeHeader(Seq(
        "Type", "PorcE Node ID", 
        "PorcE Node", "Porc Node", "Source Range", 
        "PorcE Specialization Dump",
        "Hits", "Self Time", "Total Time"))
    for (entry <- nodeCounts.entrySet().asScala) {
      val k = entry.getKey();
      val count = entry.getValue();
      if (count.getHits() > 0) {
        import orc.util.StringExtension._
        val srcLoc = k.getSourceSection()
        csv.writeRow(Seq(
            k.getClass.getName, System.identityHashCode(k).formatted("%08x"),
            k.toString().truncateTo(500).stripNewLines,
            k match {
              case n: HasPorcNode => n.porcNode() map { _.toString().truncateTo(200).stripNewLines } getOrElse "NULL"
              case _ => "NA"
            }, 
            s"${srcLoc.getSource.getName}:${srcLoc.getStartLine}:${srcLoc.getStartColumn}-${srcLoc.getEndColumn}",
            DumpSpecializations.specializationsAsString(k).stripNewLines,
            count.getHits(), count.getSelfTime(), count.getTime()))
      }
    }
    out.flush();
  }

  @TruffleBoundary(allowInlining = true) @noinline
  def reset(): Unit = synchronized {
    for (c <- nodeCounts.values().asScala) {
      c.reset()
    }
  }

  val nodeCounts = new HashMap[Node, Counter]();

  @TruffleBoundary(allowInlining = true) @noinline
  def getCounter(n: Node): Counter = synchronized {
    nodeCounts.computeIfAbsent(n, (_) => new Counter())
  }
}

object PorcENodeExecutionProfiler {
  /** Finds profiler associated with given engine. There is at most one profiler associated with
    * any {@link PolyglotEngine}. One can access it by calling this static method.
    */
  def get(engine: PolyglotEngine): PorcENodeExecutionProfiler = {
    val instrument = engine.getRuntime().getInstruments().get(PorcENodeExecutionProfilerInstrument.ID);
    if (instrument == null) {
      throw new IllegalStateException();
    }
    return instrument.lookup(classOf[PorcENodeExecutionProfiler]);
  }

  val KEY = PorcENodeExecutionProfiler;
}