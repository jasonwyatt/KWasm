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

import kwasm.ast.Global
import kwasm.ast.Identifier
import kwasm.format.parseCheck
import kwasm.format.text.token.Token

/**
 * Parses a [Global] from the receiving [List] of [Token]s.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/modules.html#globals):
 *
 * ```
 *   global_I   ::= ‘(’ ‘global’ id? gt:globaltype  e:expr_I ‘)’    => {type gt, init e}
 * ```
 */
fun List<Token>.parseGlobal(fromIndex: Int): ParseResult<Global>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "global")) return null
    currentIndex++

    val id = parseIdentifier<Identifier.Global>(currentIndex)
    currentIndex += id.parseLength

    val globalType = parseGlobalType(currentIndex)
    currentIndex += globalType.parseLength

    val expression = parseExpression(currentIndex)
    currentIndex += expression.parseLength

    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++

    return ParseResult(
        Global(id.astNode, globalType.astNode, expression.astNode),
        currentIndex - fromIndex
    )
}