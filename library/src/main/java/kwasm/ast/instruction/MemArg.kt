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

package kwasm.ast.instruction

import kotlin.math.floor
import kotlin.math.log
import kwasm.ast.AstNode
import kwasm.ast.DeDupeableAstNode

/**
 * Represents an [Argument] to a memory [Instruction].
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/instructions.html#text-memarg):
 *
 * The [offset] and [alignment] immediates to memory instructions are optional. The [offset]
 * defaults to `0` and the [alignment] to the storage size of the respective memory access, which is
 * its natural alignment.
 */
@Suppress("DataClassPrivateConstructor")
@UseExperimental(ExperimentalUnsignedTypes::class)
class MemArg(
    offset: Int? = null,
    private val alignment: Int
) : Argument,
    DeDupeableAstNode<MemArg> {
    val offset: UInt = (offset ?: 0).toUInt()
    override val valueAstNode: AstNode
        get() = this

    /**
     * Returns whether or not the value of [alignment] is valid for the given [forByteWidth]
     * upper-bound.
     */
    fun isAlignmentValid(forByteWidth: Int? = null) =
        log(alignment.toFloat(), 2.0f)
            // If it's a power of 2, and it's less than or equal to 2^forN, then it's valid.
            .let { floor(it) == it && (forByteWidth == null || alignment <= forByteWidth * 8) }

    /** Returns a canonical instance of [MemArg] matching this one, if it exists. */
    override fun deDupe(): MemArg = when (this) {
        ONE -> ONE
        TWO -> TWO
        FOUR -> FOUR
        EIGHT -> EIGHT
        else -> this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MemArg

        if (offset != other.offset) return false
        if (alignment != other.alignment) return false

        return true
    }

    override fun hashCode(): Int {
        var result = offset.hashCode()
        result = 31 * result + alignment.hashCode()
        return result
    }

    override fun toString(): String = "MemArg(offset=$offset, alignment=$alignment)"

    companion object {
        internal val ONE = MemArg(0, 8)
        internal val TWO = MemArg(0, 16)
        internal val FOUR = MemArg(0, 32)
        internal val EIGHT = MemArg(0, 64)
    }
}
