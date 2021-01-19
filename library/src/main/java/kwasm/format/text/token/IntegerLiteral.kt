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

package kwasm.format.text.token

import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.shiftColumnBy
import kwasm.format.text.token.util.Num
import kwasm.format.text.token.util.TokenMatchResult
import kwasm.format.text.token.util.parseLongSign
import kotlin.math.pow

/**
 * From [the docs](https://webassembly.github.io/spec/core/text/values.html#integers):
 *
 * The allowed syntax for integer literals depends on size and signedness. Moreover, their value
 * must lie within the range of the respective type
 *
 * ```
 *   sign       ::= empty => +
 *                  '+' => +
 *                  '-' => -
 *   uN         ::= n:num         => n (if n < 2^N)
 *                  '0x' n:hexnum => n (if n < 2^N)
 *   sN         ::= plusminus:sign n:num => plusminus * n (if -2^(N-1) <= plusminus n < 2^(N-1))
 *                  plusminus:sign n:hexnum => plusminus * n (same conditions as above)
 * ```.
 *
 * Uninterpreted integers can be written as either signed or unsigned, and are normalized to
 * unsigned in the abstract syntax.
 *
 * ```
 *   iN         ::= n:uN => n
 *                  i:sN => n (if i = signed(n))
 * ```
 */
@OptIn(ExperimentalUnsignedTypes::class)
sealed class IntegerLiteral<Type>(
    protected val sequence: CharSequence,
    magnitude: Int = 64,
    override val context: ParseContext? = null
) : Token {
    val value: Type by lazy {
        val res = parseValue()
        if (!checkMagnitude(res, this.magnitude)) {
            throw ParseException(
                "Illegal value: $res, for expected magnitude: " +
                    "${this.magnitude}: i${this.magnitude} constant",
                context
            )
        }
        res
    }
    var magnitude: Int = magnitude
        set(value) {
            check(value >= 0) { "Negative magnitudes are not allowed" }
            check(value <= 64) { "Magnitudes above 64 are not allowed" }
            field = value
        }

    protected abstract fun parseValue(): Type
    protected abstract fun checkMagnitude(value: Type, magnitude: Int): Boolean
    abstract fun toUnsigned(): Unsigned
    abstract fun toSigned(): Signed

    class Unsigned(
        sequence: CharSequence,
        magnitude: Int = 64,
        context: ParseContext? = null
    ) : IntegerLiteral<ULong>(sequence, magnitude, context) {
        override fun parseValue(): ULong {
            val expectHex: Boolean
            val num: Num
            if (sequence.length > 2 && sequence[0] == '0' && sequence[1] == 'x') {
                expectHex = true
                num = Num(
                    sequence.subSequence(2, sequence.length),
                    context.shiftColumnBy(2)
                )
            } else {
                expectHex = false
                num = Num(sequence, context)
            }

            if (!expectHex && num.foundHexChars) {
                throw ParseException("Unexpected hex integer", context)
            }
            num.forceHex = expectHex
            return num.value
        }

        override fun checkMagnitude(value: ULong, magnitude: Int): Boolean = when (magnitude) {
            32 -> value <= UInt.MAX_VALUE
            64 -> value <= ULong.MAX_VALUE
            else -> value.toDouble() < 2.0.pow(magnitude)
        }

        override fun toUnsigned(): Unsigned = this

        override fun toSigned(): Signed = Signed(sequence, magnitude, context)
    }

    class Signed(
        sequence: CharSequence,
        magnitude: Int = 64,
        context: ParseContext? = null
    ) : IntegerLiteral<Long>(sequence, magnitude, context) {
        override fun parseValue(): Long {
            val (sequenceOffset, sign) = sequence.parseLongSign()
            val expectHex: Boolean
            val num: Num
            if (
                sequence.length > 2 + sequenceOffset &&
                sequence[sequenceOffset] == '0' &&
                sequence[sequenceOffset + 1] == 'x'
            ) {
                expectHex = true
                num = Num(
                    sequence.subSequence(sequenceOffset + 2, sequence.length),
                    context.shiftColumnBy(2 + sequenceOffset)
                )
            } else {
                expectHex = false
                num = Num(
                    sequence.subSequence(sequenceOffset, sequence.length),
                    context.shiftColumnBy(sequenceOffset)
                )
            }

            if (!expectHex && num.foundHexChars) {
                throw ParseException("Unexpected hex integer", context)
            }
            num.forceHex = expectHex
            return sign * num.value.toLong()
        }

        override fun checkMagnitude(value: Long, magnitude: Int): Boolean {
            if (magnitude == 32) return value.toUInt().toInt() in Int.MIN_VALUE..Int.MAX_VALUE
            if (magnitude == 64) return value in Long.MIN_VALUE..Long.MAX_VALUE
            val doubleValue = value.toDouble()
            val extent = 2.0.pow(magnitude - 1)
            return -extent <= doubleValue && doubleValue < extent
        }

        override fun toUnsigned(): Unsigned = Unsigned(sequence, magnitude, context)

        override fun toSigned(): Signed = this
    }

    companion object {
        private const val DECIMAL_PATTERN = "(${Num.DECIMAL_PATTERN})"
        private const val HEX_PATTERN = "(0x(${Num.HEX_PATTERN}))"

        internal val PATTERN = object : ThreadLocal<Regex>() {
            override fun initialValue(): Regex = "([-+]?($HEX_PATTERN|$DECIMAL_PATTERN))".toRegex()
        }
    }
}

fun RawToken.findIntegerLiteral(): TokenMatchResult? {
    val match = IntegerLiteral.PATTERN.get().findAll(sequence)
        .maxByOrNull { it.value.length } ?: return null
    return TokenMatchResult(match.range.first, match.value)
}

fun RawToken.isIntegerLiteral(): Boolean =
    IntegerLiteral.PATTERN.get().matchEntire(sequence) != null

fun RawToken.toIntegerLiteral(): IntegerLiteral<*> =
    if ('-' in sequence || '+' in sequence) {
        IntegerLiteral.Signed(sequence, context = context)
    } else {
        IntegerLiteral.Unsigned(sequence, context = context)
    }
