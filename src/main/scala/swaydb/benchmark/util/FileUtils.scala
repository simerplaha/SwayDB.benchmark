package swaydb.benchmark.util

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files, Path}

import scala.jdk.CollectionConverters._

object FileUtils {

  def printFilesSize(folder: Path, fileExtension: String) = {

    def extensionFilter(path: Path, attributes: BasicFileAttributes) = {
      val fileName = path.getFileName.toString
      val isSST = fileName.contains(fileExtension)
      //if (isSST) println(fileName)
      isSST
    }

    val size =
      Files
        .find(folder, Int.MaxValue, extensionFilter)
        .iterator()
        .asScala
        .foldLeft(0L)(_ + _.toFile.length())

    //    val gb = BigDecimal(size / 1000000000.0).setScale(2, BigDecimal.RoundingMode.HALF_UP)
    val mb = BigDecimal(size / 1000000.0).setScale(2, BigDecimal.RoundingMode.HALF_UP)

    //    println(s"${fileExtension.toUpperCase} files size: $gb gb - $mb mb - $size bytes")
    println(s"${fileExtension.toUpperCase} files size: $mb mb - $size bytes\n")
  }
}
