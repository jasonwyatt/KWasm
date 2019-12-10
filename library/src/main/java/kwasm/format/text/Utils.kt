package kwasm.format.text

import kwasm.format.ParseContext
import kwasm.format.ParseException
import kotlin.math.pow

fun CharSequence.parseLongSign(): Pair<Int, Long> = when(this[0]) {
    '-' -> NumberConstants.negativeLongWithOffset
    '+' -> NumberConstants.positiveLongWithOffset
    else -> NumberConstants.positiveLong
}

fun CharSequence.parseDoubleSign(): Pair<Int, Double> = when(this[0]) {
    '-' -> NumberConstants.negativeDoubleWithOffset
    '+' -> NumberConstants.positiveDoubleWithOffset
    else -> NumberConstants.positiveDouble
}

fun CharSequence.parseDigit(index: Int, context: ParseContext? = null): Byte =
    when (val c = this[index]) {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> (c.toInt() - 48).toByte()
        'a', 'b', 'c', 'd', 'e', 'f' -> (c.toInt() - 97 + 10).toByte()
        'A', 'B', 'C', 'D', 'E', 'F' -> (c.toInt() - 65 + 10).toByte()
        '_' -> NumberConstants.UNDERSCORE
        else -> throw ParseException(
            "Illegal char '$c' in expected number.",
            context?.copy(column = context.column + index)
        )
    }

/**
 * From [the docs](https://webassembly.github.io/spec/core/syntax/values.html#aux-significand).
 */
fun Int.significand(context: ParseContext? = null): Int = when (this) {
    32 -> 23
    64 -> 52
    else -> throw ParseException("Illegal significand", context)
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/syntax/values.html#aux-exponent).
 */
fun Int.expon(context: ParseContext? = null): Int = when (this) {
    32 -> 8
    64 -> 11
    else -> throw ParseException("Illegal expon", context)
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/syntax/values.html#aux-canon).
 */
data class CanonincalNaN(val magnitude: Int) {
    val value: Long = magnitude.canon()
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/syntax/values.html#aux-canon).
 */
fun Int.canon(context: ParseContext? = null): Long =
    2.0.pow(this.significand(context) - 1).toLong()

internal object NumberConstants {
    val negativeLongWithOffset = 1 to -1L
    val positiveLongWithOffset = 1 to 1L
    val positiveLong = 0 to 1L
    val negativeDoubleWithOffset = 1 to -1.0
    val positiveDoubleWithOffset = 1 to 1.0
    val positiveDouble = 0 to 1.0

    const val UNDERSCORE = (-1).toByte()

    const val DEFAULT_FLOAT_MAGNITUDE = 64
}
