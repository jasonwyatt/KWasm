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
import kwasm.ast.IntegerLiteral
import kwasm.ast.astNodeListOf
import kwasm.ast.instruction.Expression
import kwasm.ast.instruction.NumericConstantInstruction
import kwasm.ast.module.DataSegment
import kwasm.ast.module.Export
import kwasm.ast.module.ExportDescriptor
import kwasm.ast.module.Import
import kwasm.ast.module.ImportDescriptor
import kwasm.ast.module.Index
import kwasm.ast.module.Memory
import kwasm.ast.module.Offset
import kwasm.ast.type.Limits
import kwasm.ast.type.MemoryType
import kwasm.format.ParseException
import kwasm.format.parseCheck
import kwasm.format.parseCheckNotNull
import kwasm.format.text.ParseResult
import kwasm.format.text.contextAt
import kwasm.format.text.isClosedParen
import kwasm.format.text.isKeyword
import kwasm.format.text.isOpenParen
import kwasm.format.text.parseIdentifier
import kwasm.format.text.parseLiteral
import kwasm.format.text.token.Token
import kwasm.format.text.tokensUntilParenClosure
import kwasm.format.text.type.parseMemoryType
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
        return ParseResult(
            astNodeListOf(it.astNode),
            it.parseLength
        )
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
    val id = parseIdentifier<Identifier.Memory>(currentIndex, false)
    currentIndex += id.parseLength
    val memoryType = try { parseMemoryType(currentIndex) } catch (e: ParseException) { null }
        ?: return null
    currentIndex += memoryType.parseLength
    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++
    return ParseResult(
        Memory(id.astNode, memoryType.astNode),
        currentIndex - fromIndex
    )
}

/** See [parseMemory]. */
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
    val pageSize = ceil(dataBytes.size.toDouble() / 65536.0).toLong() // (64Ki)

    return ParseResult(
        astNodeListOf(
            Memory(
                id.astNode,
                MemoryType(
                    Limits(pageSize, pageSize)
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

/**
 * Parses an inline [Memory]-[Import] from the receiving [List] of [Token]s.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/modules.html#text-mem-abbrev):
 *
 * ```
 *   ‘(’ ‘memory’ id? ‘(’ ‘import’ name^1 name^2 ‘)’ memtype ‘)’
 *      == ‘(’ ‘import’ name^1 name^2 ‘(’ ‘memory’ id? memtype ‘)’ ‘)’
 * ```
 */
fun List<Token>.parseInlineMemoryImport(fromIndex: Int): ParseResult<Import>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "memory")) return null
    currentIndex++

    val identifier = parseIdentifier<Identifier.Memory>(currentIndex)
    currentIndex += identifier.parseLength

    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "import")) return null
    currentIndex++

    val moduleName = parseLiteral(currentIndex, String::class)
    currentIndex += moduleName.parseLength
    val tableName = parseLiteral(currentIndex, String::class)
    currentIndex += tableName.parseLength

    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++

    val memType = parseMemoryType(currentIndex)
    currentIndex += memType.parseLength

    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++

    return ParseResult(
        Import(
            moduleName.astNode.value,
            tableName.astNode.value,
            ImportDescriptor.Memory(
                identifier.astNode,
                memType.astNode
            )
        ),
        currentIndex - fromIndex
    )
}

/**
 * Parses an inline [Memory]-[Export] from the receiving [List] of [Token]s.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/modules.html#text-mem-abbrev):
 *
 * ```
 *   ‘(’ ‘memory’ id? ‘(’ ‘export’ name ‘)’ ... ‘)’
 *      == ‘(’ ‘export’ name ‘(’ ‘memory’ id′ ‘)’ ‘)’ ‘(’ ‘memory’ id′ ... ‘)’
 *          (if id′ = id? != ϵ ∨ id′ fresh)
 * ```
 *
 * Where “...” can contain another import or export or an inline data segment.
 */
fun List<Token>.parseInlineMemoryExport(fromIndex: Int): ParseResult<AstNodeList<*>>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "memory")) return null
    currentIndex++

    val identifier = parseIdentifier<Identifier.Memory>(currentIndex)
    currentIndex += identifier.parseLength

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
            ExportDescriptor.Memory(
                Index.ByIdentifier(
                    identifier.astNode
                )
            )
        )
    )

    if (isClosedParen(currentIndex)) {
        // No `...` case, so we can return here.
        currentIndex++
        return ParseResult(
            AstNodeList(result),
            currentIndex - fromIndex
        )
    }

    // Looks like there might be more to handle, so construct a new list of tokens to recurse with.
    // Add the opening paren and 'memory' keyword.
    val withoutFirstExport = mutableListOf(
        this[fromIndex],
        this[fromIndex + 1]
    )

    // Add the id.
    if (identifier.parseLength == 0) {
        withoutFirstExport.add(kwasm.format.text.token.Identifier(identifier.astNode.toString()))
    } else {
        (0 until identifier.parseLength).forEach {
            withoutFirstExport.add(this[fromIndex + 2 + it])
        }
    }

    val lengthOfPrefix = withoutFirstExport.size

    // Add all of the tokens until the closing paren for the memory block.
    withoutFirstExport.addAll(tokensUntilParenClosure(currentIndex, expectedClosures = 1))

    // Recurse.
    val additionalItems = parseCheckNotNull(
        contextAt(currentIndex),
        withoutFirstExport.parseInlineMemoryExport(0)
            ?: withoutFirstExport.parseInlineMemoryImport(0)
            ?: withoutFirstExport.parseMemory(0),
        "Expected additional details for memory"
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
