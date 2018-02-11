/*
 * Copyright (C) 2018 Simer Plaha (@simerplaha)
 *
 * This file is a part of SwayDB.
 *
 * SwayDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * SwayDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with SwayDB. If not, see <https://www.gnu.org/licenses/>.
 */

package benchmark

import com.typesafe.scalalogging.LazyLogging
import swaydb.core.util.Benchmark
import swaydb.data.slice.Slice
import swaydb.serializers.Default.{LongSerializer, StringSerializer}
import swaydb.types.SwayDBMap

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.util.Random

trait BenchmarkBase extends Benchmark with LazyLogging {

  val db: SwayDBMap[Slice[Byte], Slice[Byte]]

  def randomWrite: Boolean

  def randomRead: Boolean

  def forwardIteration: Boolean

  def reverseIteration: Boolean

  def keyValueCount: Long

  def startBenchmark = {
    println(s"\nCreating $keyValueCount test key-values.\n")

    val keys =
      (0L to keyValueCount).map(LongSerializer.write)

    val testValue = StringSerializer.write("Test value of 60 bytes for benchmarking SwayDB's performance")

    lazy val shuffledKeys = Random.shuffle(keys)

    val writeKeys = if (randomWrite) shuffledKeys else keys

    benchmark("Write benchmark") {
      writeKeys foreach {
        key =>
          db.put(key, testValue)
      }
    }

    if (forwardIteration)
      benchmark("Forward iteration benchmark during compaction") {
        db.foreach {
          _ =>
        }
      }
    else if (reverseIteration)
      benchmark("Reverse iteration benchmark during compaction") {
        db.foreachRight {
          case (_, _) =>
        }
      }
    else {
      val readKeys = if (randomRead) shuffledKeys else keys
      benchmark("Read benchmark during compaction") {
        readKeys foreach {
          key =>
            db.get(key)
        }
      }
    }

    def pluralSegment(count: Int) = if (count == 1) "Segment" else "Segments"

    @tailrec
    def areTopLevelsEmpty(levelNumber: Int): Unit =
      db.levelMeter(levelNumber) match {
        case Some(meter) if db.levelMeter(levelNumber + 1).nonEmpty =>
          if (meter.segmentsCount == 0) {
            println(s"Level $levelNumber is empty.")
            areTopLevelsEmpty(levelNumber + 1)
          } else {
            val interval = (levelNumber * 3).seconds
            println(s"Level $levelNumber contains ${meter.segmentsCount} ${pluralSegment(meter.segmentsCount)}. Will check again after $interval.")
            Thread.sleep(interval.toMillis) //<-- For test case so it's ok :)
            areTopLevelsEmpty(levelNumber)
          }
        case _ =>
          val segmentsCount = db.levelMeter(levelNumber).map(_.segmentsCount) getOrElse -1
          println(s"Compaction completed. Level ${levelNumber + 1} contains all $segmentsCount ${pluralSegment(segmentsCount)}.\n")
      }

    println("Waiting for compaction to complete before executing after compaction benchmark.")
    areTopLevelsEmpty(1)

    if (forwardIteration)
      benchmark("Forward iteration benchmark after compaction") {
        db foreach {
          _ =>
        }
      }
    else if (reverseIteration)
      benchmark("Reverse iteration benchmark after compaction") {
        db foreachRight {
          case (_, _) =>
        }
      }
    else {
      val readKeys = if (randomRead) shuffledKeys else keys
      benchmark("Read benchmark after compaction") {
        readKeys foreach {
          key =>
            db.get(key)
        }
      }
    }
  }
}
