//
// ThreadPoolScheduler.scala -- Scala traits OrcWithThreadPoolScheduler, OrcRunner, and OrcThreadPoolExecutor
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Mar 29, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.logging.Level

import orc.Handle
import orc.OrcExecutionOptions
import orc.run.Orc
import orc.run.Logger

/**
 * An Orc runtime engine extension which
 * schedules Orc Tokens to run in an OrcThreadPoolExecutor.
 *
 * @author jthywiss
 */
trait OrcWithThreadPoolScheduler extends Orc {

  private var executor: OrcRunner = null
  private val executorLock = new Object()

  override def schedule(ts: List[GroupMember with Runnable]) {
    ts.foreach(schedule(_))
  }

  override def schedule(t: GroupMember with Runnable, u: GroupMember with Runnable) {
    schedule(t)
    schedule(u)
  }

  override def schedule(t: GroupMember with Runnable) {
    if (executor == null) {
      throw new IllegalStateException("Cannot schedule a task without an inited executor")
    }
    executor.execute(t, false)
  }

  override def schedule(h: Handle) {
    if (executor == null) {
      throw new IllegalStateException("Cannot schedule a task without an inited executor")
    }
    executor.execute(h, true)
  }

  override def startScheduler(options: OrcExecutionOptions) {
    executorLock synchronized {
      if (executor == null) {
        executor = new OrcThreadPoolExecutor(options.maxSiteThreads)
        executor.startup()
      } else {
        throw new IllegalStateException("startScheduler() multiply invoked")
      }
    }
  }

  /* (non-Javadoc)
   * @see orc.run.Orc#stop()
   */
  override def stopScheduler() {
    executorLock synchronized {
      if (executor != null) {
        // First, gently shut down
        executor.shutdown()
        // Wait "a little while"
        if (!executor.awaitTermination(20L)) {
          // Now, we insist
          executor.shutdownNow()
          // Wait long enough for all running workers to receive shutdown
          executor.awaitTermination(2L)
        }
        executor = null
      }
    }
  }
}


/**
 * Interface from Orc runtime engine to an executor service
 *
 * @author jthywiss
 */
trait OrcRunner {

  type Task = Runnable

  /** Begin executing submitted tasks */
  @throws(classOf[IllegalStateException])
  @throws(classOf[SecurityException])
  def startup(): Unit

  /** Submit task for execution */
  @throws(classOf[IllegalStateException])
  @throws(classOf[SecurityException])
  def execute(task: Task, taskMayBlock: Boolean): Unit

  /** Orderly shutdown; let running & enqueued tasks complete */
  @throws(classOf[IllegalStateException])
  @throws(classOf[SecurityException])
  def shutdown(): Unit

  /** Attempt immediate shutdown; interrupt running tasks
   * @return List of queued tasks discarded
   */
  @throws(classOf[IllegalStateException])
  @throws(classOf[SecurityException])
  def shutdownNow(): java.util.List[Task]

  @throws(classOf[InterruptedException])
  def awaitTermination(timeoutMillis: Long): Boolean

}


/**
 * A ThreadPoolExecutor that periodically resizes the worker thread pool
 * to ensure there is a minimum number of runnable threads.  I.e., as
 * threads are blocked by their task, new threads are added to serve
 * the work queue.
 *
 * @author jthywiss
 */
class OrcThreadPoolExecutor(maxSiteThreads: Int) extends ThreadPoolExecutor(
    //TODO: Make more of these params configurable
    math.max(4, Runtime.getRuntime().availableProcessors * 2),
    if (maxSiteThreads > 0) maxSiteThreads else 256,
    2000L, TimeUnit.MILLISECONDS,
    new LinkedBlockingQueue[Runnable],
    new ThreadPoolExecutor.CallerRunsPolicy) with OrcRunner with Runnable {

  val threadGroup = new ThreadGroup("Orc Runtime Engine ThreadGroup")

  object OrcWorkerThreadFactory extends ThreadFactory {
    var threadCreateCount = 0
    protected def getNewThreadName() = {
      var ourThreadNum = 0
      synchronized {
        ourThreadNum = threadCreateCount
        threadCreateCount += 1
      }
      "Orc Worker Thread " + ourThreadNum
    }
    def newThread(r: Runnable): Thread = {
      new Thread(threadGroup, r, getNewThreadName())
    }
  }

  setThreadFactory(OrcWorkerThreadFactory)

  @scala.volatile private var supervisorThread: Thread = null
  def startup() {
    synchronized {
      if (supervisorThread != null) {
        throw new IllegalStateException("OrcThreadPoolExecutor.startup() on a started instance")
      }
      supervisorThread = new Thread(threadGroup, this, "Orc Runtime Engine Thread Pool Supervisor")
      supervisorThread.start()
    }
  }

  override def execute(task: Task, taskMayBlock: Boolean): Unit = {
    if (supervisorThread == null) {
      throw new IllegalStateException("OrcThreadPoolExecutor.execute() on an un-started instance")
    }
    //FIXME: Don't allow blocking tasks to consume all worker threads
    super.execute(task)
  }

  def awaitTermination(timeoutMillis: Long) = {
    super.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS)
  }

  protected val CHECK_PERIOD = 10 /* milliseconds */

  override def run() {
    val numCores = Runtime.getRuntime().availableProcessors()
    val mainLockField = getClass.getSuperclass.getDeclaredField("mainLock")
    mainLockField.setAccessible(true)
    val mainLock = mainLockField.get(this).asInstanceOf[java.util.concurrent.locks.ReentrantLock]

    try {
      while (!isShutdown) {
        try {
          Thread.sleep(CHECK_PERIOD)
        } catch {
          case _: InterruptedException => Thread.interrupted // Reset interrupted state
        }

        try {
          mainLock.lock()

          // Java thread states are:
          // NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED
          // RUNNABLE means can be or is running on a core
          // BLOCKED means waiting on a monitor (synchronized), so that's like RUNNABLE for us
          // WAITING, TIMED_WAITING, TERMINATED may never come back to make progress
          // However, some WAITING/TIMED_WAITING threads are actually waiting for new tasks
          // We want enough RUNNABLE+BLOCKED threads to keep all CPU cores busy, but not more.

          // This approach is stochastic; and the following calculation is approximate -- there are transients
          val threadBuffer = new Array[Thread](threadGroup.activeCount)
          val liveThreads = threadBuffer.take(threadGroup.enumerate(threadBuffer, false))
          val workingThreads = getActiveCount // Number of Workers running a Task
          val supervisor = Thread.currentThread
          val progressingThreadCount = liveThreads.count({t => t != supervisor && (t.getState == Thread.State.RUNNABLE || t.getState == Thread.State.BLOCKED || t.getState == Thread.State.NEW)})
          val nonProgressingWorkingThreadCount = workingThreads - progressingThreadCount

          //Logger.finest("poolSize = " + getPoolSize)
          //Logger.finest("workingThreads = " + workingThreads)
          //Logger.finest(liveThreads.filter({t => t != supervisor}).map(_.getState.toString + "  ").foldLeft("Thread States:  ")({(x,y)=>x+y}))
          //Logger.finest("progressingThreadCount = " + progressingThreadCount)
          //Logger.finest("nonProgressingWorkingThreadCount = " + nonProgressingWorkingThreadCount)
          //Logger.finest("numCores*2 + nonProgressingTaskCount = " + (numCores*2 + nonProgressingWorkingThreadCount))

          setCorePoolSize(math.min(math.max(4, numCores*2 + nonProgressingWorkingThreadCount), getMaximumPoolSize))
        } finally {
          mainLock.unlock()
        }
      }
    } catch {
      case t => { t.printStackTrace(); Logger.log(Level.SEVERE, "Caught in "+getClass.getCanonicalName+".run()", t); shutdownNow(); throw t }
    } finally {
      logThreadExit()
    }
    Logger.exiting(getClass.getCanonicalName, "run")
  }

  Logger.finer(getClass.getCanonicalName+": Constructed")
  Logger.finest("corePoolSize = " + getCorePoolSize)
  Logger.finest("maximumPoolSize = " + getMaximumPoolSize)

  def logThreadExit() = {
    Logger.finer(getClass.getCanonicalName+": Supervisor thread exit")
    Logger.finest("corePoolSize = " + getCorePoolSize)
    Logger.finest("maximumPoolSize = " + getMaximumPoolSize)
    Logger.finest("poolSize = " + getPoolSize)
    Logger.finest("activeCount = " + getActiveCount)
    Logger.finest("largestPoolSize = " + getLargestPoolSize)
    Logger.finest("taskCount = " + getTaskCount)
    Logger.finest("completedTaskCount = " + getCompletedTaskCount)
    Logger.finest("Worker threads creation count: " + OrcWorkerThreadFactory.threadCreateCount)
  }

}
