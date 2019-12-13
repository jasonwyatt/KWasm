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


import kwasm.ast.Result
import kwasm.format.ParseException
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Paren
import kwasm.format.text.token.Token

/**
 * Parses a Result from a list of Tokens.
 * from [the docs](https://webassembly.github.io/spec/core/text/types.html#function-types):
 *
 * ```
 *   result   ::=  ‘(’ ‘result’  t:valtype ‘)’  => t
 * ```
 */
fun List<Token>.parseResult(currentIndex: Int): ParseResult<Result> {
    val openParen = this[currentIndex]
    if (openParen !is Paren.Open) {
        throw ParseException("Invalid Result: Expecting \"(\"", openParen.context)
    }
    val keyword = this[currentIndex + 1]
    if (keyword !is Keyword || keyword.value != "result") {
        throw ParseException("Invalid Result: Expecting \"result\"", keyword.context)
    }
    val valueTypeParseResult = this.parseValueType(currentIndex + 2)
    val closeParen = this[currentIndex + valueTypeParseResult.parseLength + 2]
    if (closeParen !is Paren.Closed) {
        throw ParseException("Invalid Result: Expecting \")\"", closeParen.context)
    }
    return ParseResult(Result(valueTypeParseResult.astNode), valueTypeParseResult.parseLength + 3)
}