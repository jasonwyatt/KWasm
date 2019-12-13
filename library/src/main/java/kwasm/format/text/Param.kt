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

import kwasm.ast.Identifier
import kwasm.ast.Param
import kwasm.format.ParseException
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Paren
import kwasm.format.text.token.Token

/**
 * Parses a Param from a list of Tokens.
 * from [the docs](https://webassembly.github.io/spec/core/text/types.html#function-types):
 *
 * ```
 *   param    ::=  ‘(’ ‘param’  id?  t:valtype ‘)’  => t
 * ```
 */
fun List<Token>.parseParam(currentIndex: Int): ParseResult<Param> {
    var parsedTokens = 0
    val openParen = this[currentIndex]
    if (openParen !is Paren.Open) {
        throw ParseException("Invalid Param: Expecting \"(\"", openParen.context)
    }
    parsedTokens++
    val keyword = this[currentIndex + 1]
    if (keyword !is Keyword || keyword.value != "param") {
        throw ParseException("Invalid Param: Expecting \"param\"", keyword.context)
    }
    parsedTokens++
    val valueTypeIndex: Int
    val id: Identifier.Local?
    val idOrValueType = this[currentIndex + 2]
    if (idOrValueType is kwasm.format.text.token.Identifier) {
        valueTypeIndex = currentIndex + 3
        id = Identifier.Local(idOrValueType.value)
        parsedTokens++
    } else {
        valueTypeIndex = currentIndex + 2
        id = null
    }
    val valueTypeParseResult = this.parseValueType(valueTypeIndex)
    parsedTokens += valueTypeParseResult.parseLength
    val closeParen = this[valueTypeIndex + 1]
    if (closeParen !is Paren.Closed) {
        throw ParseException("Invalid Param: Expecting ) token", closeParen.context)
    }
    parsedTokens++
    return ParseResult(Param(id, valueTypeParseResult.astNode), parsedTokens)
}