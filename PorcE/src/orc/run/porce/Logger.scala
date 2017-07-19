//
// Logger.scala -- Scala object Logger
// Project OrcScala
//
// Created by jthywiss on Jul 10, 2010.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.porce

import java.util.logging.Level
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary

/**
 * Logger for the orc.run.porce subsystem
 * 
 * The wrappers are rewrapped here to mark them as truffle partial evaluation boundaries. This
 * is important since partial evaluation going into the logger would be a total waste.
 * 
 * @author jthywiss
 */
object Logger {
  private val underlying = new orc.util.Logger("orc.run.porce")
  
  val julLogger = underlying.julLogger

  @TruffleBoundary
  final def log(level: Level, msg: => String): Unit = underlying.log(level, msg)
  @TruffleBoundary
  final def log(level: Level, msg: => String, params: => Seq[Object]): Unit = underlying.log(level, msg, params)
  @TruffleBoundary
  final def log(level: Level, msg: => String, thrown: Throwable): Unit = underlying.log(level, msg, thrown)
  @TruffleBoundary
  final def logp(level: Level, sourceClass: => String, sourceMethod: => String, msg: => String): Unit = underlying.logp(level, sourceClass, sourceMethod, msg)
  @TruffleBoundary
  final def logp(level: Level, sourceClass: => String, sourceMethod: => String, msg: => String, param1: Object): Unit = underlying.logp(level, sourceClass, sourceMethod, msg, param1)
  @TruffleBoundary
  final def logp(level: Level, sourceClass: => String, sourceMethod: => String, msg: => String, params: => Seq[Object]): Unit = underlying.logp(level, sourceClass, sourceMethod, msg, params)
  @TruffleBoundary
  final def logp(level: Level, sourceClass: => String, sourceMethod: => String, msg: => String, thrown: Throwable): Unit = underlying.logp(level, sourceClass, sourceMethod, msg, thrown)
  @TruffleBoundary
  final def entering(sourceClass: => String, sourceMethod: => String): Unit = underlying.entering(sourceClass, sourceMethod)
  @TruffleBoundary
  final def entering(sourceClass: => String, sourceMethod: => String, params: => Seq[Object]): Unit = underlying.entering(sourceClass, sourceMethod, params)
  @TruffleBoundary
  final def exiting(sourceClass: => String, sourceMethod: => String): Unit = underlying.exiting(sourceClass, sourceMethod)
  @TruffleBoundary
  final def exiting(sourceClass: => String, sourceMethod: => String, result: Object): Unit = underlying.exiting(sourceClass, sourceMethod, result)
  @TruffleBoundary
  final def throwing(sourceClass: => String, sourceMethod: => String, thrown: Throwable): Unit = underlying.throwing(sourceClass, sourceMethod, thrown)
  @TruffleBoundary
  final def severe(msg: => String): Unit = underlying.severe(msg)
  @TruffleBoundary
  final def warning(msg: => String): Unit = underlying.warning(msg)
  @TruffleBoundary
  final def info(msg: => String): Unit = underlying.info(msg)
  @TruffleBoundary
  final def config(msg: => String): Unit = underlying.config(msg)
  @TruffleBoundary
  final def fine(msg: => String): Unit = underlying.fine(msg)
  @TruffleBoundary
  final def finer(msg: => String): Unit = underlying.finer(msg)
  @TruffleBoundary
  final def finest(msg: => String): Unit = underlying.finest(msg)
  
  @TruffleBoundary
  final def check(assertion: Boolean, message: => Any) = underlying.check(assertion, message)
}
