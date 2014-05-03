//
// BenchmarkTest.scala -- Scala class/trait/object BenchmarkTest
// Project OrcTests
//
// $Id$
//
// Created by amp on Oct 27, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.test

import orc.test.TestUtils.OrcTestCase
import orc.error.compiletime.CompilationException
import orc.error.OrcException
import java.util.concurrent.TimeoutException
import java.io.FileNotFoundException
import orc.script.OrcScriptEngine
import orc.script.OrcBindings
import java.io.File
import orc.BackendType
import java.util.ArrayList
import java.util.Date
import scala.collection.JavaConversions._
import java.io.IOException
import java.text.SimpleDateFormat
import java.io.OutputStreamWriter
import java.io.FileOutputStream
import orc.util.SynchronousThreadExec
import orc.PorcInterpreterBackend
import orc.TokenInterpreterBackend

/** @author amp
  */
object AllBenchmarkTests {
  case class AllBenchmarkConfig(cpuCounts: Seq[Int], backends: Seq[BackendType], optLevels: Seq[Int],
    output: File,
    timeout: Long = 180L, nRuns: Int = 5, nDroppedRuns: Int = 2, outputCompileTime: Boolean = false,
    jvmArguments: Seq[String] = Nil) {
    def configs = {
      val cs = for (cpuCount <- cpuCounts; optLevel <- optLevels; backend <- backends) yield {
        BenchmarkConfig(0 until cpuCount, backend, optLevel, timeout = timeout, nRuns = nRuns,
          nDroppedRuns = nDroppedRuns, outputCompileTime = outputCompileTime, output = output,
          outputHeader = false)
      }

      cs.head.copy(outputHeader = true) +: cs.tail
    }
  }

  def parseRangeList(arg: String) = {
    arg.split(",").toSeq.flatMap({ s =>
      if (s contains "-") {
        val vs = s.split("-")
        (vs(0).toInt to vs(1).toInt)
      } else {
        Seq(s.toInt)
      }
    })
  }

  val dateFormatter = new SimpleDateFormat("yyyy-MM-dd.HH-mm-ss")

  def main(args: Array[String]) {
    def processArgs(args: Seq[String], conf: AllBenchmarkConfig): AllBenchmarkConfig = args match {
      case "-c" +: arg +: rest =>
        processArgs(rest, conf.copy(cpuCounts = parseRangeList(arg)))
      case "-B" +: arg +: rest =>
        processArgs(rest, conf.copy(backends = arg.split(",").map(BackendType.fromString)))
      case "-O" +: arg +: rest =>
        processArgs(rest, conf.copy(optLevels = parseRangeList(arg)))
      case "-t" +: arg +: rest =>
        processArgs(rest, conf.copy(timeout = arg.toLong))
      case "-r" +: arg +: rest =>
        processArgs(rest, conf.copy(nRuns = arg.toInt))
      case "-d" +: arg +: rest =>
        processArgs(rest, conf.copy(nDroppedRuns = arg.toInt))
      case "-C" +: rest =>
        processArgs(rest, conf.copy(outputCompileTime = true))
      case "+J" +: rest if rest.contains("-J") =>
        val i = rest.indexOf("-J")
        processArgs(rest.drop(i), conf.copy(jvmArguments = rest.take(i - 1)))
      case "-o" +: arg +: rest =>
        processArgs(rest, conf.copy(output = new File(arg).getAbsoluteFile()))
      case b if b.size == 0 =>
        conf
    }

    val defaultOutputFile = new File(s"allbenchmarks-${dateFormatter.format(new Date())}.csv")
    implicit val config = processArgs(args, AllBenchmarkConfig(
      Seq(1, 2, 4, 8).reverse, Seq(TokenInterpreterBackend, PorcInterpreterBackend),
      Seq(0, 1, 3).reverse, nRuns = 7, nDroppedRuns = 2, output = defaultOutputFile))

    println("Running configs:\n" + config.configs.map(_.asArguments.mkString(" ")).mkString("\n"))
      
    for (conf <- config.configs) {
      import scala.sys.process._
      val bootpath = if (System.getProperty("sun.boot.class.path") ne null)
        System.getProperty("path.separator", ":") + System.getProperty("sun.boot.class.path")
      else
        ""
      val cmd = Seq("java", "-cp", System.getProperty("java.class.path") + bootpath) ++
        Seq("-Xss3M") ++ config.jvmArguments ++ 
        Seq(BenchmarkTest.getClass().getCanonicalName().stripSuffix("$")) ++ conf.asArguments
      println(s"Running: ${cmd.mkString(" ")}")
      def retry(n: Int): Unit = if(n > 0){
          val result = cmd.!
          if(result != 0) {
            println(s"Retrying failed run: ${cmd.mkString(" ")}")
            retry(n-1)
          }
      }
      retry(3)
    }
  }
}