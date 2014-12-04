//
// Main.scala -- Scala object Main
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Jul 20, 2010.
//
// Copyright (c) 2012 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc

import javax.script.{ ScriptException, Compilable, ScriptEngine, ScriptEngineManager }
import javax.script.ScriptContext.ENGINE_SCOPE
import java.io.{ PrintStream, FileNotFoundException, InputStreamReader, FileInputStream, File }
import scala.collection.JavaConversions._
import orc.error.OrcException
import orc.error.runtime.JavaException
import orc.script.{ OrcBindings, OrcScriptEngine }
import orc.values.Format
import orc.util.CmdLineParser
import orc.util.CmdLineUsageException
import orc.util.PrintVersionAndMessageException
import orc.run.OrcDesktopEventAction
import orc.ast.oil.xml.OrcXML
import java.util.logging.LogRecord
import java.io.StringWriter
import java.io.PrintWriter
import java.util.logging.SimpleFormatter
import java.util.logging.Formatter

/** A command-line tool invocation of the Orc compiler and runtime engine
  *
  * @author jthywiss
  */
object Main {
  class OrcCmdLineOptions() extends OrcBindings() with CmdLineOptions

  def main(args: Array[String]) {
    try {
      val options = new OrcCmdLineOptions()
      options.parseCmdLine(args)
      setupLogging(options)

      val engine = (new ScriptEngineManager).getEngineByName("orc").asInstanceOf[ScriptEngine with Compilable]
      if (engine == null) throw new ClassNotFoundException("Unable to load Orc ScriptEngine")
      engine.setBindings(options, ENGINE_SCOPE)

      val stream = new FileInputStream(options.filename)

      val compiledOrc =
        if (options.runOil) {
          engine.asInstanceOf[OrcScriptEngine[AnyVal]].loadDirectly(stream)
        } else {
          val reader = new InputStreamReader(stream, "UTF-8")
          engine.compile(reader).asInstanceOf[OrcScriptEngine[AnyVal]#OrcCompiledScript]
        }

      if (options.compileOnly) {
        if (options.runOil) {
          Console.err.println("Warning: run-oil ignored since compile-only was also set.")
        }
        return
      }

      val printPubs = new OrcDesktopEventAction() {
        override def published(value: AnyRef) { println(Format.formatValue(value)); Console.out.flush() }
        override def caught(e: Throwable) { Console.out.flush(); printException(e, Console.err, options.showJavaStackTrace); Console.err.flush() }
      }
      compiledOrc.run(printPubs)

    } catch {
      case e: CmdLineUsageException => Console.err.println("Orc: " + e.getMessage)
      case e: PrintVersionAndMessageException => println(orcImplName + " " + orcVersion + "\n" + orcURL + "\n" + orcCopyright + "\n\n" + e.getMessage)
      case e: FileNotFoundException => Console.err.println("Orc: File not found: " + e.getMessage)
      case e: ScriptException if (e.getCause == null) => Console.err.println(e.getMessage)
      case e: ScriptException => printException(e.getCause, Console.err, false)
    }
  }

  val versionProperties = {
    val p = new java.util.Properties()
    val vp = getClass().getResourceAsStream("version.properties")
    if (vp == null) throw new java.util.MissingResourceException("Unable to load version.properties resource", "/orc/version.properties", "")
    p.load(vp)
    p
  }
  lazy val orcImplName: String = versionProperties.getProperty("orc.title")
  lazy val svnRevision: String = versionProperties.getProperty("orc.svn-revision")
  lazy val orcVersion: String = versionProperties.getProperty("orc.version") + " rev. " + svnRevision + (if (svnRevision.forall(_.isDigit)) "" else " (dev. build " + versionProperties.getProperty("orc.build.date") + " " + versionProperties.getProperty("orc.build.user") + ")")
  lazy val orcURL: String = versionProperties.getProperty("orc.url")
  lazy val orcCopyright: String = "(c) " + copyrightYear + " " + versionProperties.getProperty("orc.vendor")
  lazy val copyrightYear: String = versionProperties.getProperty("orc.copyright-year")

  // Must keep a reference to this logger, or it'll get GC-ed and the level reset
  lazy val orcLogger = java.util.logging.Logger.getLogger("orc")

  def setupLogging(options: OrcOptions) {
    val logLevel = java.util.logging.Level.parse(options.logLevel)
    orcLogger.setLevel(logLevel)
    val testOrcLogRecord = new java.util.logging.LogRecord(logLevel, "")
    testOrcLogRecord.setLoggerName(orcLogger.getName())
    def willLog(checkLogger: java.util.logging.Logger, testLogRecord: java.util.logging.LogRecord): Boolean = {
      for (handler <- checkLogger.getHandlers()) {
        if (handler.isLoggable(testLogRecord))
          return true
      }
      if (checkLogger.getUseParentHandlers() && checkLogger.getParent() != null) {
        return willLog(checkLogger.getParent(), testLogRecord)
      } else {
        return false
      }
    }
    if (!willLog(orcLogger, testOrcLogRecord)) {
      /* Only add handler if no existing handler (here or in parents) is at our logging level */
      val logHandler = new java.util.logging.ConsoleHandler()
      logHandler.setLevel(logLevel)
      orcLogger.addHandler(logHandler)
      orcLogger.warning("No log handler found for 'orc' " + logLevel + " log records, so a ConsoleHandler was added.  This may result in duplicate log records.")
    }
    orcLogger.config(orcImplName + " " + orcVersion)
    orcLogger.config("Orc logging level: " + logLevel)
    //TODO: orcLogger.config(options.printAllTheOptions...)

    val includeStackTracesWithEveryLogMessage = false
    if(includeStackTracesWithEveryLogMessage) {
      for (handler <- orcLogger.getHandlers()) {
        val oldFormatter = handler.getFormatter()
        val formatter = new Formatter() {
          override def format(record: LogRecord) = {
            val tid = record.getThreadID()
            val stack = {
              val frames = (Thread.currentThread().getStackTrace().toVector.drop(2)
                    .dropWhile(!_.getClassName().startsWith("orc"))
                    .takeWhile(_.getMethodName() != "runWorker"))
              val sb = new scala.collection.mutable.StringBuilder()
              for(frame <- frames) {
                sb ++= "\tat " + frame + "\n"
              }
              sb.toString
            }
            oldFormatter.format(record).stripSuffix("\n") + s" tid=$tid\n$stack"
          }
        }
        handler.setFormatter(formatter)
      }
    }
  }

  def printException(e: Throwable, err: PrintStream, showJavaStackTrace: Boolean) {
    e match {
      case je: JavaException if (!showJavaStackTrace) => err.print(je.getMessageAndPositon() + "\n" + je.getOrcStacktraceAsString())
      case oe: OrcException => err.print(oe.getMessageAndDiagnostics())
      case _ => e.printStackTrace(err)
    }
  }
}

/** An OrcOptions that parses command line arguments passed with <code>parseCmdLine(args)</code>.
  *
  * @author jthywiss
  */
trait CmdLineOptions extends OrcOptions with CmdLineParser {
  StringOprd(() => filename, filename = _, position = 0, argName = "file", required = true, usage = "Path to script to execute.")

  StringOpt(() => logLevel, logLevel = _, ' ', "loglevel", usage = "Set the level of logging. Default is INFO. Allowed values: OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL")

  UnitOpt(() => (!usePrelude), () => usePrelude = false, ' ', "noprelude", usage = "Do not implicitly include standard library (prelude), which is included by default.")

  StringListOpt(() => includePath, includePath = _, 'I', "include-path", usage = "Set the include path for Orc includes (same syntax as CLASSPATH). Default is \".\", the current directory. Prelude files are always available for include regardless of this setting.")

  StringListOpt(() => additionalIncludes, additionalIncludes = _, ' ', "additional-includes", usage = "Include these files as if the program had include statements for them (same syntax as CLASSPATH). Default is none.")

  StringListOpt(() => classPath, classPath = _, ' ', "cp", usage = "Set the class path for Orc sites (same syntax as CLASSPATH). This is only used for classes not found in the Java VM classpath.")

  UnitOpt(() => typecheck, () => typecheck = true, ' ', "typecheck", usage = "Enable typechecking, which is disabled by default.")

  UnitOpt(() => disableRecursionCheck, () => disableRecursionCheck = true, ' ', "no-recursion-warn", usage = "Disable unguarded recursion check.")

  UnitOpt(() => echoOil, () => echoOil = true, ' ', "echo-oil", usage = "Write the compiled program in OIL format to stdout.")

  IntOpt(() => echoIR, echoIR = _, ' ', "echo-ir", usage = "Write selected program intermetiate representations to the stdout. The argument is a bitmask. So, 0 means echo nothing, or -1 means echo all.")

  FileOpt(() => oilOutputFile.getOrElse(null), f => oilOutputFile = Some(f), 'o', "output-oil", usage = "Write the compiled program in OIL format to the given filename.")

  UnitOpt(() => runOil, () => runOil = true, ' ', "run-oil", usage = "Attempt to parse the given program as an OIL file and run it. This performs no compilation steps.")

  UnitOpt(() => compileOnly, () => compileOnly = true, 'c', "compile-only", usage = "Compile this program, but do not run it.")

  UnitOpt(() => showJavaStackTrace, () => showJavaStackTrace = true, ' ', "java-stack-trace", usage = "Show Java stack traces on thrown Java exceptions.")

  UnitOpt(() => disableTailCallOpt, () => disableTailCallOpt = true, ' ', "no-tco", usage = "Disable tail call optimization, for easier-to-debug stack traces.")

  IntOpt(() => stackSize, stackSize = _, ' ', "stack-size", usage = "Terminate the program if this stack depth is exceeded. Default=infinity.")

  IntOpt(() => maxTokens, maxTokens = _, ' ', "max-tokens", usage = "Terminate the program if more than this many tokens to be created. Default=infinity.")

  IntOpt(() => maxSiteThreads, maxSiteThreads = _, ' ', "max-site-threads", usage = "Limit the number of simultaneously outstanding site calls to this number. Default=infinity.")

  StringOpt(() => backend.toString, s =>
    backend = BackendType.fromStringOption(s) match {
      case Some(b) => b
      case None => throw new IllegalArgumentException(s"The backend '$s' does not exist or is not supported.")
    }, ' ', "backend", usage = "Set the backend to use for compilation and execution. Allowed value: Token. Default is 'Token'")
}
