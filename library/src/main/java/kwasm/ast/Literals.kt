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

/** Base for all literal values in the AST. */
interface Literal<T> : AstNode {
    val value: T
}

/** Base for all [Literal] classes which represent an integer or long value. */
@UseExperimental(ExperimentalUnsignedTypes::class)
sealed class IntegerLiteral<T> : Literal<T> {
    /** [Literal] Representation of an unsigned [Int] (as [UInt]). */
    data class U32(override val value: UInt) : IntegerLiteral<UInt>()

    /** [Literal] Representation of a signed [Int]. */
    data class S32(override val value: Int) : IntegerLiteral<Int>()

    /** [Literal] Representation of an unsigned [Long] (as [ULong]). */
    data class U64(override val value: ULong) : IntegerLiteral<ULong>()

    /** [Literal] Representation of a signed [Long]. */
    data class S64(override val value: Long) : IntegerLiteral<Long>()
}

/** Base for all [Literal] classes which represent a floating-point value. */
sealed class FloatLiteral<T> : Literal<T> {
    /** [Literal] Representation of a [Float]. */
    data class SinglePrecision(override val value: Float) : FloatLiteral<Float>()

    /** [Literal] Representation of a [Double]. */
    data class DoublePrecision(override val value: Double) : FloatLiteral<Double>()
}

/** Representation of a [String] as a [Literal]. */
data class StringLiteral(override val value: String) : Literal<String>
