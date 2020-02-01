# SwayDB.benchmark

# RocksDB performance comparison

Performance benchmarks compared with RocksDB can be viewed at [swaydb.io/benchmarks/rocksdb](http://swaydb.io/benchmarks/rocksdb/?language=scala/).

You can execute them locally by running [RocksDB_Benchmark.scala](/src/main/scala/swaydb/benchmark/readwrite/RocksDB_Benchmark.scala)
and [SwayDB_Benchmark.scala](/src/main/scala/swaydb/benchmark/readwrite/SwayDB_Benchmark.scala). 

# Execute benchmark jar file

The following jar file was created for v0.1 of SwayDB and is targeted to benchmark SwayDB's data types (`Map` & `Set`). 

You can run benchmarks for SwayDB locally by executing the following `jar`.

1. Download [swaydb-benchmark.jar](https://github.com/simerplaha/SwayDB.benchmark/blob/master/swaydb-benchmark.jar).
2. Run the jar and follow the prompts on the console.
    ```scala
    java -jar swaydb-benchmark.jar
    ```

# Rebuilding the jar
`sbt assembly`