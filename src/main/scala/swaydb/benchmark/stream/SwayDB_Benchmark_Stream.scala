package swaydb.benchmark.stream

import swaydb.Bag
import swaydb.core.util.Benchmark

object SwayDB_Benchmark_Stream extends App {

  implicit val bag = Bag.less

  /**
   * SwayDB v0.12-RC3
   * 3.897010694 seconds - swaydb.Stream.
   * 4.161759733 seconds - swaydb.Stream.
   *
   * SwayDB v0.12.3
   * 7.110504304 seconds - swaydb.Stream.
   * 6.683342865 seconds - swaydb.Stream.
   * 6.922609006 seconds - swaydb.Stream.
   * 6.665925745 seconds - swaydb.Stream.
   * 6.592949532 seconds - swaydb.Stream.
   */
  Benchmark("swaydb.Stream") {
    swaydb
      .Stream((1 to 100000000).iterator)
      .map(_ + 1)
      .takeWhile(_ => true)
      .collect { case int => int }
      .foldLeft(0)(_ + _)
  }

  /**
   * 15.58047123 seconds - scala.LazyList.
   * 14.973142375 seconds - scala.LazyList.
   * 15.362722525 seconds - scala.LazyList.
   * 15.561274273 seconds - scala.LazyList.
   * 15.334505448 seconds - scala.LazyList.
   */
  Benchmark("scala.LazyList") {
    scala
      .LazyList
      .from((1 to 100000000).iterator)
      .map(_ + 1)
      .takeWhile(_ => true)
      .collect { case int => int }
      .foldLeft(0)(_ + _)
  }
}
