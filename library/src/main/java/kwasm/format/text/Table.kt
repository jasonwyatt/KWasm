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
import kwasm.ast.ElementSegment
import kwasm.ast.ElementType
import kwasm.ast.Expression
import kwasm.ast.Identifier
import kwasm.ast.Index
import kwasm.ast.IntegerLiteral
import kwasm.ast.Limit
import kwasm.ast.NumericConstantInstruction
import kwasm.ast.Offset
import kwasm.ast.Table
import kwasm.ast.TableType
import kwasm.ast.astNodeListOf
import kwasm.format.ParseException
import kwasm.format.parseCheck
import kwasm.format.parseCheckNotNull
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Token

/**
 * Parses a [Table] and (optionally) an [ElementType] from the receiving [List] of [Token]s.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/modules.html#tables):
 *
 * ```
 *   table_I ::= ‘(’ ‘table’ id? tt:tabletype ‘)’ => {type tt}
 * ```
 *
 * **Abbreviations**
 *
 * An element segment can be given inline with a table definition, in which case its offset is `0`
 * and the limits of the table type are inferred from the length of the given segment:
 *
 * ```
 *   ‘(’ ‘table’ id? elemtype ‘(’ ‘elem’ xn:vec(funcidx) ‘)’ ‘)’
 *      == ‘(’ ‘table’ id′ n n elemtype ‘)’ ‘(’ ‘elem’ id′ ‘(’ ‘i32.const’ ‘0’ ‘)’ vec(funcidx) ‘)’
 *          (if id′ == id? != ϵ ∨ id′ fresh)
 * ```
 */
fun List<Token>.parseTable(fromIndex: Int): ParseResult<AstNodeList<*>>? {
    parseTableBasic(fromIndex)?.let {
        return ParseResult(astNodeListOf(it.astNode), it.parseLength)
    }
    return parseTableAndElementSegment(fromIndex)
}

/** See [parseTable]. */
internal fun List<Token>.parseTableBasic(fromIndex: Int): ParseResult<Table>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "table")) return null
    currentIndex++
    val id = parseIdentifier<Identifier.Table>(currentIndex)
    currentIndex += id.parseLength
    val tableType = try { parseTableType(currentIndex) } catch (e: ParseException) { null }
        ?: return null
    currentIndex += tableType.parseLength
    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++
    return ParseResult(Table(id.astNode, tableType.astNode), currentIndex - fromIndex)
}

/** See [parseTable]. */
@UseExperimental(ExperimentalUnsignedTypes::class)
internal fun List<Token>.parseTableAndElementSegment(fromIndex: Int): ParseResult<AstNodeList<*>>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "table")) return null
    currentIndex++
    val id = parseIdentifier<Identifier.Table>(currentIndex)
    currentIndex += id.parseLength

    val keyword = parseCheckNotNull(
        contextAt(currentIndex),
        getOrNull(currentIndex) as? Keyword,
        "Expected an ElementType"
    )
    val elemType = when (keyword.value) {
        "funcref" -> ElementType.FunctionReference
        else -> return null
    }
    currentIndex++

    parseCheck(
        contextAt(currentIndex),
        isOpenParen(currentIndex),
        "Expected inline element segment"
    )
    currentIndex++
    parseCheck(
        contextAt(currentIndex),
        isKeyword(currentIndex, "elem"),
        "Expected inline element segment"
    )
    currentIndex++

    val funcIndices = parseIndices<Identifier.Function>(currentIndex)
    currentIndex += funcIndices.parseLength

    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++

    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++

    return ParseResult(
        astNodeListOf(
            Table(
                id.astNode,
                TableType(
                    Limit(funcIndices.astNode.size.toUInt(), funcIndices.astNode.size.toUInt()),
                    elemType
                )
            ),
            ElementSegment(
                Index.ByIdentifier(id.astNode),
                Offset(
                    Expression(
                        astNodeListOf(NumericConstantInstruction.I32(IntegerLiteral.S32(0)))
                    )
                ),
                funcIndices.astNode
            )
        ),
        currentIndex - fromIndex
    )
}