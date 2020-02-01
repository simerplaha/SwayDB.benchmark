package swaydb.benchmark.storage

import java.nio.ByteBuffer

import swaydb.data.slice.Slice
import swaydb.data.util.StorageUnits._

/**
 * Configurations shared between databases being
 * benchmarked in the [[swaydb.benchmark.readwrite]] package.
 */
protected object CommonSettings {

  val mmap: Boolean = false
  val level0LogSize: Int = 60.mb
  val segmentSize: Int = 40.mb
  val enableHashIndex: Boolean = false

  val range = 1 to 1000000

  def keyValues =
    range map {
      int =>
        val key = ByteBuffer.allocate(4).putInt(int).array()
        (key, int.toString.getBytes())
    }

  def keyValuesSlice =
    range map {
      int =>
        val key = ByteBuffer.allocate(4).putInt(int).array()
        (Slice(key), Slice(int.toString.getBytes()))
    }
}
