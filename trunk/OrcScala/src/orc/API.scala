//
// API.scala -- Interfaces for Orc compiler and runtime
// Project OrcScala
//
// $Id: API.scala 2379 2011-01-27 21:10:08Z dkitchin $
//
// Created by dkitchin on May 10, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc

import java.io.IOException
import java.io.File
import scala.collection.JavaConversions._
import orc.compile.parse.OrcInputContext
import orc.error.OrcException
import orc.error.compiletime.{CompilationException, CompileLogger}
import orc.error.runtime.ExecutionException
import orc.ast.oil.nameless.Expression
import orc.progress.ProgressMonitor
import orc.values.Signal

/**
 * The interface from a caller to the Orc compiler
 */
trait OrcCompilerProvides {
  @throws(classOf[IOException])
  def apply(source: OrcInputContext, options: OrcCompilationOptions, compileLogger: CompileLogger, progress: ProgressMonitor): Expression
  def refineOil(oilAstRoot: Expression): Expression = oilAstRoot
}

/**
 * The interface from the Orc compiler to its environment
 */
trait OrcCompilerRequires {
  @throws(classOf[IOException])
  def openInclude(includeFileName: String, relativeTo: OrcInputContext, options: OrcCompilationOptions): OrcInputContext
  @throws(classOf[ClassNotFoundException])
  def loadClass(className: String): Class[_]
}

/** 
 * An Orc compiler
 */
trait OrcCompiler extends OrcCompilerProvides with OrcCompilerRequires

/**
 * The interface from a caller to an Orc runtime
 */
trait OrcRuntimeProvides {
  @throws(classOf[ExecutionException])
  def run(e: Expression, k: OrcEvent => Unit, options: OrcExecutionOptions): Unit
  def stop: Unit
}

/**
 *  The interface from an Orc runtime to its environment
 */
trait OrcRuntimeRequires {
  def invoke(h: Handle, v: AnyRef, vs: List[AnyRef]): Unit
}

/**
 * An Orc runtime 
 */
trait OrcRuntime extends OrcRuntimeProvides with OrcRuntimeRequires {
  type Token

  def schedule(ts: List[Token]): Unit

  // Schedule function is overloaded for convenience
  def schedule(t: Token) { schedule(List(t)) }
  def schedule(t: Token, u: Token) { schedule(List(t, u)) }
}

/* Define invocation behaviors for a runtime */
trait InvocationBehavior extends OrcRuntime {
  /* By default, an invocation halts silently. This will be overridden by other traits. */
  def invoke(h: Handle, v: AnyRef, vs: List[AnyRef]): Unit = { h.halt }
}


/**
 * The interface through which the environment response to site calls.
 */
trait Handle {
  
  def notifyOrc(event: OrcEvent): Unit
  
  def publish(v: AnyRef): Unit
  def publish(): Unit = { publish(Signal) }
  def halt: Unit
  def !!(e: OrcException): Unit
  
  def isLive: Boolean
}

/**
 * An event reported by an Orc execution
 */
trait OrcEvent

case class PublishedEvent(value: AnyRef) extends OrcEvent
case object HaltedEvent extends OrcEvent
case class CaughtEvent(e: Throwable) extends OrcEvent

/**
 * An action for a few major events reported by an Orc execution.
 * This is an alternative to receiving <code>OrcEvents</code> for a client
 * with simple needs, or for Java code that cannot create Scala functions.
 */
class OrcEventAction {
  val asFunction: (OrcEvent => Unit) = _ match {
    case PublishedEvent(v) => published(v)
    case CaughtEvent(e) => caught(e)
    case HaltedEvent => halted()
    case event => other(event)
  }

  def published(value: AnyRef) {}
  def caught(e: Throwable) {}
  def halted() {}
  
  @throws(classOf[Exception])
  def other(event: OrcEvent) {}
}


/**
 * Options for Orc compilation and execution.
 *
 * @author jthywiss
 */
trait OrcOptions extends OrcCompilationOptions with OrcExecutionOptions

trait OrcCommonOptions {
  def filename: String
  def filename_=(newVal: String)
  def classPath: java.util.List[String]
  def classPath_=(newVal: java.util.List[String])
  def logLevel: String
  def logLevel_=(newVal: String)
}

trait OrcCompilationOptions extends OrcCommonOptions {
  def usePrelude: Boolean
  def usePrelude_=(newVal: Boolean)
  def includePath: java.util.List[String]
  def includePath_=(newVal: java.util.List[String])
  def additionalIncludes: java.util.List[String]
  def additionalIncludes_=(newVal: java.util.List[String])
  def typecheck: Boolean
  def typecheck_=(newVal: Boolean)
  def disableRecursionCheck: Boolean
  def disableRecursionCheck_=(newVal: Boolean)
  def echoOil: Boolean
  def echoOil_=(newVal: Boolean)
  def oilOutputFile: Option[File]
  def oilOutputFile_=(newVal: Option[File])
  def compileOnly: Boolean
  def compileOnly_=(newVal: Boolean)
  def runOil: Boolean
  def runOil_=(newVal: Boolean)
}

trait OrcExecutionOptions extends OrcCommonOptions {
  def showJavaStackTrace: Boolean
  def showJavaStackTrace_=(newVal: Boolean)
  def disableTailCallOpt: Boolean
  def disableTailCallOpt_=(newVal: Boolean)
  def stackSize: Int
  def stackSize_=(newVal: Int)
  def maxTokens: Int
  def maxTokens_=(newVal: Int)
  def maxSiteThreads: Int
  def maxSiteThreads_=(newVal: Int)
  def hasRight(rightName: String): Boolean
  def setRight(rightName: String, newVal: Boolean)
}
