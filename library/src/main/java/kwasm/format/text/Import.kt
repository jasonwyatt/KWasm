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
import kwasm.ast.Import
import kwasm.ast.ImportDescriptor
import kwasm.format.parseCheck
import kwasm.format.parseCheckNotNull
import kwasm.format.text.token.Token

/**
 * Parses an [Import] from the receiving [List] of [Token]s.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/modules.html#imports):
 *
 * ```
 *   import_I ::= ‘(’ ‘import’ mod:name nm:name d:importdesc_I ‘)’ => {module mod, name nm, desc d}
 * ```
 */
fun List<Token>.parseImport(fromIndex: Int): ParseResult<Import>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "import")) return null
    currentIndex++
    val moduleName = parseLiteral(currentIndex, String::class)
    currentIndex += moduleName.parseLength
    val name = parseLiteral(currentIndex, String::class)
    currentIndex += moduleName.parseLength
    val importDescriptor = parseCheckNotNull(
        contextAt(currentIndex),
        parseImportDescriptor(currentIndex),
        "Expected import descriptor"
    )
    currentIndex += importDescriptor.parseLength
    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++
    return ParseResult(
        Import(moduleName.astNode.value, name.astNode.value, importDescriptor.astNode),
        currentIndex - fromIndex
    )
}

/**
 * Parses an [ImportDescriptor] of type [T] from the receiving [List] of [Tokens].
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/modules.html#imports):
 *
 * ```
 *   importdesc_I  ::= ‘(’ ‘func’ id? x,I':typeuse_I ‘)’  => func x
 *                     ‘(’ ‘table’ id? tt:tabletype ‘)’   => table tt
 *                     ‘(’ ‘memory’ id? mt:memtype ‘)’    => mem mt
 *                     ‘(’ ‘global’ id? gt:globaltype ‘)’ => global gt
 * ```
 */
@Suppress("UNCHECKED_CAST")
fun List<Token>.parseImportDescriptor(fromIndex: Int): ParseResult<out ImportDescriptor>? =
    parseFuncImportDescriptor(fromIndex)
        ?: parseTableImportDescriptor(fromIndex)
        ?: parseMemoryImportDescriptor(fromIndex)
        ?: parseGlobalImportDescriptor(fromIndex)

internal fun List<Token>.parseFuncImportDescriptor(fromIndex: Int): ParseResult<ImportDescriptor.Function>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "func")) return null
    currentIndex++
    val id = parseIdentifier<Identifier.Function>(currentIndex)
    currentIndex += id.parseLength
    val typeUse = parseTypeUse(currentIndex)
    currentIndex += typeUse.parseLength
    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++
    return ParseResult(
        ImportDescriptor.Function(id.astNode, typeUse.astNode),
        currentIndex - fromIndex
    )
}

internal fun List<Token>.parseTableImportDescriptor(fromIndex: Int): ParseResult<ImportDescriptor.Table>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "table")) return null
    currentIndex++
    val id = parseIdentifier<Identifier.Table>(currentIndex)
    currentIndex += id.parseLength
    val tableType = parseTableType(currentIndex)
    currentIndex += tableType.parseLength
    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++
    return ParseResult(
        ImportDescriptor.Table(id.astNode, tableType.astNode),
        currentIndex - fromIndex
    )
}

internal fun List<Token>.parseMemoryImportDescriptor(fromIndex: Int): ParseResult<ImportDescriptor.Memory>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "memory")) return null
    currentIndex++
    val id = parseIdentifier<Identifier.Memory>(currentIndex)
    currentIndex += id.parseLength
    val memoryType = parseMemoryType(currentIndex)
    currentIndex += memoryType.parseLength
    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++
    return ParseResult(
        ImportDescriptor.Memory(id.astNode, memoryType.astNode),
        currentIndex - fromIndex
    )
}

internal fun List<Token>.parseGlobalImportDescriptor(fromIndex: Int): ParseResult<ImportDescriptor.Global>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "global")) return null
    currentIndex++
    val id = parseIdentifier<Identifier.Global>(currentIndex)
    currentIndex += id.parseLength
    val globalType = parseGlobalType(currentIndex)
    currentIndex += globalType.parseLength
    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++
    return ParseResult(
        ImportDescriptor.Global(id.astNode, globalType.astNode),
        currentIndex - fromIndex
    )
}
