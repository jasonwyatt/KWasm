/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kwasm.format.binary

import kwasm.format.ParseException
import java.io.InputStream
import java.io.Reader

/**
 * Tool for reading bytes from a [reader] while supporting position tracking for error reporting
 * purposes.
 */
class BinaryParser(private val reader: InputStream) {
    private var position = 0
    private val byteBuffer = ByteArray(1)

    /** Reads a single [Byte] from the [reader]. */
    fun readByte(): Byte {
        val read = reader.read(byteBuffer, 0, 1)
        if (read != 1) throw ParseException("Expected byte at position $position, but none found.")
        position += read
        return byteBuffer[0]
    }
}
