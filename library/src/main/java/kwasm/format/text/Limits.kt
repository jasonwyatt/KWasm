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

import kwasm.ast.Limit
import kwasm.format.ParseException
import kwasm.format.text.token.IntegerLiteral
import kwasm.format.text.token.Paren
import kwasm.format.text.token.Token

/**
 * From [the spec](]https://webassembly.github.io/spec/core/text/types.html#limits):
 *
 * ```
 *   limits ::=  n:u32        => {min n, max ϵ}
 *           |   n:u32  m:u32 => {min n, max m}
 * ```
 */
@UseExperimental(ExperimentalUnsignedTypes::class)
fun List<Token>.parseLimits(startingIndex: Int): ParseResult<Limit> {
    if (startingIndex >= this.size) throw ParseException("Expected integer literal")
    val min = this[startingIndex] as? IntegerLiteral.Unsigned ?: throw ParseException(
        "Expected integer literal",
        this[startingIndex].context
    )
    min.magnitude = 32
    val maxOrCloseParenIndex = startingIndex + 1
    if (maxOrCloseParenIndex >= this.size || this[maxOrCloseParenIndex] is Paren.Closed) return ParseResult(
        Limit(
            min.value.toUInt(),
            UInt.MAX_VALUE
        ), 1
    )
    val max = this[maxOrCloseParenIndex] as? IntegerLiteral.Unsigned ?: throw ParseException(
        "Expected integer literal",
        this[maxOrCloseParenIndex].context
    )
    max.magnitude = 32
    if (min.value > max.value) throw ParseException("Arguments out of order, min > max. min: ${min.value}, max: ${max.value}")
    return ParseResult(Limit(min.value.toUInt(), max.value.toUInt()), 2)
}