package kwasm.format.text

import kwasm.format.ParseContext
import kwasm.format.ParseException

/**
 * From [the docs](https://webassembly.github.io/spec/core/text/values.html#integers).
 *
 * All integers can be written in either decimal or hexadecimal notation. In both cases, digits can
 * optionally be separated by underscores.
 *
 * ```
 *   digit      ::= '0' => 0, '1' => 1, ... '9' => 9
 *   hexdigit   ::= d:digit => d
 *                  'A' => 10, 'B' => 11, ... 'F' => 15
 *                  'a' => 10, 'b' => 11, ... 'f' => 15
 *   num        ::= d:digit => d
 *                  n:num '_'? d:digit => n * 10 + d
 *   hexnum     ::= h:hexdigit => h
 *                  n:hexnum '_'? h:hexdigit => n * 16 + h
 * ```
 */
@UseExperimental(ExperimentalUnsignedTypes::class)
class Num(private val sequence: CharSequence, context: ParseContext? = null) {
    var forceHex: Boolean = false

    val foundHexChars: Boolean by lazy { digits.any { it >= 10 } }

    val value: ULong by lazy {
        val multiplier = (if (foundHexChars || forceHex) 16 else 10).toULong()
        var powerVal = 1.toULong()
        var power = 0
        digits.foldRightIndexed(0.toULong()) { index, byteVal, acc ->
            if (byteVal == NumberConstants.UNDERSCORE) return@foldRightIndexed acc
            if (power > 0) {
                powerVal *= multiplier
            }
            power++
            acc + byteVal.toULong() * powerVal
        }
    }

    private val digits: ByteArray by lazy {
        if (sequence.isEmpty()) throw ParseException("Empty number sequence", context)

        val value = ByteArray(sequence.length)
        repeat(sequence.length) { index -> value[index] = sequence.parseDigit(index, context) }
        value
    }
}
