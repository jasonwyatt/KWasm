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

@file:Suppress("EXPERIMENTAL_API_USAGE")

package kwasm.format.binary

import kwasm.format.ParseException
import java.io.InputStream

/**
 * Tool for reading bytes from a [reader] while supporting position tracking for error reporting
 * purposes.
 */
class BinaryParser(private val reader: InputStream) {
    private var position = 0
    private val byteBuffer = ByteArray(1)
    private val intBuffer = ByteArray(4)
    private val longBuffer = ByteArray(8)

    /** Reads a single [Byte] from the [reader]. */
    fun readByte(): Byte {
        val read = reader.read(byteBuffer, 0, 1)
        if (read != 1) throw ParseException("Expected byte at position $position, but none found.")
        position += read
        return byteBuffer[0]
    }

    /**
     * Reads the next four bytes from the [reader] as a little-endian-encoded [Int].
     */
    fun readFourBytes(): Int {
        val read = reader.read(intBuffer, 0, 4)
        if (read != 4) {
            throw ParseException("Expected 4 bytes at position $position, but $read found.")
        }
        position += read
        return intBuffer[0].toUByte().toInt() or
            (intBuffer[1].toUByte().toInt() shl 8) or
            (intBuffer[2].toUByte().toInt() shl 16) or
            (intBuffer[3].toUByte().toInt() shl 24)
    }

    /**
     * Reads the next eight bytes from the [reader] as a little-endian-encoded [Long].
     */
    fun readEightBytes(): Long {
        val read = reader.read(longBuffer, 0, 8)
        if (read != 8) {
            throw ParseException("Expected 8 bytes at position $position, but $read found.")
        }
        position += read
        return longBuffer[0].toUByte().toLong() or
            (longBuffer[1].toUByte().toLong() shl 8) or
            (longBuffer[2].toUByte().toLong() shl 16) or
            (longBuffer[3].toUByte().toLong() shl 24) or
            (longBuffer[4].toUByte().toLong() shl 32) or
            (longBuffer[5].toUByte().toLong() shl 40) or
            (longBuffer[6].toUByte().toLong() shl 48) or
            (longBuffer[7].toUByte().toLong() shl 56)
    }
}