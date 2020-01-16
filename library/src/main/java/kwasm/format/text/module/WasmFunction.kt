/*
 * Copyright 2020 Google LLC
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

package kwasm.format.text.module

import kwasm.ast.AstNode
import kwasm.ast.AstNodeList
import kwasm.ast.Identifier
import kwasm.ast.module.Export
import kwasm.ast.module.ExportDescriptor
import kwasm.ast.module.Import
import kwasm.ast.module.ImportDescriptor
import kwasm.ast.module.Index
import kwasm.ast.module.WasmFunction
import kwasm.format.parseCheck
import kwasm.format.parseCheckNotNull
import kwasm.format.text.ParseResult
import kwasm.format.text.contextAt
import kwasm.format.text.instruction.parseInstructions
import kwasm.format.text.isClosedParen
import kwasm.format.text.isKeyword
import kwasm.format.text.isOpenParen
import kwasm.format.text.parseIdentifier
import kwasm.format.text.parseLiteral
import kwasm.format.text.token.Token
import kwasm.format.text.tokensUntilParenClosure

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

    val id = parseIdentifier<Identifier.Function>(currentIndex, true)
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
        WasmFunction(
            id.astNode,
            typeUse.astNode,
            locals.astNode,
            instructions.astNode
        ),
        currentIndex - fromIndex
    )
}

/**
 * Parses an inline-import function declaration from the receiving [List] of [Token]s.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/modules.html#text-func-abbrev):
 *
 * ```
 *   ‘(’ ‘func’ id? ‘(’ ‘import’ name^1 name^2 ‘)’ typeuse ‘)’
 *      == ‘(’ ‘import’ name^1 name^2 ‘(’ ‘func’ id? typeuse ‘)’ ‘)’
 * ```
 */
fun List<Token>.parseInlineWasmFunctionImport(fromIndex: Int): ParseResult<Import>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "func")) return null
    currentIndex++

    val id = parseIdentifier<Identifier.Function>(currentIndex)
    currentIndex += id.parseLength

    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "import")) return null
    currentIndex++

    val moduleName = parseLiteral(currentIndex, String::class)
    currentIndex += moduleName.parseLength
    val funcName = parseLiteral(currentIndex, String::class)
    currentIndex += funcName.parseLength

    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++

    val typeUse = parseTypeUse(currentIndex)
    currentIndex += typeUse.parseLength

    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++

    return ParseResult(
        Import(
            moduleName.astNode.value,
            funcName.astNode.value,
            ImportDescriptor.Function(
                id.astNode,
                typeUse.astNode
            )
        ),
        currentIndex - fromIndex
    )
}

/**
 * Parses an inline-export function from the receiving [List] of [Token]s.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/modules.html#text-func-abbrev):
 *
 * ```
 *   ‘(’ ‘func’ id? ‘(’ ‘export’ name ‘)’ … ‘)’
 *      == ‘(’ ‘export’ name ‘(’ ‘func’ id′ ‘)’ ‘)’ ‘(’ ‘func’ id′ … ‘)’
 *              (if id′ == id? != ϵ ∨ id′ fresh)
 * ```
 */
fun List<Token>.parseInlineWasmFunctionExport(fromIndex: Int): ParseResult<AstNodeList<*>>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "func")) return null
    currentIndex++

    val id = parseIdentifier<Identifier.Function>(currentIndex)
    currentIndex += id.parseLength

    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "export")) return null
    currentIndex++

    val exportName = parseLiteral(currentIndex, String::class)
    currentIndex += exportName.parseLength

    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++

    val result = mutableListOf<AstNode>(
        Export(
            exportName.astNode.value,
            ExportDescriptor.Function(Index.ByIdentifier(id.astNode))
        )
    )

    // Looks like there might be more to handle, so construct a new list of tokens to recurse with.
    // Add the opening paren and 'func' keyword.
    val withoutFirstExport = mutableListOf(
        this[fromIndex],
        this[fromIndex + 1]
    )

    // Add the id.
    if (id.parseLength == 0) {
        withoutFirstExport.add(kwasm.format.text.token.Identifier(id.astNode.toString()))
    } else {
        (0 until id.parseLength).forEach {
            withoutFirstExport.add(this[fromIndex + 2 + it])
        }
    }

    val lengthOfPrefix = withoutFirstExport.size

    // Add all of the tokens until the closing paren for the func block.
    withoutFirstExport.addAll(tokensUntilParenClosure(currentIndex, expectedClosures = 1))

    if (isClosedParen(currentIndex)) {
        // No `...` case, so we can return here.
        val function = parseCheckNotNull(contextAt(currentIndex), withoutFirstExport.parseWasmFunction(0))
        currentIndex += (function.parseLength - lengthOfPrefix)
        return ParseResult(
            AstNodeList(
                result.apply {
                    add(0, function.astNode)
                }
            ),
            currentIndex - fromIndex
        )
    }

    // Recurse.
    val additionalItems = parseCheckNotNull(
        contextAt(currentIndex),
        withoutFirstExport.parseInlineWasmFunctionExport(0)
            ?: withoutFirstExport.parseInlineWasmFunctionImport(0)
            ?: withoutFirstExport.parseWasmFunction(0),
        "Expected additional details for func"
    )
    currentIndex += (additionalItems.parseLength - lengthOfPrefix)

    when (val node = additionalItems.astNode) {
        is AstNodeList<*> -> result.addAll(node)
        else -> result.add(node)
    }

    return ParseResult(
        AstNodeList(result),
        currentIndex - fromIndex
    )
}
