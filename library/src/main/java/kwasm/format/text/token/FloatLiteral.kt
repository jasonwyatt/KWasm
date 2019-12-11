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
import kwasm.format.text.token.util.CanonincalNaN
import kwasm.format.text.token.util.Frac
import kwasm.format.text.token.util.Num
import kwasm.format.text.token.util.NumberConstants
import kwasm.format.text.token.util.parseLongSign
import kwasm.format.text.token.util.significand
import kotlin.math.pow

/**
 * From [the docs](https://webassembly.github.io/spec/core/text/values.html#floating-point).
 *
 * ```
 *   float    ::= p:num                                                 => p
 *                p:num '.' q:frac                                      => p + q
 *                p:num ('E' | 'e') s:sign e:num                        => p * 10^(s * e)
 *                p:num '.' q:frac ('E' | 'e') s:sign e:num             => (p + q) * 10^(s * e)
 *   hexfloat ::= '0x' p:hexnum                                         => p
 *                '0x' p:hexnum '.' q:hexfrac                           => p + q
 *                '0x' p:hexnum  ('P' | 'p') s:sign e:num               => p * 2^(s * e)
 *                '0x' p:hexnum '.' q:hexfrac ('P' | 'p') s:sign e:num  => (p + q) * 2^(s * e)
 * ```
 *
 * And:
 *
 * ```
 *   fN       ::= s:sign z:fNMag                                      => s * z
 *   fNMag    ::= z:float                                             => float_N(z) (if != inf)
 *                z:hexfloat                                          => float_N(z) (if != inf)
 *                'inf'                                               => inf
 *                'nan'                                               => NaN
 *                'nan:0x' n:hexnum                                   => NaN (if 1 <= n < 2^(sig(N))
 * ```
 */
@UseExperimental(ExperimentalUnsignedTypes::class)
class FloatLiteral(
    private val sequence: CharSequence,
    magnitude: Int = NumberConstants.DEFAULT_FLOAT_MAGNITUDE,
    private val context: ParseContext? = null
) {
    var magnitude: Int = magnitude
        set(value) {
            check(value == 32 || value == 64) { "Magnitude must be either 32 or 64" }
            field = value
        }

    val value: Double by lazy {
        when {
            sequence == INFINITY_LITERAL -> INFINITY_VALUE
            sequence == NAN_LITERAL -> if (magnitude == 32) NAN_32_VALUE else NAN_64_VALUE
            sequence.startsWith(HEXNAN_LITERAL_PREFIX) -> {
                val n = Num(
                    sequence.subSequence(
                        HEXNAN_LITERAL_PREFIX.length,
                        sequence.length
                    ),
                    context
                )
                n.forceHex = true
                val nValue = n.value

                if (
                    nValue.toLong() != 0L &&
                    nValue.toLong() >= 2.0.pow(magnitude.significand(context))
                ) throw ParseException("Illegal hex NaN value.")

                CanonincalNaN(magnitude).value.toDouble()
            }
            else -> {
                val (sequenceOffset, sign) = sequence.parseLongSign()
                val floatValue = determineFloatValue(
                    sequence.subSequence(sequenceOffset, sequence.length),
                    context.shiftColumnBy(sequenceOffset)
                )
                sign * floatValue
            }
        }
    }

    fun isNaN(): Boolean = when (magnitude) {
        32 -> value == NAN_32_VALUE
        64 -> value == NAN_64_VALUE
        else -> throw ParseException("Illegal magnitude for NaN-checking")
    }

    fun isInfinite(): Boolean = value == INFINITY_VALUE

    private fun determineFloatValue(
        sequence: CharSequence,
        context: ParseContext? = this.context
    ): Double = if (sequence.startsWith("0x")) {
        determineHexFloatValue(
            sequence.subSequence(2, sequence.length),
            context.shiftColumnBy(2)
        )
    } else {
        determineDecimalFloatValue(sequence, context)
    }

    /**
     * ```
     *   float    ::= p:num                                                 => p
     *                p:num '.' q:frac                                      => p + q
     *                p:num ('E' | 'e') s:sign e:num                        => p * 10^(s * e)
     *                p:num '.' q:frac ('E' | 'e') s:sign e:num             => (p + q) * 10^(s * e)
     * ```
     */
    private fun determineDecimalFloatValue(
        sequence: CharSequence,
        context: ParseContext?
    ): Double {
        val dotIndex = sequence.indexOf('.')
        val eIndex = sequence.indexOf('e', ignoreCase = true)

        if (dotIndex == 0) throw ParseException("Invalid placement for decimal in float", context)
        if (eIndex == sequence.length - 1) {
            throw ParseException("Invalid exponent for float (no contents)", context)
        }

        return if (dotIndex == -1 && eIndex == -1) {
            // Case 1.
            val p = case1(sequence, context)
            assertNoHexChars(
                context,
                p
            )
            p.value.toDouble()
        } else if (dotIndex != -1 && eIndex == -1) {
            val (p, q) = case2(sequence, dotIndex, context)
            assertNoHexChars(
                context,
                p,
                q
            )
            p.value.toDouble() + q.value
        } else if (dotIndex == -1 && eIndex != -1) {
            val (p, exponent) = case3(sequence, eIndex, context)
            assertNoHexChars(
                context,
                p
            )
            p.value.toDouble() * 10.0.pow(exponent.value.toInt())
        } else {
            val (p, q, exponent) = case4(sequence, dotIndex, eIndex, context)
            assertNoHexChars(
                context,
                p,
                q
            )
            (p.value.toDouble() + q.value) * 10.0.pow(exponent.value.toInt())
        }
    }

    /**
     * ```
     *   hexfloat ::= '0x' p:hexnum                                         => p
     *                '0x' p:hexnum '.' q:hexfrac                           => p + q
     *                '0x' p:hexnum ('P' | 'p') s:sign e:num                => p * 2^(s * e)
     *                '0x' p:hexnum '.' q:hexfrac ('P' | 'p') s:sign e:num  => (p + q) * 2^(s * e)
     * ```
     *
     * **Note:** The '0x' prefix has already been stripped.
     */
    private fun determineHexFloatValue(
        sequence: CharSequence,
        context: ParseContext?
    ): Double {
        val dotIndex = sequence.indexOf('.')
        val eIndex = sequence.indexOf('p', ignoreCase = true)

        if (dotIndex == 0) throw ParseException("Invalid placement for decimal in float", context)
        if (eIndex == sequence.length - 1) {
            throw ParseException("Invalid exponent for float (no contents)", context)
        }
        return if (dotIndex == -1 && eIndex == -1) {
            // Case 1.
            val p = case1(sequence, context)
            p.forceHex = true
            p.value.toDouble()
        } else if (dotIndex != -1 && eIndex == -1) {
            val (p, q) = case2(sequence, dotIndex, context)
            p.forceHex = true
            q.forceHex = true
            p.value.toDouble() + q.value
        } else if (dotIndex == -1 && eIndex != -1) {
            val (p, exponent) = case3(sequence, eIndex, context)
            p.forceHex = true
            p.value.toDouble() * 2.0.pow(exponent.value.toInt())
        } else {
            val (p, q, exponent) = case4(sequence, dotIndex, eIndex, context)
            p.forceHex = true
            q.forceHex = true
            (p.value.toDouble() + q.value) * 2.0.pow(exponent.value.toInt())
        }
    }

    private fun case1(sequence: CharSequence, context: ParseContext?): Num =
        Num(sequence, context)

    private fun case2(
        sequence: CharSequence,
        dotIndex: Int,
        context: ParseContext?
    ): Pair<Num, Frac> {
        val p = Num(
            sequence.subSequence(0, dotIndex),
            context
        )
        val q = Frac(
            sequence.subSequence(dotIndex + 1, sequence.length),
            context.shiftColumnBy(dotIndex + 1)
        )
        return p to q
    }

    private fun case3(
        sequence: CharSequence,
        eIndex: Int,
        context: ParseContext?
    ): Pair<Num, IntegerLiteral.Signed> {
        val p = Num(
            sequence.subSequence(0, eIndex),
            context
        )
        val exponent = IntegerLiteral.Signed(
            sequence.subSequence(eIndex + 1, sequence.length),
            context = context.shiftColumnBy(eIndex + 1)
        )
        return p to exponent
    }

    private fun case4(
        sequence: CharSequence,
        dotIndex: Int,
        eIndex: Int,
        context: ParseContext?
    ): Triple<Num, Frac, IntegerLiteral.Signed> {
        val p = Num(
            sequence.subSequence(0, dotIndex),
            context
        )
        val frac = Frac(
            sequence.subSequence(dotIndex + 1, eIndex),
            context.shiftColumnBy(dotIndex + 1)
        )
        val exponent = IntegerLiteral.Signed(
            sequence.subSequence(eIndex + 1, sequence.length),
            context = context.shiftColumnBy(eIndex + 1)
        )
        return Triple(p, frac, exponent)
    }

    companion object {
        private val FLOAT_PATTERN = object : ThreadLocal<Regex>() {
            override fun initialValue(): Regex =
                "(0x)?(${Num.PATTERN.get()})(\\.${Frac.PATTERN.get()})?([EePp][+-]?${Num.PATTERN.get()})?".toRegex()
        }

        val PATTERN = object : ThreadLocal<Regex>() {
            override fun initialValue(): Regex =
                "[+-]?((${FLOAT_PATTERN.get()})|(inf|nan(:0x${Num.PATTERN.get()})?))".toRegex()
        }

        private const val INFINITY_LITERAL = "inf"
        private const val NAN_LITERAL = "nan"
        private const val HEXNAN_LITERAL_PREFIX = "nan:0x"
        private val INFINITY_VALUE = Double.POSITIVE_INFINITY
        private val NAN_64_VALUE = CanonincalNaN(64).value.toDouble()
        private val NAN_32_VALUE = CanonincalNaN(32).value.toDouble()

        private fun assertNoHexChars(context: ParseContext?, vararg components: Any) {
            val foundHexChars = components.any {
                (it as? Num)?.foundHexChars == true || (it as? Frac)?.foundHexChars == true
            }
            if (foundHexChars) {
                throw ParseException("Found illegal hex digits in expected base-10 float", context)
            }
        }
    }
}
