/*
 * Copyright 2019 Google LLC
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

package kwasm.ast

import kotlin.math.pow

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
    private val n: Int,
    offset: Int? = null,
    private val align: Int? = null
) : Argument, AstNode {
    val offset: UInt = (offset ?: 0).toUInt()
    val alignment: Int = n
    override val valueAstNode: AstNode
        get() = this

    /** Returns whether or not the value of [alignment] is valid for this the given [n]. */
    fun isAlignmentValid(forN: Int = n) = align == null || align.toDouble() == 2.0.pow(forN)

    /** Returns a canonical instance of [MemArg] matching this one, if it exists. */
    fun deDupe(): MemArg = when (this) {
        one -> one
        two -> two
        four -> four
        eight -> eight
        else -> this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MemArg

        if (n != other.n) return false
        if (offset != other.offset) return false
        if (alignment != other.alignment) return false

        return true
    }

    override fun hashCode(): Int {
        var result = n
        result = 31 * result + offset.hashCode()
        result = 31 * result + alignment.hashCode()
        return result
    }

    override fun toString(): String = "MemArg(n=$n, offset=$offset, alignment=$alignment)"

    companion object {
        private val one = MemArg(1)
        private val two = MemArg(2)
        private val four = MemArg(4)
        private val eight = MemArg(8)
    }
}
