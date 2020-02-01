package swaydb.benchmark.readwrite

import java.nio.file.{Files, Paths}

import org.rocksdb.{RocksDB, _}
import swaydb.benchmark.readwrite.CommonSettings._
import swaydb.benchmark.util.FileUtils
import swaydb.core.util.Benchmark

import scala.util.Random

/**
 * The following benchmarks RocksDB's put, get and iteration
 *
 * The goal is to keep the default configuration of RocksDB since it's already
 * tuned for performance and configure [[SwayDB_Benchmark]] such that they
 * both generate same size data files, so that the benchmarks are fair.
 */
object RocksDB_Benchmark extends App {

  val dir = Paths.get("target").resolve("dbs").resolve(this.getClass.getSimpleName.takeWhile(_ != '$'))
  if (Files.notExists(dir)) Files.createDirectories(dir)

  //Following the documentationhttps://github.com/facebook/rocksdb/wiki/RocksJava-Basics#opening-a-database
  //this initialises RocksDB with default configuration.
  RocksDB.loadLibrary()

  val options: Options = new Options().setCreateIfMissing(true)

  options.setCreateIfMissing(true)
    .setCompressionType(CompressionType.NO_COMPRESSION)
    .setCompactionStyle(CompactionStyle.LEVEL)
    //Reading https://github.com/facebook/rocksdb/wiki/IO#memory-mapping?
    .setAllowMmapWrites(mmap)
    .setAllowMmapReads(mmap)

  val tableConfig = new BlockBasedTableConfig()
  tableConfig.setDataBlockIndexType(DataBlockIndexType.kDataBlockBinarySearch)
  options.setTableFormatConfig(tableConfig)

  var db: RocksDB = _

  try {
    db = RocksDB.open(options, dir.toString)

    /**
     * Comment out any of the following to disable specific benchmarking.
     *
     * The benchmark results were generated running each test independently
     * and not by running them all.
     */
    runWriteBenchmark()
    //    runGetBenchmark()
    //    runForwardIteratorBenchmark()
    //    runReverseIteratorBenchmark()
  } finally {
    db.closeE()
  }

  /**
   * Benchmark outcome
   *
   * Setting MMAP to true does not seems to improve write performance, not sure why.
   * Reference - https://github.com/facebook/rocksdb/wiki/IO#memory-mapping?
   *
   * SST files size: 209.28 mb - 209276128 bytes
   *
   * 8 SST files
   *
   * Sequential writes when MMAP = true
   * 45.813645809 seconds - RocksDB write benchmark
   * 50.508779181 seconds - RocksDB write benchmark
   * 45.266165614 seconds - RocksDB write benchmark
   * 47.660416393 seconds - RocksDB write benchmark
   * 45.503487944 seconds - RocksDB write benchmark
   *
   * Sequential writes when MMAP = false
   * 46.395437331 seconds - RocksDB write benchmark
   * 49.042667069 seconds - RocksDB write benchmark
   * 47.568988724 seconds - RocksDB write benchmark
   * 46.659120858 seconds - RocksDB write benchmark
   * 48.190332371 seconds - RocksDB write benchmark
   *
   * Random writes when MMAP = true
   * 63.596482965 seconds - RocksDB write benchmark
   * 62.331631868 seconds - RocksDB write benchmark
   * 64.550185832 seconds - RocksDB write benchmark
   * 63.649219735 seconds - RocksDB write benchmark
   * 64.820218425 seconds - RocksDB write benchmark
   *
   * Random writes when MMAP = false
   * 64.131865702 seconds - RocksDB write benchmark
   * 67.310220168 seconds - RocksDB write benchmark
   * 64.91836709 seconds - RocksDB write benchmark
   * 62.431859886 seconds - RocksDB write benchmark
   * 68.371243788 seconds - RocksDB write benchmark
   */
  def runWriteBenchmark(): Unit = {
    val keyValuesToWrite =
      if (randomPut)
        Random.shuffle(keyValues)
      else
        keyValues

    Benchmark(s"RocksDB write benchmark") {
      keyValuesToWrite foreach {
        case (key, value) =>
          db.put(key, value)
      }
    }

    //print the disk space taken by all sst files.
    FileUtils.printFilesSize(dir, "sst")
  }

  /**
   * SEQUENTIAL READS
   *
   * Round 1
   * 18.542748423 seconds - 1 - RocksDB get benchmark
   * 17.714711606 seconds - 2 - RocksDB get benchmark
   * 18.429583195 seconds - 3 - RocksDB get benchmark
   *
   * Round 2
   * 19.931010065 seconds - 1 - RocksDB get benchmark
   * 18.099922013 seconds - 2 - RocksDB get benchmark
   * 18.322394244 seconds - 3 - RocksDB get benchmark
   *
   * Round 3
   * 18.338736555 seconds - 1 - RocksDB get benchmark
   * 20.713966147 seconds - 2 - RocksDB get benchmark
   * 18.321136763 seconds - 3 - RocksDB get benchmark
   *
   * RANDOM READS
   *
   * Round 1
   * 87.591932011 seconds - 1 - RocksDB get benchmark.
   * 88.467268891 seconds - 2 - RocksDB get benchmark.
   * 88.010611396 seconds - 3 - RocksDB get benchmark.
   *
   * Round 2
   * 92.670774118 seconds - 1 - RocksDB get benchmark.
   * 90.353062746 seconds - 2 - RocksDB get benchmark.
   * 91.179779257 seconds - 3 - RocksDB get benchmark.
   *
   * Round 3
   * 90.38328082 seconds - 1 - RocksDB get benchmark.
   * 90.616760827 seconds - 2 - RocksDB get benchmark.
   * 91.841050684 seconds - 3 - RocksDB get benchmark.
   */
  def runGetBenchmark(): Unit = {
    val keyVals =
      if (randomGet)
        Benchmark("Shuffling key-values")(Random.shuffle(keyValues))
      else
        keyValues

    FileUtils.printFilesSize(dir, "sst")

    (1 to 3) foreach {
      count =>
        Benchmark(s"$count - RocksDB get benchmark") {
          keyVals foreach {
            case (key, _) =>
              db.get(key)
          }
        }
    }
  }

  /**
   * Round 1
   * 6.250122786 seconds - 1 - RocksDB forward iterator benchmark.
   * 3.933878549 seconds - 2 - RocksDB forward iterator benchmark.
   * 3.9904791 seconds - 3 - RocksDB forward iterator benchmark.
   *
   * Round 2
   * 4.211968881 seconds - 1 - RocksDB forward iterator benchmark.
   * 6.505442164 seconds - 2 - RocksDB forward iterator benchmark.
   * 3.955146595 seconds - 3 - RocksDB forward iterator benchmark.
   *
   * Round 2
   * 6.334389409 seconds - 1 - RocksDB forward iterator benchmark.
   * 3.869475149 seconds - 2 - RocksDB forward iterator benchmark.
   * 3.969485287 seconds - 3 - RocksDB forward iterator benchmark.
   */
  def runForwardIteratorBenchmark(): Unit =
    (1 to 3) foreach {
      count =>
        Benchmark(s"$count - RocksDB forward iterator benchmark") {
          val iterator = db.newIterator()
          iterator.seekToFirst()
          keyValues foreach {
            _ =>
              iterator.key()
              iterator.value()
              iterator.next()
          }
          iterator.close()
        }
    }

  /**
   * Round 1
   * 6.887790127 seconds - 1 - RocksDB reverse iterator benchmark.
   * 4.773549085 seconds - 2 - RocksDB reverse iterator benchmark.
   * 4.426286691 seconds - 3 - RocksDB reverse iterator benchmark.
   *
   * Round 2
   * 6.492809259 seconds - 1 - RocksDB reverse iterator benchmark.
   * 4.634335185 seconds - 2 - RocksDB reverse iterator benchmark.
   * 4.864573025 seconds - 3 - RocksDB reverse iterator benchmark.
   */
  def runReverseIteratorBenchmark(): Unit =
    (1 to 3) foreach {
      count =>
        Benchmark(s"$count - RocksDB reverse iterator benchmark") {
          val iterator = db.newIterator()
          iterator.seekToLast()
          keyValues foreach {
            _ =>
              iterator.key()
              iterator.value()
              iterator.prev()
          }
          iterator.close()
        }
    }
}
