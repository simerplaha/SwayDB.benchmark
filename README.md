# SwayDB.benchmark

Benchmarks for SwayDB. Benchmark results on 1 million key-values can be viewed at [http://swaydb.io/](http://swaydb.io/#performance/macbook-pro-mid-2014)

# Run benchmarks

1. Download [swaydb-benchmark.jar](https://github.com/simerplaha/SwayDB.benchmark/blob/master/swaydb-benchmark.jar).
2. Run the jar and follow the prompts on the console.
```scala
java -jar swaydb-benchmark.jar
```

# Prompts
Select database type

```
1. Memory database
2. Persistent memory-mapped database
3. Persistent memory-mapped disabled database (FileChannel)

Select database type (hit Enter for 1):
```

Select test
```
1. Sequential write & sequential read
2. Random write & random read
3. Sequential write & random read
4. Random write & sequential read
5. Forward iteration
6. Reverse iteration

Select test number (hit Enter for 1):
```

Select test data size
```
Enter test key-value count (hit Enter for 1 million):
```

Persistent databases will create a test directory named `speedDB` in the same directory the jar is located. This 
directory gets deleted after the test is complete or if the JVM is terminated.

**Note:** Reverse iterations on ***persistent databases*** are much slower than forward iterations.

# Rebuilding the jar
`sbt clean assembly`