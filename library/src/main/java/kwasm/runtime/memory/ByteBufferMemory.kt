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

package kwasm.runtime.memory

import kwasm.runtime.Memory
import kwasm.runtime.Memory.Companion.GROW_FAILURE
import kwasm.runtime.Memory.Companion.PAGE_SIZE
import kwasm.util.Impossible
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ceil

@Suppress("EXPERIMENTAL_API_USAGE", "MemberVisibilityCanBePrivate")
internal class ByteBufferMemory(
    private val maximumPages: Int = 10, // 64 Megabytes
    initialData: ByteArray = ByteArray(0),
    initialPages: Int = maxOf(1, ceil(initialData.size / PAGE_SIZE.toDouble()).toInt())
) : Memory {
    private val pages = mutableListOf<ByteBuffer>()
    private val tempBytes = ByteArray(8)
    private val tempBuffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)

    init {
        require(maximumPages > 0) {
            "Maximum size must be > 0 pages"
        }
        require(maximumPages >= initialPages * PAGE_SIZE) {
            "Maximum size specified as $maximumPages page(s), but initialPages = $initialPages"
        }
        require(maximumPages * PAGE_SIZE >= initialData.size) {
            "Maximum size specified as $maximumPages page(s) (${maximumPages * PAGE_SIZE} " +
                "bytes), but initialData has length: ${initialData.size}"
        }

        var initializedPages = 0
        while (initializedPages < initialPages) {
            pages.add(
                ByteBuffer.allocate(PAGE_SIZE)
                    .order(ByteOrder.LITTLE_ENDIAN)
            )
            initializedPages++
        }

        writeBytes(initialData, 0)
    }

    override val sizePages: Int
        get() = pages.size

    override fun readInt(offset: Int, byteWidth: Int, alignment: Int): Int {
        byteWidth.assertValidByteWidth<Int>()
        val currentPage = offset / PAGE_SIZE

        return if (byteWidth == 4 && offset % PAGE_SIZE < PAGE_SIZE - 4) {
            // Simple case... use ByteBuffer directly.
            pages[currentPage].getInt(offset % PAGE_SIZE)
        } else {
            pages.readInt(offset, byteWidth)
        }
    }

    override fun readLong(offset: Int, byteWidth: Int, alignment: Int): Long {
        byteWidth.assertValidByteWidth<Long>()
        val currentPage = offset / PAGE_SIZE

        return if (byteWidth == 8 && offset % PAGE_SIZE < PAGE_SIZE - 8) {
            // Simple case... use ByteBuffer directly.
            pages[currentPage].getLong(offset % PAGE_SIZE)
        } else {
            pages.readLong(offset, byteWidth)
        }
    }

    override fun readFloat(offset: Int, byteWidth: Int, alignment: Int): Float {
        byteWidth.assertValidByteWidth<Float>()
        val currentPage = offset / PAGE_SIZE

        return if (byteWidth == 4 && offset % PAGE_SIZE < PAGE_SIZE - 4) {
            // Simple case... use ByteBuffer directly.
            pages[currentPage].getFloat(offset % PAGE_SIZE)
        } else {
            pages.readFloat(offset, byteWidth)
        }
    }

    override fun readDouble(offset: Int, byteWidth: Int, alignment: Int): Double {
        byteWidth.assertValidByteWidth<Double>()
        val currentPage = offset / PAGE_SIZE

        return if (byteWidth == 8 && offset % PAGE_SIZE < PAGE_SIZE - 8) {
            // Simple case... use ByteBuffer directly.
            pages[currentPage].getDouble(offset % PAGE_SIZE)
        } else {
            pages.readDouble(offset, byteWidth)
        }
    }

    override fun writeInt(value: Int, offset: Int, byteWidth: Int, alignment: Int) {
        byteWidth.assertValidByteWidth<Int>()
        val currentPage = offset / PAGE_SIZE

        if (byteWidth == 4 && offset % PAGE_SIZE < PAGE_SIZE - 4) {
            // Simple case... use ByteBuffer directly.
            pages[currentPage].putInt(offset % PAGE_SIZE, value)
        } else {
            pages.writeInt(value, offset, byteWidth)
        }
    }

    override fun writeLong(value: Long, offset: Int, byteWidth: Int, alignment: Int) {
        byteWidth.assertValidByteWidth<Long>()
        val currentPage = offset / PAGE_SIZE

        if (byteWidth == 8 && offset % PAGE_SIZE < PAGE_SIZE - 8) {
            // Simple case... use ByteBuffer directly.
            pages[currentPage].putLong(offset % PAGE_SIZE, value)
        } else {
            pages.writeLong(value, offset, byteWidth)
        }
    }

    override fun writeFloat(value: Float, offset: Int, byteWidth: Int, alignment: Int) {
        byteWidth.assertValidByteWidth<Float>()
        val currentPage = offset / PAGE_SIZE

        if (byteWidth == 4 && offset % PAGE_SIZE < PAGE_SIZE - 4) {
            // Simple case... use ByteBuffer directly.
            pages[currentPage].putFloat(offset % PAGE_SIZE, value)
        } else {
            pages.writeFloat(value, offset)
        }
    }

    override fun writeDouble(value: Double, offset: Int, byteWidth: Int, alignment: Int) {
        byteWidth.assertValidByteWidth<Double>()
        val currentPage = offset / PAGE_SIZE

        if (byteWidth == 8 && offset % PAGE_SIZE < PAGE_SIZE - 8) {
            // Simple case... use ByteBuffer directly.
            pages[currentPage].putDouble(offset % PAGE_SIZE, value)
        } else {
            pages.writeDouble(value, offset)
        }
    }

    override fun readBytes(
        out: ByteArray,
        memoryOffset: Int,
        outOffset: Int,
        length: Int
    ): Int {
        require(length <= out.size - outOffset) {
            "Invalid read length ($length) for output array (size: ${out.size}) starting at " +
                "output offset: $outOffset"
        }

        val readableLength = minOf(length, sizeBytes - memoryOffset)
        pages.readBytes(out, memoryOffset, length, outOffset)
        return readableLength
    }

    override fun writeBytes(value: ByteArray, offset: Int, valueOffset: Int, valueLength: Int) {
        require(valueOffset + valueLength < value.size) {
            "Illegal offset/length for value with size: ${value.size}"
        }
        require(offset + valueLength < sizeBytes) {
            "Value with size ${value.size} bytes cannot fit into memory with size: $sizeBytes " +
                "bytes ($sizePages page(s))"
        }
        var valuePosition = valueOffset
        var currentPage = offset / PAGE_SIZE
        var currentPositionInPage = offset % PAGE_SIZE
        while (valuePosition < valueOffset + valueLength) {
            val leftInPage = PAGE_SIZE - currentPositionInPage
            val leftInValue = (valueLength + valueOffset) - valuePosition
            valuePosition += if (leftInPage >= leftInValue) {
                // We're at the last page, so write all the remaining bytes from the value.
                pages[currentPage]
                    .position(currentPositionInPage)
                    .put(value, valuePosition, leftInValue)
                    .rewind()
                leftInValue
            } else {
                // We're still working through the pages, so write as much as we can in this page.
                pages[currentPage]
                    .position(currentPositionInPage)
                    .put(value, valuePosition, leftInPage)
                    .rewind()
                leftInPage
            }
            currentPage++
            currentPositionInPage = 0
        }
    }

    override fun growBy(newPages: Int): Int {
        if (sizePages + newPages > maximumPages) return GROW_FAILURE
        val sizeBefore = sizePages
        repeat(newPages) {
            pages.add(
                ByteBuffer.allocate(PAGE_SIZE)
                    .order(ByteOrder.LITTLE_ENDIAN)
            )
        }
        return sizeBefore
    }

    internal fun List<ByteBuffer>.readInt(offset: Int, byteWidth: Int): Int {
        val result = this.readBytes(tempBytes, offset, byteWidth).toBigEndianInt(byteWidth)
        return when(byteWidth) {
            1 -> result.toByte().toInt()
            2 -> result.toShort().toInt()
            4 -> result
            else -> Impossible()
        }
    }

    internal fun List<ByteBuffer>.writeInt(value: Int, offset: Int, byteWidth: Int) {
        val wrappedValue = value.wrap(byteWidth)
        var currentPage = offset / PAGE_SIZE
        var currentPositionInPage = offset % PAGE_SIZE
        repeat(byteWidth) {
            val byte = (wrappedValue ushr (8 * it)) and 0xFF
            this[currentPage].put(currentPositionInPage, byte.toByte())
            currentPositionInPage++
            if (currentPositionInPage == PAGE_SIZE) {
                currentPositionInPage = 0
                currentPage++
            }
        }
    }

    internal fun List<ByteBuffer>.readLong(offset: Int, byteWidth: Int): Long {
        val result = this.readBytes(tempBytes, offset, byteWidth).toBigEndianLong(byteWidth)
        return when(byteWidth) {
            1 -> result.toByte().toLong()
            2 -> result.toShort().toLong()
            4 -> result.toInt().toLong()
            8 -> result
            else -> Impossible()
        }
    }

    internal fun List<ByteBuffer>.writeLong(value: Long, offset: Int, byteWidth: Int) {
        val wrappedValue = value.wrap(byteWidth)
        var currentPage = offset / PAGE_SIZE
        var currentPositionInPage = offset % PAGE_SIZE
        repeat(byteWidth) {
            val byte = (wrappedValue ushr (8 * it)) and 0xFF
            this[currentPage].put(currentPositionInPage, byte.toByte())
            currentPositionInPage++
            if (currentPositionInPage == PAGE_SIZE) {
                currentPositionInPage = 0
                currentPage++
            }
        }
    }

    internal fun List<ByteBuffer>.readFloat(offset: Int, byteWidth: Int): Float {
        val result = this.readBytes(tempBytes, offset, byteWidth).toBigEndianInt(byteWidth)
        return when(byteWidth) {
            1 -> result.toByte().toFloat()
            2 -> result.toShort().toFloat()
            4 -> result.toFloat()
            else -> Impossible()
        }
    }

    internal fun List<ByteBuffer>.writeFloat(value: Float, offset: Int) {
        tempBuffer.putFloat(0, value)
        var currentPage = offset / PAGE_SIZE
        var currentPositionInPage = offset % PAGE_SIZE
        repeat(4) {
            val byte = tempBuffer.get(it)
            this[currentPage].put(currentPositionInPage, byte)
            currentPositionInPage++
            if (currentPositionInPage == PAGE_SIZE) {
                currentPositionInPage = 0
                currentPage++
            }
        }
    }

    internal fun List<ByteBuffer>.readDouble(offset: Int, byteWidth: Int): Double {
        val result = this.readBytes(tempBytes, offset, byteWidth).toBigEndianLong(byteWidth)
        return when(byteWidth) {
            1 -> result.toByte().toDouble()
            2 -> result.toShort().toDouble()
            4 -> result.toInt().toDouble()
            8 -> result.toDouble()
            else -> Impossible()
        }
    }

    internal fun List<ByteBuffer>.writeDouble(value: Double, offset: Int) {
        tempBuffer.putDouble(0, value)
        var currentPage = offset / PAGE_SIZE
        var currentPositionInPage = offset % PAGE_SIZE
        repeat(8) {
            val byte = tempBuffer.get(it)
            this[currentPage].put(currentPositionInPage, byte)
            currentPositionInPage++
            if (currentPositionInPage == PAGE_SIZE) {
                currentPositionInPage = 0
                currentPage++
            }
        }
    }

    private fun List<ByteBuffer>.readBytes(
        out: ByteArray,
        offset: Int,
        length: Int,
        outOffset: Int = 0
    ): ByteArray {
        var currentPage = offset / PAGE_SIZE
        var currentPageOffset = offset % PAGE_SIZE
        var currentOutOffset = outOffset
        var readBytes = 0
        while (readBytes < length) {
            val leftInPage = PAGE_SIZE - currentPageOffset
            val leftInLength = length - readBytes
            readBytes += if (leftInPage >= leftInLength) {
                // Last page needed for reading. Read the remaining length.
                this[currentPage]
                    .position(currentPageOffset)
                    .get(out, currentOutOffset, leftInLength)
                    .rewind()
                leftInLength
            } else {
                this[currentPage]
                    .position(currentPageOffset)
                    .get(out, currentOutOffset, leftInPage)
                    .rewind()
                leftInPage
            }
            currentOutOffset = outOffset + readBytes
            currentPage++
            currentPageOffset = 0
        }
        return out
    }

    private fun ByteArray.toBigEndianInt(length: Int): Int {
        var result = 0
        repeat(length) { result += this[it].toInt() shl 8 }
        return result
    }

    private fun ByteArray.toBigEndianLong(length: Int): Long {
        var result = 0L
        repeat(length) { result += this[it].toInt() shl 8 }
        return result
    }

    private fun Int.wrap(bytes: Int) = when (bytes) {
        1 -> this % BYTE_MAX_VALUE.toInt()
        2 -> this % SHORT_MAX_VALUE.toInt()
        4 -> this
        else -> Impossible()
    }

    private fun Long.wrap(bytes: Int) = when (bytes) {
        1 -> this % BYTE_MAX_VALUE
        2 -> this % SHORT_MAX_VALUE
        4 -> this % INT_MAX_VALUE
        8 -> this
        else -> Impossible()
    }

    companion object {
        private const val BYTE_MAX_VALUE = 256L
        private const val SHORT_MAX_VALUE = 65536L
        private const val INT_MAX_VALUE = 4294967296L
    }
}
