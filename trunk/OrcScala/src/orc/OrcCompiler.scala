//
// OrcCompiler.scala -- Scala class OrcCompiler
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

package orc

import java.io.PrintWriter

import scala.util.parsing.input.Reader
import scala.util.parsing.input.StreamReader

import orc.error.compiletime.CompilationException
import orc.error.compiletime.CompileLogger
import orc.error.compiletime.CompileLogger.Severity
import orc.error.compiletime.ParsingException
import orc.error.compiletime.PrintWriterCompileLogger

/**
 * Represents one phase in a compiler.  It is defined as a function from
 * compiler options to a function from a phase's input to its output.
 * CompilerPhases can be composed with the >>> operator.
 *
 * @author jthywiss
 */
trait CompilerPhase[O, A, B] extends (O => A => B) { self =>
  def >>>[C](that: CompilerPhase[O, B, C]) = new CompilerPhase[O, A, C] { 
    override def apply(o: O) = { a: A => that(o)(self.apply(o)(a)) }
  }
}

/**
 * An instance of OrcCompiler is a particular Orc compiler configuration, 
 * which is a particular Orc compiler implementation, in a JVM instance.
 * Note, however, that an OrcCompiler instance is not specialized for
 * a single Orc program; in fact, multiple compilations of different programs,
 * with different options set, may be in progress concurrently within a
 * single OrcCompiler instance.  
 *
 * @author jthywiss
 */
class OrcCompiler extends OrcCompilerAPI {

  val parse = new CompilerPhase[OrcOptions, Reader[Char], orc.ext.Expression] {
    override def apply(options: OrcOptions) = { source =>
      OrcParser.parse(options, source) match {
        case OrcParser.Success(result, _) => result
        case OrcParser.NoSuccess(msg, in) => throw new ParsingException(msg, in.pos)
      }
    }
  }

  val translate = new CompilerPhase[OrcOptions, orc.ext.Expression, orc.oil.Expression] { 
    override def apply(options: OrcOptions) = { ast =>
      orc.translation.Translator.translate(options, ast)
    }
  }

  val typeCheck = new CompilerPhase[OrcOptions, orc.oil.Expression, orc.oil.Expression] {
    override def apply(options: OrcOptions) = { ast => ast }
  }

  val refineOil = new CompilerPhase[OrcOptions, orc.oil.Expression, orc.oil.Expression] {
    override def apply(options: OrcOptions) = { ast => ast }
  }

  val phases = parse >>> translate >>> typeCheck >>> refineOil

  def apply(source: Reader[Char], options: OrcOptions): orc.oil.Expression = {
    try {
      phases(options)(source)
    } catch {case e: CompilationException =>
      compileLogger.recordMessage(Severity.FATAL, 0, e.getMessageOnly, e.pos, null, e)
      null
    } finally {
      compileLogger.endProcessing(options.filename)
    }
  }

  def apply(source: java.io.Reader, options: OrcOptions): orc.oil.Expression = apply(StreamReader(source), options)

  val compileLogger: CompileLogger = new PrintWriterCompileLogger(new PrintWriter(System.err, true))

}
