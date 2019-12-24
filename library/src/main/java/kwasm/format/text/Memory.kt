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
import kwasm.ast.DataSegment
import kwasm.ast.Expression
import kwasm.ast.Identifier
import kwasm.ast.Index
import kwasm.ast.IntegerLiteral
import kwasm.ast.Limit
import kwasm.ast.Memory
import kwasm.ast.MemoryType
import kwasm.ast.NumericConstantInstruction
import kwasm.ast.Offset
import kwasm.ast.astNodeListOf
import kwasm.format.ParseException
import kwasm.format.parseCheck
import kwasm.format.text.token.Token
import kotlin.math.ceil

/**
 * Parses a [Memory] from the receiving [List] of [Token]s.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/modules.html#memories):
 *
 * ```
 *   mem_I  ::= ‘(’ 'memory'  id?  mt:memtype ‘)’   => {type mt}
 * ```
 *
 * **Abbreviations**
 *
 * A data segment can be given inline with a memory definition, in which case its offset is `0` the
 * limits of the memory type are inferred from the length of the data, rounded up to page size:
 *
 * ```
 *   ‘(’ ‘memory’  id?  ‘(’ ‘data’  bn:datastring ‘)’  ‘)’
 *      ≡   ‘(’ ‘memory’  id′  m  m ‘)’ ‘(’ ‘data’  id′  ‘(’ ‘i32.const’  ‘0’ ‘)’  datastring ‘)’
 *          (if id′ = id? != ϵ ∨ id′ fresh, m=ceil(n/64Ki))
 * ```
 */
fun List<Token>.parseMemory(fromIndex: Int): ParseResult<AstNodeList<*>>? {
    parseMemoryBasic(fromIndex)?.let {
        return ParseResult(astNodeListOf(it.astNode), it.parseLength)
    }
    return parseMemoryAndDataSegment(fromIndex)
}

/** See [parseMemory]. */
internal fun List<Token>.parseMemoryBasic(fromIndex: Int): ParseResult<Memory>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "memory")) return null
    currentIndex++
    val id = parseIdentifier<Identifier.Memory>(currentIndex)
    currentIndex += id.parseLength
    val memoryType = try { parseMemoryType(currentIndex) } catch (e: ParseException) { null }
        ?: return null
    currentIndex += memoryType.parseLength
    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++
    return ParseResult(Memory(id.astNode, memoryType.astNode), currentIndex - fromIndex)
}

/** See [parseMemory]. */
@Suppress("EXPERIMENTAL_API_USAGE")
internal fun List<Token>.parseMemoryAndDataSegment(fromIndex: Int): ParseResult<AstNodeList<*>>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "memory")) return null
    currentIndex++
    val id = parseIdentifier<Identifier.Memory>(currentIndex)
    currentIndex += id.parseLength

    parseCheck(
        contextAt(currentIndex),
        isOpenParen(currentIndex),
        "Expected inline data segment"
    )
    currentIndex++
    parseCheck(
        contextAt(currentIndex),
        isKeyword(currentIndex, "data"),
        "Expected inline data segment"
    )
    currentIndex++

    val dataStringBuilder = StringBuilder()
    while (true) {
        val dataString = try {
            parseLiteral(currentIndex, String::class)
        } catch (e: ParseException) {
            break
        }
        currentIndex += dataString.parseLength
        dataStringBuilder.append(dataString.astNode.value)
    }

    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++

    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++

    val dataBytes = dataStringBuilder.toString().toByteArray(Charsets.UTF_8)
    val pageSize = ceil(dataBytes.size.toDouble() / 65536.0).toUInt() // (64Ki)

    return ParseResult(
        astNodeListOf(
            Memory(
                id.astNode,
                MemoryType(
                    Limit(pageSize, pageSize)
                )
            ),
            DataSegment(
                Index.ByIdentifier(id.astNode),
                Offset(
                    Expression(
                        astNodeListOf(NumericConstantInstruction.I32(IntegerLiteral.S32(0)))
                    )
                ),
                dataBytes
            )
        ),
        currentIndex - fromIndex
    )
}
