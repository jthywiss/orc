//
// ManyActorBasedScheduler.scala -- Scala class/trait/object ManyActorBasedScheduler
// Project OrcScala
//
// $Id$
//
// Created by amshali on Aug 10, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import orc.run.Orc
import scala.concurrent._
import scala.actors.Actor
import scala.actors.Actor._
import scala.collection.mutable.Queue


/**
 * 
 *
 * @author amshali, dkitchin
 */
trait ManyActorBasedScheduler extends Orc {
  // distribute the work between actors in a round-robin fashion
  override def schedule(ts: List[Token]) = synchronized {
    var i = 0
    for (t <- ts) {
      ActorPool.get ! Some(t)
    }
  }

  object ActorPool {
    val queue : Queue[Worker] = Queue()
    def get() : Worker = synchronized {
      if (queue.size == 0) new Worker().start
      else queue.dequeue
    }
    def store(w : Worker) = synchronized {
      queue.enqueue(w)
    }
    def shutdown() {
      for (x <- queue) x ! None
    }
  }
  
  /* Shut down this runtime and all of its backing threads.
   * All executions stop without cleanup, though they are not guaranteed to stop immediately. 
   * This will cause all synchronous executions to hang. 
   */
  // TODO: Implement cleaner alternatives.
  override def stop = {
    ActorPool.shutdown
    super.stop
  }

  class Worker extends Actor {
    override def start() = {
      super.start
      this
    }
    def act() {
      loop {
        react {
          case Some(x: Token) => {
            // If this thread should be interrupted, then reflect now, rather than wait for I/O
            if (Thread.interrupted()) // Note: Clears thread's interrupted status bit
              throw new InterruptedException()
            x.run
            ActorPool.store(this)
          }
          // machine has stopped
          case None => exit
        }
      }
    }
  }

}