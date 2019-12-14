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

import kwasm.ast.Literal
import kwasm.format.ParseException
import kwasm.format.text.token.FloatLiteral
import kwasm.format.text.token.IntegerLiteral
import kwasm.format.text.token.StringLiteral
import kwasm.format.text.token.Token

/** Parses a [Literal] of type [T] from the receiving [List] of [Token]s at the given position. */
@UseExperimental(ExperimentalUnsignedTypes::class)
inline fun <reified T> List<Token>.parseLiteral(fromIndex: Int): ParseResult<Literal<T>> {
    val node = getOrNull(fromIndex)
    val context = node?.context ?: getOrNull(fromIndex - 1)?.context

    @Suppress("UNCHECKED_CAST")
    val literalNode = when (T::class) {
        Int::class -> {
            var literalToken = node as? IntegerLiteral.Signed
                ?: node as? IntegerLiteral.Unsigned
                ?: throw ParseException("Expected i32", context)
            literalToken = literalToken.toSigned()
            literalToken.magnitude = 32

            kwasm.ast.IntegerLiteral.S32(literalToken.value.toInt())
        }
        UInt::class -> {
            var literalToken = node as? IntegerLiteral.Signed
                ?: node as? IntegerLiteral.Unsigned
                ?: throw ParseException("Expected i32", context)
            literalToken = literalToken.toUnsigned()
            literalToken.magnitude = 32

            kwasm.ast.IntegerLiteral.U32(literalToken.value.toUInt())
        }
        Long::class -> {
            var literalToken = node as? IntegerLiteral.Signed
                ?: node as? IntegerLiteral.Unsigned
                ?: throw ParseException("Expected i64", context)
            literalToken = literalToken.toSigned()
            literalToken.magnitude = 64

            kwasm.ast.IntegerLiteral.S64(literalToken.value)
        }
        ULong::class -> {
            var literalToken = node as? IntegerLiteral.Signed
                ?: node as? IntegerLiteral.Unsigned
                ?: throw ParseException("Expected i64", context)
            literalToken = literalToken.toUnsigned()

            kwasm.ast.IntegerLiteral.U64(literalToken.value)
        }
        Float::class -> {
            val literalToken = node as? FloatLiteral
                ?: throw ParseException("Expected f32", context)
            literalToken.magnitude = 32

            kwasm.ast.FloatLiteral.SinglePrecision(literalToken.value.toFloat())
        }
        Double::class -> {
            val literalToken = node as? FloatLiteral
                ?: throw ParseException("Expected f64", context)
            literalToken.magnitude = 64

            kwasm.ast.FloatLiteral.DoublePrecision(literalToken.value)
        }
        String::class -> {
            val literalToken = node as? StringLiteral
                ?: throw ParseException("Expected String", context)

            kwasm.ast.StringLiteral(literalToken.value)
        }
        else -> throw ParseException("Expected literal value", context)
    } as Literal<T>

    return ParseResult(literalNode, 1)
}
