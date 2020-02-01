package swaydb.benchmark.readwrite

import java.nio.ByteBuffer
import java.nio.file.Files

import swaydb.core.util.Benchmark
import swaydb.data.slice.Slice

/**
 * Shared configurations for [[RocksDB_Benchmark]] and [[SwayDB_Benchmark]].
 */
protected object CommonSettings {

  //if true, enables memory mapped files
  val mmap: Boolean = true

  //flags to enable and disable random writes and reads.
  val randomPut: Boolean = false
  val randomGet: Boolean = false

  //total number of key-values used.
  //Default is 10 million but this can be adjust to whatever.
  val range = 1 to 10000000

  val settingsLog =
    s"""
       |Settings:
       |keyValues = ${range.size}
       |mmap = $mmap
       |randomPut = $randomPut
       |randomGet = $randomGet
       |""".stripMargin

  //information log
  println(settingsLog)

  /**
   * Key-values used in benchmarking RocksDB
   */
  val keyValues =
    Benchmark("Generating test data - keyValues") {
      range map {
        int =>
          val data = ByteBuffer.allocate(8).putLong(int).array()
          (data, data)
      }
    }

  /**
   * Set items used in SwayDB
   */
  val setItems =
    Benchmark("Generating test data - setItems") {
      keyValues map {
        case (key, value) =>
          Slice(key ++ value)
      }
    }

  /**
   * Used in by SwayDB to get [[setItems]].
   */
  val setKeys =
    Benchmark("Generating test data - setKeys") {
      keyValues map {
        case (key, _) =>
          Slice(key)
      }
    }
}
