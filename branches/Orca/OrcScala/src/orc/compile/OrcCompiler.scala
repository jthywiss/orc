//
// OrcCompiler.scala -- Scala classes CoreOrcCompiler and StandradOrcCompiler
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on May 26, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile

import java.io.{ BufferedReader, File, FileNotFoundException, IOException, PrintWriter, Writer }
import java.net.URI
import orc.{ OrcOptions, OrcCompiler }
import orc.compile.optimize._
import orc.compile.parse.{ OrcResourceInputContext, OrcInputContext, OrcProgramParser, OrcIncludeParser }
import orc.error.compiletime._
import orc.error.compiletime.CompileLogger.Severity
import orc.error.OrcExceptionExtension._
import orc.progress.{ NullProgressMonitor, ProgressMonitor }
import orc.values.sites.SiteClassLoading
import scala.collection.JavaConversions._
import scala.compat.Platform.currentTime


/**
 * Represents a configuration state for a compiler.
 */
class CompilerOptions(val options: OrcOptions, val logger: CompileLogger) {
  
  def reportProblem(exn: CompilationException with ContinuableSeverity) {
    logger.recordMessage(exn.severity, 0, exn.getMessage(), exn.getPosition(), exn)
  }
  
}

/**
 * Represents one phase in a compiler.  It is defined as a function from
 * compiler options to a function from a phase's input to its output.
 * CompilerPhases can be composed with the >>> operator.
 *
 * @author jthywiss
 */
trait CompilerPhase[O, A, B] extends (O => A => B) { self =>
  val phaseName: String
  def >>>[C >: Null](that: CompilerPhase[O, B, C]) = new CompilerPhase[O, A, C] {
    val phaseName = self.phaseName + " >>> " + that.phaseName
    override def apply(o: O) = { a: A =>
      if (a == null) null else {
        val b = self.apply(o)(a)
        if (b == null) null else {
          that(o)(b)
        }
      }
    }
  }
  def timePhase: CompilerPhase[O, A, B] = new CompilerPhase[O, A, B] {
    val phaseName = self.phaseName
    override def apply(o: O) = { a: A =>
      val phaseStart = currentTime
      val b = self.apply(o)(a)
      val phaseEnd = currentTime
      Logger.fine("phase duration: " + phaseName + ": " + (phaseEnd - phaseStart) + " ms")
      b
    }
  }
  def printOut: CompilerPhase[O, A, B] = new CompilerPhase[O, A, B] {
    val phaseName = self.phaseName
    override def apply(o: O) = { a: A =>
      val b = self.apply(o)(a)
      Logger.info(phaseName + " result = " + b.toString())
      b
    }
  }
}

/**
 * An instance of CoreOrcCompiler is a particular Orc compiler configuration, 
 * which is a particular Orc compiler implementation, in a JVM instance.
 * Note, however, that an CoreOrcCompiler instance is not specialized for
 * a single Orc program; in fact, multiple compilations of different programs,
 * with different options set, may be in progress concurrently within a
 * single CoreOrcCompiler instance.  
 *
 * @author jthywiss
 */
abstract class CoreOrcCompiler extends OrcCompiler {

  ////////
  // Definition of the phases of the compiler
  ////////

  val parse = new CompilerPhase[CompilerOptions, OrcInputContext, orc.ast.ext.Expression] {
    val phaseName = "parse"
    @throws(classOf[IOException])
    override def apply(co: CompilerOptions) = { source =>
      val options = co.options
      var includeFileNames = options.additionalIncludes
      if (options.usePrelude) {
        includeFileNames = "prelude.inc" :: (includeFileNames).toList
      }
      val includeAsts = for (fileName <- includeFileNames) yield {
        val ic = openInclude(fileName, null, options)
        OrcIncludeParser(ic, options, CoreOrcCompiler.this) match {
          case r: OrcIncludeParser.SuccessT[_] => r.get.asInstanceOf[OrcIncludeParser.ResultType]
          case n: OrcIncludeParser.NoSuccess => throw new ParsingException(n.msg, n.next.pos)
        }
      }
      val progAst = OrcProgramParser(source, options, CoreOrcCompiler.this) match {
        case r: OrcProgramParser.SuccessT[_] => r.get.asInstanceOf[OrcProgramParser.ResultType]
        case n: OrcProgramParser.NoSuccess => throw new ParsingException(n.msg, n.next.pos)
      }
      (includeAsts :\ progAst) { orc.ast.ext.Declare }
    }
  }

  val translate = new CompilerPhase[CompilerOptions, orc.ast.ext.Expression, orc.ast.oil.named.Expression] {
    val phaseName = "translate"
    @throws(classOf[ClassNotFoundException])
    override def apply(co: CompilerOptions) = 
      { ast =>
          val translator = new orc.compile.translate.Translator(co reportProblem _)
          translator.translate(ast)
      }
  }

  val noUnboundVars = new CompilerPhase[CompilerOptions, orc.ast.oil.named.Expression, orc.ast.oil.named.Expression] {
    val phaseName = "noUnboundVars"
    override def apply(co: CompilerOptions) = { ast =>
      def reportProblem(exn: CompilationException with ContinuableSeverity) {
            co.logger.recordMessage(exn.severity, 0, exn.getMessage(), exn.getPosition(), exn)
          }
      for (x <- ast.unboundvars) {
        co.reportProblem(UnboundVariableException(x.name) at x)
      }
      for (u <- ast.unboundtypevars) {
        co.reportProblem(UnboundTypeVariableException(u.name) at u)
      }
      ast
    }
  }

  val typeCheck = new CompilerPhase[CompilerOptions, orc.ast.oil.named.Expression, orc.ast.oil.named.Expression] {
    val phaseName = "typeCheck"
    override def apply(co: CompilerOptions) = { ast => ast }
  }

  val refineNamedOil = new CompilerPhase[CompilerOptions, orc.ast.oil.named.Expression, orc.ast.oil.named.Expression] {
    val phaseName = "refineNamedOil"
    override def apply(co: CompilerOptions) =
      (e: orc.ast.oil.named.Expression) => {
        val refine = FractionDefs andThen RemoveUnusedDefs andThen RemoveUnusedTypes
        refine(e)
      }
  }

  val noUnguardedRecursion = new CompilerPhase[CompilerOptions, orc.ast.oil.named.Expression, orc.ast.oil.named.Expression] {
    val phaseName = "noUnguardedRecursion"
    override def apply(co: CompilerOptions) = 
      { ast =>
          def warn(e: orc.ast.oil.named.Expression) = {
            co.reportProblem(UnguardedRecursionException() at e)
          }
          ast.checkGuarded(warn)
          ast 
      }
  }

  val deBruijn = new CompilerPhase[CompilerOptions, orc.ast.oil.named.Expression, orc.ast.oil.nameless.Expression] {
    val phaseName = "deBruijn"
    override def apply(co: CompilerOptions) = { ast => ast.withoutNames }
  }

  ////////
  // Compose phases into a compiler
  ////////

  val phases =
    parse.timePhase >>>
      translate.timePhase >>>
      noUnboundVars.timePhase >>>
      typeCheck.timePhase >>>
      refineNamedOil.timePhase >>>
      noUnguardedRecursion.timePhase >>>
      deBruijn.timePhase

  ////////
  // Compiler methods
  ////////

  @throws(classOf[IOException])
  @throws(classOf[ClassNotFoundException])
  def apply(source: OrcInputContext, options: OrcOptions, compileLogger: CompileLogger, progress: ProgressMonitor): orc.ast.oil.nameless.Expression = {
    //Logger.config(options)
    Logger.config("Begin compile "+options.filename)
    compileLogger.beginProcessing(options.filename)
    try {
      val result = phases(new CompilerOptions(options, compileLogger))(source)
      if (compileLogger.getMaxSeverity().ordinal() >= Severity.ERROR.ordinal()) null else result
    } catch {
      case e: CompilationException =>
        compileLogger.recordMessage(Severity.FATAL, 0, e.getMessage, e.getPosition(), null, e)
        null
    }
    finally {
      compileLogger.endProcessing(options.filename)
      Logger.config("End compile "+options.filename)
    }
  }

}

/**
 * StandardOrcCompiler extends CoreOrcCompiler with "standard" environment interfaces. 
 *
 * @author jthywiss
 */
class StandardOrcCompiler() extends CoreOrcCompiler with SiteClassLoading {
  @throws(classOf[IOException])
  @throws(classOf[ClassNotFoundException])
  override def apply(source: OrcInputContext, options: OrcOptions, compileLogger: CompileLogger, progress: ProgressMonitor): orc.ast.oil.nameless.Expression = {
    SiteClassLoading.initWithClassPathStrings(options.classPath)
    super.apply(source, options, compileLogger, progress)
  }

  private class OrcReaderInputContext(val javaReader: java.io.Reader, override val descr: String) extends OrcInputContext {
    val file = new File(descr)
    override val reader = orc.compile.parse.OrcReader(new BufferedReader(javaReader), descr)
    override def toURI = file.toURI
    override def toURL = toURI.toURL
  }

  @throws(classOf[IOException])
  @throws(classOf[ClassNotFoundException])
  def apply(source: java.io.Reader, options: OrcOptions, err: Writer): orc.ast.oil.nameless.Expression = {
    this(new OrcReaderInputContext(source, options.filename), options, new PrintWriterCompileLogger(new PrintWriter(err, true)), NullProgressMonitor)
  }

  private object OrcNullInputContext extends OrcInputContext {
    override val descr = ""
    override val reader = null
    override val toURI = new URI("")
    override def toURL = throw new UnsupportedOperationException("OrcNullInputContext.toURL")
  }

  @throws(classOf[IOException])
  def openInclude(includeFileName: String, relativeTo: OrcInputContext, options: OrcOptions): OrcInputContext = {
    val baseIC = if (relativeTo != null) relativeTo else OrcNullInputContext
    Logger.finer("openInclude "+includeFileName+", relative to "+baseIC.getClass.getCanonicalName+"("+baseIC.descr+")")

    // Try filename under the include path list
    for (incPath <- scala.collection.JavaConversions.asIterable(options.includePath)) {
      try {
        //FIXME: Security implications of including local files:
        // For Orchard's sake, OrcJava disallowed relative file names
        // in certain cases, to prevent examining files by including
        // them.  This seems a weak barrier, and in fact was broken.
        // We need an alternative way to control local file reads.
        val newIC = baseIC.newInputFromPath(incPath, includeFileName)
        Logger.finer("include "+includeFileName+", found on include path entry "+incPath+", opened as "+newIC.getClass.getCanonicalName+"("+newIC.descr+")")
        return newIC
      } catch {
        case _: IOException => /* Ignore, must not be here */
      }
    }

    // Try in the bundled include resources
    try {
      val newIC = new OrcResourceInputContext("orc/lib/includes/" + includeFileName, getResource)
        Logger.finer("include "+includeFileName+", found in bundled resources, opened as "+newIC.getClass.getCanonicalName+"("+newIC.descr+")")
        return newIC
    } catch {
      case _: IOException => /* Ignore, must not be here */
    }

    Logger.finer("include "+includeFileName+" not found")
    throw new FileNotFoundException("Include file '" + includeFileName + "' not found; check the include path.");
  }
}