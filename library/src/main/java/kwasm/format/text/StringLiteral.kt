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

package kwasm.format.text

import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.shiftColumnBy

/**
 * A [String] literal.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/values.html#strings):
 *
 * ```
 *   string ::= ‘"’ (b*:stringelem)* ‘"’ => concat((b*)*) (if |concat((b*)*)| < 2^32)
 * ```
 *
 * The JVM will crash if the string is 4 billion characters, so no need to perform the check.
 */
class StringLiteral(private val sequence: CharSequence, private val context: ParseContext? = null) {
    val value: String by lazy {
        val builder = StringBuilder()

        if (sequence.first() != '"') {
            throw ParseException("Expecting opening \" for StringLiteral", context)
        }
        if (sequence.last() != '"') {
            throw ParseException("Expecting closing \" for StringLiteral", context)
        }

        val internalSequence = sequence.subSequence(1, sequence.length - 1)

        val stringElem = StringChar()
        var i = 0
        while (i < internalSequence.length) {
            internalSequence.parseStringElem(i, stringElem, context.shiftColumnBy(i + 1))
            builder.appendCodePoint(stringElem.value)
            i += stringElem.sequenceLength
        }

        builder.toString()
    }

    override fun equals(other: Any?): Boolean = other is StringLiteral && other.value == value

    override fun hashCode(): Int = value.hashCode()
}
