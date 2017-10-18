package orc.test.item.scalabenchmarks.dedup

import java.io.{ DataOutputStream, FileInputStream, FileOutputStream }
import java.util.concurrent.ConcurrentHashMap

import orc.test.item.scalabenchmarks.{ BenchmarkApplication, Util }
import Util.thread
import java.util.concurrent.ArrayBlockingQueue
import scala.annotation.tailrec
import java.io.File

object DedupBoundedQueue extends BenchmarkApplication[Unit] {
  import Dedup._
  
  def dedup(inFn: String, outFn: String): Unit = {
    val dedupMap = new ConcurrentHashMap[ArrayKey, CompressedChunk]()
    val roughChunks = new ArrayBlockingQueue[(Chunk, Int)](1024)
    val fineChunks = new ArrayBlockingQueue[(Chunk, Int, Int)](2 * 1025)
    val compressedChunks = new ArrayBlockingQueue[(CompressedChunk, Int, Int)](2 * 1024)
        
    val in = new FileInputStream(inFn)
    
    val readThread = thread {
      for (p <- readSegments(largeChunkMin, in)) {
        roughChunks.put(p)   
      }
    }
    val segmentThreads = for (_ <- 0 until 8) yield thread {
      while (true) {
        val (roughChunk, roughID) = roughChunks.take()
        for ((fineChunk, fineID) <- segment(0, roughChunk)) {
          fineChunks.put((fineChunk, roughID, fineID))
        }
      }
    }
    val compressThreads = for (_ <- 0 until 8) yield thread {
      while (true) {
        val (fineChunk, roughID, fineID) = fineChunks.take()
        compressedChunks.put((compress(fineChunk, dedupMap), roughID, fineID))
      }
    }

    val out = new DataOutputStream(new FileOutputStream(outFn))
    val alreadyOutput = new ConcurrentHashMap[ArrayKey, Boolean]()
    val outputPool = collection.mutable.HashMap[(Int, Int), CompressedChunk]()
    
    @tailrec
    def doOutput(roughID: Int, fineID: Int, id: Int): Unit = {
      outputPool.get((roughID, fineID)) match {
        case Some(cchunk) if cchunk.uncompressedSize == 0 && fineID == 0 => {
          outputPool -= ((roughID, fineID))
        }
        case Some(cchunk) if cchunk.uncompressedSize == 0 => {
          outputPool -= ((roughID, fineID))
          doOutput(roughID + 1, 0, id)
        }
        case Some(cchunk) => {
          cchunk.outputChunkID = id
    			writeChunk(out, cchunk, alreadyOutput.containsKey(cchunk.uncompressedSHA1))
    			alreadyOutput.put(cchunk.uncompressedSHA1, true)
    			//print(s"$id: ($roughID, $fineID) $roughChunk (${roughChunk.size}), $fineChunk (${fineChunk.size})\r")
          outputPool -= ((roughID, fineID))
          doOutput(roughID, fineID + 1, id + 1)
        }
        case None => {
          val (cchunk, rID, fID) = compressedChunks.take()
          outputPool += (rID, fID) -> cchunk
          doOutput(roughID, fineID, id)
        }
      }
    }
    
    doOutput(0, 0, 0)
    
    readThread.join()
    segmentThreads foreach { _.terminate() }
    compressThreads foreach { _.terminate() }
  }
  
  def benchmark(ctx: Unit): Unit = {
    // FIXME: Generate or include data.
    dedup("test.in", "test.out")
  }

  def setup(): Unit = ()

  val name: String = "Dedup-boundedqueue"

  val size: Int = new File("test.in").length().toInt
}