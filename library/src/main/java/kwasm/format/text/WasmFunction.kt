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
import kwasm.ast.WasmFunction
import kwasm.format.parseCheck
import kwasm.format.parseCheckNotNull
import kwasm.format.text.token.IntegerLiteral
import kwasm.format.text.token.Token

/**
 * Parses a [WasmFunction] from the receiving [List] of [Token]s.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/modules.html#functions):
 *
 * ```
 *   func_I ::= ‘(’ ‘func’ id? x,I′:typeuse_I (t:local)* (in:instr_I′‘)‘* ‘)’
 *              => {type x, locals t*, body in* end}
 * ```
 */
fun List<Token>.parseWasmFunction(fromIndex: Int): ParseResult<WasmFunction>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "func")) return null
    currentIndex++

    val id = parseIdentifier<Identifier.Function>(currentIndex)
    currentIndex += id.parseLength
    val typeUse = parseTypeUse(currentIndex)
    currentIndex += typeUse.parseLength
    val locals = parseLocals(currentIndex)
    currentIndex += locals.parseLength
    val instructions = parseInstructions(currentIndex)
    currentIndex += instructions.parseLength
    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++

    return ParseResult(
        WasmFunction(id.astNode, typeUse.astNode, locals.astNode, instructions.astNode),
        currentIndex - fromIndex
    )
}
