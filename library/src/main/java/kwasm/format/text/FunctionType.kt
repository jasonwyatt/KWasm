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

import kwasm.ast.AstNodeList
import kwasm.ast.FunctionType
import kwasm.ast.Param
import kwasm.format.ParseException
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Paren
import kwasm.format.text.token.Token

/**
 * Parses a FunctionType from a list of Tokens.
 * From [the docs](https://webassembly.github.io/spec/core/text/types.html#function-types):
 *
 * ```
 *   functype ::=  ‘(’ ‘func’  t*1:vec(param)  t*2:vec(result) ‘)’ => [t*1]→[t*2]
 *   param    ::=  ‘(’ ‘param’  id?  t:valtype ‘)’                 => t
 *   result   ::=  ‘(’ ‘result’  t:valtype ‘)’                     => t
 * ```
 */
fun List<Token>.parseFunctionType(currentIndex: Int): ParseResult<FunctionType> {
    var parsedTokens = 0
    val openParen = this[currentIndex]
    if (openParen !is Paren.Open) {
        throw ParseException("Invalid FunctionType: Expecting \"(\"", openParen.context)
    }
    parsedTokens++
    val keyword = this[currentIndex + 1]
    if (keyword !is Keyword || keyword.value != "func") {
        throw ParseException("Invalid FunctionType: Expecting \"func\"", keyword.context)
    }
    parsedTokens++
    val nextParamIndex = currentIndex + 2
    val parsedParamList = this.parseParamList(nextParamIndex)
    parsedTokens += parsedParamList.parseLength

    val nextResultIndex = nextParamIndex + parsedParamList.parseLength
    val parsedResultList = this.parseResultList(nextResultIndex)
    parsedTokens += parsedResultList.parseLength

    val closingParenIndex = nextResultIndex + parsedResultList.parseLength
    val closeParen = this[closingParenIndex]
    if (closeParen !is Paren.Closed) {
        throw ParseException("Invalid FunctionType: Expecting \")\"", closeParen.context)
    }
    parsedTokens++
    return ParseResult(FunctionType(parsedParamList.astNode, parsedResultList.astNode), parsedTokens)
}

/**
 * Parses a list of Param from a list of tokens.
 * @return a Pair containing the list of Params and an integer tracking the number of tokens parsed
 */
fun List<Token>.parseParamList(currentIndex: Int): ParseResult<AstNodeList<Param>> {
    var parsedTokens = 0
    var nextParamIndex = currentIndex
    val paramList = mutableListOf<Param>()
    while (this.size > nextParamIndex && this[nextParamIndex] is Paren.Open) {
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
    return ParseResult(AstNodeList(paramList), parsedTokens)
}

/**
 * Parses a list of Result from a list of tokens.
 * @return a Pair containing the list of Results and an integer tracking the number of tokens parsed
 */
fun List<Token>.parseResultList(currentIndex: Int): ParseResult<AstNodeList<kwasm.ast.Result>> {
    var parsedTokens = 0
    var nextResultIndex = currentIndex
    val resultList = mutableListOf<kwasm.ast.Result>()
    while (this.size > nextResultIndex && this[nextResultIndex] is Paren.Open) {
        val resultKeyword = this[nextResultIndex + 1]
        if (resultKeyword is Keyword && resultKeyword.value == "result") {
            val parsedResultResult = this.parseResult(nextResultIndex)
            resultList.add(parsedResultResult.astNode)
            nextResultIndex += parsedResultResult.parseLength
            parsedTokens += parsedResultResult.parseLength
        } else {
            break
        }
    }
    return ParseResult(AstNodeList(resultList), parsedTokens)
}
