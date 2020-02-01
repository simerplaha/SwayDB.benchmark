package swaydb.benchmark.storage

import java.nio.file.Paths

import swaydb.benchmark.storage.CommonSettings._
import swaydb.benchmark.util.FileUtils
import swaydb.core.util.Benchmark
import swaydb.data.accelerate.Accelerator
import swaydb.data.compression.{LZ4Compressor, LZ4Decompressor, LZ4Instance}
import swaydb.data.config.{BinarySearchIndex, MightContainIndex, PrefixCompression, RandomKeyIndex}
import swaydb.data.order.KeyOrder
import swaydb.serializers.Default._
import swaydb.{Bag, Compression, IO}

import scala.concurrent.duration._

object SwayDB_Benchmark_Space extends App {

  implicit val ordering = KeyOrder.default //import default sorting

  val dir = Paths.get("target").resolve("dbs").resolve(this.getClass.getSimpleName.takeWhile(_ != '$'))

  val cacheOnAccess = false

  val db =
    swaydb.persistent.Map[Int, String, Nothing, Bag.Less](
      dir = dir,
      mapSize = level0LogSize, //set memory map for levelZero log/map files.
      mmapMaps = mmap, //max size of levelZero log/map files
      cacheKeyValueIds = true,
      acceleration = Accelerator.cruise,
      randomKeyIndex = RandomKeyIndex.Disable,
      mightContainKeyIndex = MightContainIndex.Disable,
      binarySearchIndex = BinarySearchIndex.Disable(false),
      sortedKeyIndex =
        swaydb.persistent.DefaultConfigs.sortedKeyIndex(cacheOnAccess = cacheOnAccess).copy(
          enablePositionIndex = false,
          prefixCompression =
            PrefixCompression.Enable(
              keysOnly = false,
              interval = PrefixCompression.Interval.CompressAt(1)
            )
        ),
      valuesConfig =
        swaydb.persistent.DefaultConfigs.valuesConfig(cacheOnAccess = cacheOnAccess).copy(
          compressDuplicateValues = true,
          compressDuplicateRangeValues = true
        ),
      segmentConfig =
        swaydb.persistent.DefaultConfigs.segmentConfig(cacheOnAccess = cacheOnAccess).copy(
          minSegmentSize = segmentSize, //set minSegmentSize
          maxKeyValuesPerSegment = Int.MaxValue,
          compression =
            _ =>
              Seq(
                Compression.LZ4(
                  compressor = (LZ4Instance.Native, LZ4Compressor.High(minCompressionSavingsPercent = 20.0)),
                  decompressor = (LZ4Instance.Native, LZ4Decompressor.Safe)
                )
              )
        ),
      levelZeroThrottle = _ => Duration.Zero //disable hashIndex
    ) match {
      case IO.Right(set) =>
        set

      case IO.Left(error: swaydb.Error.Boot) =>
        throw new Exception("Failed to start SwayDB", error.exception) //for this test simply throw to exit.
    }

  val keyValues = keyValuesSlice

  /**
   * When keyValues = 1000000
   * 11.92 mb - 11919852 bytes - WITHOUT compression
   * 3.94 mb - 3938698 bytes - WITH LZ4 compression
   */
  Benchmark(s"SwayDB write benchmark - PERSISTENT. keyValues = ${keyValues.size}, mmapWrites = $mmap, enableHashIndex = $enableHashIndex, cacheOnAccess = $cacheOnAccess") {
    keyValues foreach {
      case (key, value) =>
        db.core.zero.put(key, value)
    }
  }

  FileUtils.printFilesSize(dir, "seg")
}
