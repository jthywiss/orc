package orc.run.tojava

import java.util.function.Consumer
import java.util.function.Function
import orc.OrcRuntime
import orc.Schedulable
import orc.OrcEvent
import orc.Handle
import orc.error.OrcException
import scala.util.parsing.input.Position
import java.util.concurrent.atomic.AtomicInteger
import orc.run.Logger

trait Context {
  import Context._
  
  val runtime: OrcRuntime
  
  def publish(v: AnyRef): Unit
  def halt(): Unit

  def spawn(f: Consumer[Context]): Unit = {
    prepareSpawn()
    runtime.schedule(() => f.accept(this))
  }
  
  def spawnFuture(f: Consumer[Context]): Future = {
    prepareSpawn()
    val fut = new Future(runtime)
    runtime.schedule { () =>
      f.accept(new ContextBase(this) {
        override def publish(v: AnyRef): Unit = {
          fut.bind(v)
        }
      })
    }
    fut
  }
  
  /** Setup for a spawn, but don't actually spawn anything.
   *  
   *  This is used in Future and possibly other places to prepare for a later execution.
   * 
   */
  def prepareSpawn(): Unit
  
  def checkKilled(): Unit = {
    if(! isLive()) {
      throw KilledException.SINGLETON
    }
  }
  def isLive(): Boolean
}

/**
 * @author amp
 */
class ContextBase(val parent: Context) extends Context {
  import Context._
  
  val runtime: OrcRuntime = parent.runtime
  
  def publish(v: AnyRef): Unit = {
    parent.publish(v)
  }
  
  def halt(): Unit = {
    parent.halt()
  }
  
  /** Setup for a spawn, but don't actually spawn anything.
   *  
   *  This is used in Future and possibly other places to prepare for a later execution.
   * 
   */
  def prepareSpawn(): Unit = {
    parent.prepareSpawn()
  }

  def isLive(): Boolean = {
    parent.isLive()
  }
}

final class ContextHandle(p: Context, val callSitePosition: Position) extends ContextBase(p) with Handle {
  def notifyOrc(event: OrcEvent): Unit = ???
  def setQuiescent(): Unit = ???

  def !!(e: OrcException): Unit = ???

  def hasRight(rightName: String): Boolean = ???
}



final class RootContext(override val runtime: OrcRuntime) extends Context {
  import Context._
  
  val count = new AtomicInteger()
  
  override def publish(v: AnyRef): Unit = {
    println(v)
  }
  
  override def halt(): Unit = {
    Logger.info("Decr")
    if(count.decrementAndGet() <= 0) {
      runtime.stopScheduler()
    }
  }

  override def prepareSpawn(): Unit = {
    count.incrementAndGet()
    Logger.info("Incr")
  }
  
  override def isLive(): Boolean = {
    true
  }
}

object Context {
  implicit final class FunctionSchedulable(f : () => Unit) extends Schedulable {
    override val nonblocking = true
    def run(): Unit = {
      f()
    }
  }
}


