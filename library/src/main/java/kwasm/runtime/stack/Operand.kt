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

package kwasm.runtime.stack

import kwasm.runtime.StackElement

/**
 * Defines a value for use in WebAssembly at runtime.
 *
 * From [the docs](https://webassembly.github.io/spec/core/exec/runtime.html#values):
 *
 * WebAssembly computations manipulate values of the four basic value types: integers and
 * floating-point data of 32 or 64 bit width each, respectively.
 *
 * In most places of the semantics, values of different types can occur. In order to avoid
 * ambiguities, values are therefore represented with an abstract syntax that makes their type
 * explicit. It is convenient to reuse the same notation as for the `const` instructions producing
 * them:
 *
 * ```
 *   val    ::= i32.const i32
 *              i64.const i64
 *              f32.const f32
 *              f64.const f64
 * ```
 */
interface Value<T : Number> : StackElement {
    val value: T
}

/** Holds a 32-bit integer [Value]. */
inline class IntValue(override val value: Int) : Value<Int> {
    val unsignedValue: UInt
        get() = value.toUInt()
}

/** Holds a 64-bit integer [Value]. */
inline class LongValue(override val value: Long) : Value<Long> {
    val unsignedValue: ULong
        get() = value.toULong()
}

/** Holds a 32-bit floating-point [Value]. */
inline class FloatValue(override val value: Float) : Value<Float>

/** Holds a 64-bit floating-point [Value]. */
inline class DoubleValue(override val value: Double) : Value<Double>

/** Wraps an [Int] in an [IntValue]. */
fun Int.toValue(): IntValue = IntValue(this)

/** Wraps a [UInt] in an [IntValue]. */
fun UInt.toValue(): IntValue = IntValue(this.toInt())

/** Wraps a [Long] in a [LongValue]. */
fun Long.toValue(): LongValue = LongValue(this)

/** Wraps a [ULong] in a [LongValue]. */
fun ULong.toValue(): LongValue = LongValue(this.toLong())

/** Wraps a [Float] in a [FloatValue]. */
fun Float.toValue(): FloatValue = FloatValue(this)

/** Wraps a [Double] in a [DoubleValue]. */
fun Double.toValue(): DoubleValue = DoubleValue(this)
