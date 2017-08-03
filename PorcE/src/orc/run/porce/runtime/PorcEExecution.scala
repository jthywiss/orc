package orc.run.porce.runtime

import orc.OrcEvent
import orc.ExecutionRoot
import orc.run.core.EventHandler
import com.oracle.truffle.api.nodes.RootNode
import com.oracle.truffle.api.frame.VirtualFrame
import orc.PublishedEvent
import orc.run.porce.PorcEUnit
import orc.run.porce.Logger
import com.oracle.truffle.api.CallTarget
import orc.Schedulable
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary

class PorcEExecution(val runtime: PorcERuntime, protected var eventHandler: OrcEvent => Unit) extends ExecutionRoot with EventHandler {
  private var _isDone = false
  
  /** Block until this context halts.
    */
  def waitForHalt(): Unit = {
    synchronized {
      while (!_isDone) wait()
    }
  }
  
  def isDone = PorcEExecution.this.synchronized { _isDone }

  runtime.installHandlers(this)

	@TruffleBoundary
  def notifyOrcWithBoundary(e: OrcEvent) = {
    notifyOrc(e)
  }

  val p: PorcEClosure = {
    Utilities.PorcEClosure(new RootNode(null) {
      def execute(frame: VirtualFrame): Object = {
        // Skip the first argument since it is our captured value array.
        val v = frame.getArguments()(1)
        notifyOrcWithBoundary(PublishedEvent(v))
        // Token: from initial caller of p.
        c.haltToken()
        PorcEUnit.SINGLETON
      }      
    })
  }
  
  val haltContinuation: PorcEClosure = {
    Utilities.PorcEClosure(new RootNode(null) {
      def execute(frame: VirtualFrame): Object = {
        // Runs regardless of discorporation.
        Logger.fine("Top level context complete.")
        runtime.removeRoot(PorcEExecution.this)
        PorcEExecution.this.synchronized {
          PorcEExecution.this._isDone = true
          PorcEExecution.this.notifyAll()
        }
        PorcEUnit.SINGLETON
      }      
    })
  }

  val c: Counter = new Counter(runtime, null, haltContinuation)

  val t = new Terminator

  def scheduleProgram(prog: PorcEClosure): Unit = {
    val nStarts = System.getProperty("porce.nStarts", "1").toInt
    // Token: From initial.
    for(_ <- 0 until nStarts) {
      c.newToken()
      runtime.schedule(new CounterSchedulable(c) {
        def run(): Unit = {
          //
          try {
            prog.callFromRuntime(p, c, t)
          } finally {
            //
          }
        }
      })
    }
    c.haltToken()
    
    /*if(Counter.tracingEnabled) {
      Thread.sleep(5000)
      Counter.report()
    }*/
  }
}