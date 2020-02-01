package swaydb.benchmark.storage

import java.nio.file.Paths

import org.rocksdb.{RocksDB, _}
import swaydb.benchmark.util.FileUtils
import swaydb.core.util.Benchmark

object RocksDB_Benchmark_Space extends App {

  /**
   * Following the documentation https://github.com/facebook/rocksdb/wiki/RocksJava-Basics#opening-a-database
   */
  val dir = Paths.get("target").resolve("dbs").resolve(this.getClass.getSimpleName.takeWhile(_ != '$'))

  RocksDB.loadLibrary()

  val options: Options = new Options().setCreateIfMissing(true)

  options.setCreateIfMissing(true)
    .setMaxLogFileSize(CommonSettings.level0LogSize)
    .setMaxTotalWalSize(CommonSettings.level0LogSize)
    .setCompressionType(CompressionType.LZ4_COMPRESSION)
    .setCompactionStyle(CompactionStyle.LEVEL)
    //mmap does not seem to improve write performance in RocksDB.
    //Reading https://github.com/facebook/rocksdb/wiki/IO#memory-mapping maybe mmap is only applied to sst files?
    .setAllowMmapWrites(CommonSettings.mmap)

  val tableConfig = new BlockBasedTableConfig()
  tableConfig.setDataBlockIndexType(DataBlockIndexType.kDataBlockBinaryAndHash)

  options.setTableFormatConfig(tableConfig)

  var db: RocksDB = _

  try {
    db = RocksDB.open(options, dir.toString)

    val keyValues = CommonSettings.keyValues

    /**
     * When keyValues = 1000000
     * 19.84 mb - 19844268 bytes - WITHOUT compression
     * 11.71 mb - 11709788 bytes - WITH LZ4 compression
     */
    Benchmark(s"RocksDB space benchmark. keyValues = ${keyValues.size}, mmapWrites = ${CommonSettings.mmap}, enableHashIndex = ${CommonSettings.enableHashIndex}") {
      keyValues foreach {
        case (key, value) =>
          db.put(key, value)
      }
    }

    FileUtils.printFilesSize(dir, "sst")

  } finally {
    db.closeE()
  }
}
