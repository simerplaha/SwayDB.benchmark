package swaydb.benchmark.readwrite

import java.nio.file.Paths

import swaydb.benchmark.readwrite.CommonSettings._
import swaydb.benchmark.util.FileUtils
import swaydb.core.segment.ThreadReadState
import swaydb.core.util.Benchmark
import swaydb.data.accelerate.Accelerator
import swaydb.data.config.{IndexFormat, MMAP, PrefixCompression, RandomKeyIndex}
import swaydb.data.order.KeyOrder
import swaydb.data.slice.Slice
import swaydb.data.util.StorageUnits._
import swaydb.serializers.Default._
import swaydb.{Bag, IO}

import scala.util.Random

/**
 * SwayDB's benchmark test.
 *
 * Here SwayDB is configured to generate
 */
object SwayDB_Benchmark extends App {

  //This flag will run an unfair benchmark where SwayDB
  //configures it's HashIndex to be nearly perfect.
  //This required more disk space but random read performance doubles.
  //This should be set to false for fair benchmarking with RocksDB.
  val RUN_UNFAIR_READ_BENCHMARK = false

  //SwayDB configuration that caches block bytes on access.
  val enableCacheOnAccess = true

  //number of key-values per Segment Group - A Segment group is a mini Segment within a Segment file with
  //dedicated secondary indexes.
  val maxKeyValuesPerSegmentGroup = Int.MaxValue

  println(s"RUN_UNFAIR_READ_BENCHMARK: $RUN_UNFAIR_READ_BENCHMARK")
  println(s"cacheOnAccess: $enableCacheOnAccess")
  println(s"maxKeyValuesPerSegmentGroup: $maxKeyValuesPerSegmentGroup")

  //database directory.
  val dir = Paths.get("target").resolve("dbs").resolve(this.getClass.getSimpleName.takeWhile(_ != '$'))

  /**
   * Partial untyped ordering for Set database.
   * Don't let this scare you. You can use typed ordering
   * in your application but untyped byte ordering should be
   * used for better performance.
   *
   * See http://swaydb.io/ordering-data/?language=scala/
   */
  implicit val prefixKeyOrdering =
    Left(
      new KeyOrder[Slice[Byte]] {
        def compare(a: Slice[Byte], b: Slice[Byte]): Int = {
          var i = 0
          while (i < 8) {
            val aB = a.getC(i) & 0xFF
            val bB = b.getC(i) & 0xFF
            if (aB != bB) return aB - bB
            i += 1
          }
          0
        }

        override def indexableKey(data: Slice[Byte]): Slice[Byte] =
          data.take(8)
      }
    )

  //configuration for HashIndex
  val randomKeyIndex =
    if (RUN_UNFAIR_READ_BENCHMARK)
      swaydb.persistent.DefaultConfigs.randomKeyIndex(enableCacheOnAccess).copy(
        maxProbe = 20,
        minimumNumberOfKeys = 10,
        minimumNumberOfHits = 10,
        indexFormat = IndexFormat.Reference,
        allocateSpace = _.requiredSpace * 3
      )
    else
      RandomKeyIndex.Disable

  //configuration for values block
  val valuesConfig =
    swaydb.persistent.DefaultConfigs.valuesConfig(enableCacheOnAccess).copy(
      compressDuplicateValues = false,
      compressDuplicateRangeValues = false
    )

  val prefixCompression =
    if (RUN_UNFAIR_READ_BENCHMARK)
      PrefixCompression.Disable(false)
    else
    //this configuration prefix compresses enough key-values to result in the same data output as RocksDB
      PrefixCompression.Enable(
        keysOnly = true,
        interval = PrefixCompression.Interval.ResetCompressionAt(4)
      )

  //configuration for Sorted index block
  val sortedKeyIndex =
    swaydb.persistent.DefaultConfigs.sortedKeyIndex(enableCacheOnAccess).copy(
      enablePositionIndex = RUN_UNFAIR_READ_BENCHMARK,
      prefixCompression = prefixCompression
    )

  //configuration for segment
  val segmentConfig =
    swaydb.persistent.DefaultConfigs.segmentConfig(enableCacheOnAccess).copy(
      deleteSegmentsEventually = false,
      minSegmentSize = 40.mb,
      maxKeyValuesPerSegment = maxKeyValuesPerSegmentGroup,
      mmap =
        if (mmap)
          MMAP.WriteAndRead
        else
          MMAP.Disabled
    )

  //initialise a Set database.
  val set =
    swaydb.persistent.Set[Long, Nothing, Bag.Less](
      dir = dir,
      mapSize = 60.mb, //set memory map for levelZero log/map files.
      mmapMaps = mmap, //max size of levelZero log/map files
      cacheKeyValueIds = true,
      acceleration = Accelerator.cruise,
      randomKeyIndex = randomKeyIndex,
      valuesConfig = valuesConfig,
      sortedKeyIndex = sortedKeyIndex,
      segmentConfig = segmentConfig
    ) match {
      case IO.Right(set) =>
        set

      case IO.Left(error: swaydb.Error.Boot) =>
        throw new Exception("Failed to start SwayDB", error.exception) //for this test simply throw to exit.
    }

  val readState = ThreadReadState.limitHashMap(1000, 10)

  /**
   * Comment out any of the following function to disable benchmarking them.
   */
  runWriteBenchmark()
  //  runGetBenchmark()
  //  runForwardIterationBenchmark()
  //  runReverseIterationBenchmark()

  /**
   * SEQUENTIAL WRITES
   *
   * SEG files size: 204.52 mb - 204521235 bytes
   *
   * 8 Segment files
   *
   * When MMAP = true
   * 20.715146446 seconds - SwayDB write benchmark.
   * 21.44269499 seconds - SwayDB write benchmark.
   * 22.127488887 seconds - SwayDB write benchmark.
   * 21.20356898 seconds - SwayDB write benchmark.
   * 20.939119096 seconds - SwayDB write benchmark.
   *
   * When MMAP = false
   * 52.136054721 seconds - SwayDB write benchmark.
   * 52.999447377 seconds - SwayDB write benchmark.
   * 53.773065363 seconds - SwayDB write benchmark.
   * 54.260150911 seconds - SwayDB write benchmark.
   * 54.99077458 seconds - SwayDB write benchmark.
   *
   * RANDOM WRITES
   *
   * When MMAP = true
   * 63.488582954 seconds - SwayDB write benchmark.
   * 66.585545349 seconds - SwayDB write benchmark.
   * 66.215357379 seconds - SwayDB write benchmark.
   * 64.242695952 seconds - SwayDB write benchmark.
   * 63.702168366 seconds - SwayDB write benchmark.
   *
   * When MMAP = false
   * 99.848123745 seconds - SwayDB write benchmark.
   * 99.316002878 seconds - SwayDB write benchmark.
   * 97.102210007 seconds - SwayDB write benchmark.
   * 102.712519441 seconds - SwayDB write benchmark.
   * 102.372401551 seconds - SwayDB write benchmark.
   */
  def runWriteBenchmark() = {
    val keysToWrite =
      if (randomPut)
        Benchmark("Shuffling key-values")(Random.shuffle(setItems))
      else
        setItems

    Benchmark(s"SwayDB write benchmark") {
      keysToWrite foreach {
        key =>
          set.core.zero.put(key, Slice.Null)
      }
    }

    FileUtils.printFilesSize(dir, "seg")
  }

  /**
   * SEQUENTIAL READS (cacheOnAccess = true)
   *
   * Round 1
   * 13.548759143 seconds - 1 - SwayDB get benchmark.
   * 11.941612547 seconds - 2 - SwayDB get benchmark.
   * 11.78899835 seconds - 3 - SwayDB get benchmark.
   *
   * Round 2
   * 14.849480291 seconds - 1 - SwayDB get benchmark.
   * 11.619554216 seconds - 2 - SwayDB get benchmark.
   * 11.584474098 seconds - 3 - SwayDB get benchmark.
   *
   * Round 3
   * 13.925143285 seconds - 1 - SwayDB get benchmark.
   * 11.912952933 seconds - 2 - SwayDB get benchmark.
   * 11.655096141 seconds - 3 - SwayDB get benchmark.
   *
   * SEQUENTIAL READS (cacheOnAccess = false)
   *
   * Round 1
   * 13.812497821 seconds - 1 - SwayDB get benchmark.
   * 12.629604953 seconds - 2 - SwayDB get benchmark.
   * 11.831168668 seconds - 3 - SwayDB get benchmark.
   *
   * Round 2
   * 14.263097391 seconds - 1 - SwayDB get benchmark.
   * 13.032556002 seconds - 2 - SwayDB get benchmark.
   * 12.506409074 seconds - 3 - SwayDB get benchmark.
   *
   * Round 3
   * 14.272494305 seconds - 1 - SwayDB get benchmark.
   * 13.03650703 seconds - 2 - SwayDB get benchmark.
   * 13.012433074 seconds - 3 - SwayDB get benchmark.
   *
   * RANDOM READS (cacheOnAccess = true)
   * Round 1
   * 47.247402546 seconds - 1 - SwayDB get benchmark.
   * 45.026367624 seconds - 2 - SwayDB get benchmark.
   * 44.678418615 seconds - 3 - SwayDB get benchmark.
   *
   * Round 2
   * 47.513549441 seconds - 1 - SwayDB get benchmark.
   * 43.924893289 seconds - 2 - SwayDB get benchmark.
   * 43.762267853 seconds - 3 - SwayDB get benchmark.
   *
   * Round 3
   * 47.383432178 seconds - 1 - SwayDB get benchmark.
   * 44.886855489 seconds - 2 - SwayDB get benchmark.
   * 44.94707189 seconds - 3 - SwayDB get benchmark.
   *
   * RANDOM READS (cacheOnAccess = false)
   *
   * Round 1
   * 52.284672032 seconds - 1 - SwayDB get benchmark.
   * 49.351267318 seconds - 2 - SwayDB get benchmark.
   * 50.536640486 seconds - 3 - SwayDB get benchmark.
   *
   * Round 2
   * 49.414085604 seconds - 1 - SwayDB get benchmark.
   * 47.883605036 seconds - 2 - SwayDB get benchmark.
   * 47.894993951 seconds - 3 - SwayDB get benchmark.
   *
   * Round 3
   * 51.931299744 seconds - 1 - SwayDB get benchmark.
   * 50.689814772 seconds - 2 - SwayDB get benchmark.
   * 50.603626991 seconds - 3 - SwayDB get benchmark.
   *
   *
   * RUN_UNFAIR_READ_BENCHMARK (on 5 million key-values)
   *
   * 13.400631583 seconds - 1 - SwayDB get benchmark.
   * 9.911291557 seconds - 2 - SwayDB get benchmark.
   * 9.708567325 seconds - 3 - SwayDB get benchmark.
   *
   */
  def runGetBenchmark() = {
    val partialKeys =
      if (randomGet)
        Benchmark("Shuffling")(Random.shuffle(setKeys))
      else
        setKeys

    FileUtils.printFilesSize(dir, "seg")

    (1 to 3) foreach {
      count =>
        Benchmark(s"$count - SwayDB get benchmark") {
          partialKeys foreach {
            key =>
              set.core.zero.get(key, readState)
          }
        }
    }
  }

  /**
   * Round 1
   * 13.798430441 seconds - 1 - SwayDB forward iterator benchmark.
   * 13.394958941 seconds - 2 - SwayDB forward iterator benchmark.
   * 14.637962961 seconds - 3 - SwayDB forward iterator benchmark.
   *
   * Round 2
   * 15.233245433 seconds - 1 - SwayDB forward iterator benchmark.
   * 12.880626918 seconds - 2 - SwayDB forward iterator benchmark.
   * 12.545963602 seconds - 3 - SwayDB forward iterator benchmark.
   *
   * Round 3
   * 15.823790869 seconds - 1 - SwayDB forward iterator benchmark.
   * 13.454271074 seconds - 2 - SwayDB forward iterator benchmark.
   * 13.53381088 seconds - 3 - SwayDB forward iterator benchmark.
   */

  def runForwardIterationBenchmark() =
    (1 to 3) foreach {
      count =>
        Benchmark(s"$count - SwayDB forward iterator benchmark") {
          set
            .core
            .zero
            .iterator(set.core.readStates.get())
            .foreach(_ => ())
        }
    }

  /**
   * Round 1
   * 33.436125158 seconds - 1 - SwayDB reverse iterator benchmark.
   * 33.146549189 seconds - 2 - SwayDB reverse iterator benchmark.
   * 34.384421269 seconds - 3 - SwayDB reverse iterator benchmark.
   *
   * Round 2
   * 34.881512185 seconds - 1 - SwayDB reverse iterator benchmark.
   * 34.332373254 seconds - 2 - SwayDB reverse iterator benchmark.
   * 34.362186249 seconds - 3 - SwayDB reverse iterator benchmark.
   */
  def runReverseIterationBenchmark() =
    (1 to 3) foreach {
      count =>
        Benchmark(s"$count - SwayDB reverse iterator benchmark") {
          set
            .core
            .zero
            .reverseIterator(set.core.readStates.get())
            .foreach(_ => ())
        }
    }
}
