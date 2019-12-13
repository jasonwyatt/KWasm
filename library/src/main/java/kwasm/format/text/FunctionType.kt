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

import kwasm.ast.FunctionType
import kwasm.ast.Param
import kwasm.format.ParseException
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Paren
import kwasm.format.text.token.Token

fun List<Token>.parseFunctionType(currentIndex: Int): ParseResult<FunctionType> {
    var parsedTokens = 0
    val openParen = this[currentIndex]
    if (openParen !is Paren.Open) {
        throw ParseException("Invalid FunctionType: Expecting ( token", openParen.context)
    }
    parsedTokens++
    val keyword = this[currentIndex + 1]
    if (keyword !is Keyword || keyword.value != "func") {
        throw ParseException("Invalid FunctionType: Expecting func token", keyword.context)
    }
    parsedTokens++
    var nextParamIndex = currentIndex + 2
    val paramList = mutableListOf<Param>()
    while (this[nextParamIndex] is Paren.Open) {
        val paramKeyword = this[nextParamIndex + 1]
        if (paramKeyword is Keyword && paramKeyword.value == "param") {
            val parsedParamResult = this.parseParam(nextParamIndex)
            paramList.add(parsedParamResult.astNode)
            nextParamIndex += parsedParamResult.parseLength
            parsedTokens += parsedParamResult.parseLength
        } else {
            break
        }
    }
    var nextResultIndex = nextParamIndex

    val resultList = mutableListOf<kwasm.ast.Result>()
    while (this[nextResultIndex] is Paren.Open) {
        val resultKeyword = this[nextResultIndex + 1]
        if (resultKeyword is Keyword && resultKeyword.value == "result") {
            val parsedResultResult = this.parseResult(nextResultIndex)
            resultList.add(parsedResultResult.astNode)
            nextResultIndex += parsedResultResult.parseLength
            parsedTokens += parsedResultResult.parseLength
        } else {
            throw ParseException("Invalid FunctionType: Expecting result token", resultKeyword.context)
        }
    }
    val closeParen = this[nextResultIndex]
    if (closeParen !is Paren.Closed) {
        throw ParseException("Invalid FunctionType: Expecting ) token", closeParen.context)
    }
    parsedTokens++
    return ParseResult(FunctionType(paramList, resultList), parsedTokens)
}