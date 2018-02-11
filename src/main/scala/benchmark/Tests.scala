/*
 * Copyright (C) 2018 Simer Plaha (@simerplaha)
 *
 * This file is a part of SwayDB.
 *
 * SwayDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * SwayDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with SwayDB. If not, see <https://www.gnu.org/licenses/>.
 */

package benchmark

import java.nio.file.Path

import swaydb.SwayDB
import swaydb.data.config.MMAP
import swaydb.data.slice.Slice
import swaydb.serializers.Default.SliceSerializer

case class MemoryBenchmark(keyValueCount: Long,
                           randomWrite: Boolean,
                           randomRead: Boolean,
                           forwardIteration: Boolean,
                           reverseIteration: Boolean) extends BenchmarkBase {
  override val db = SwayDB.memory[Slice[Byte], Slice[Byte]]().get
}

case class PersistentBenchmark(dir: Path,
                               mmap: Boolean,
                               keyValueCount: Long,
                               randomWrite: Boolean,
                               randomRead: Boolean,
                               forwardIteration: Boolean,
                               reverseIteration: Boolean) extends BenchmarkBase {
  override val db =
    if (mmap)
      SwayDB.persistent[Slice[Byte], Slice[Byte]](dir = dir).get
    else
      SwayDB.persistent[Slice[Byte], Slice[Byte]](dir = dir, mmapMaps = false, mmapAppendix = false, mmapSegments = MMAP.Disable).get
}