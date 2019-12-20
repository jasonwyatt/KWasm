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
import kwasm.format.ParseContext
import kwasm.format.parseCheck
import kwasm.format.text.token.Identifier
import kwasm.format.text.token.Token

/**
 * This sealed class encapsulates all Types defined in
 * [the docs](https://webassembly.github.io/spec/core/text/types.html)
 *
 * @param T the Object representing the value related to the type and value parsed
 */
@UseExperimental(ExperimentalUnsignedTypes::class)
sealed class Type<T>(
    protected val sequence: CharSequence,
    protected val context: ParseContext? = null
) {
    val value: T by lazy { parseValue() }

    protected abstract fun parseValue(): T

    class TableType(
        sequence: CharSequence,
        context: ParseContext? = null
    ) : Type<Unit>(sequence, context) {
        override fun parseValue() {
            TODO("not implemented")
        }
    }

    class ElementType(
        sequence: CharSequence,
        context: ParseContext? = null
    ) : Type<Unit>(sequence, context) {
        override fun parseValue() {
            TODO("not implemented")
        }
    }

}

/**
 * Parses a type as a [FunctionType] from the receiving [List] of [Token]s.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/modules.html#types):
 *
 * ```
 *   type ::= ‘(’ ‘type’ id? ft:functype ‘)’ => ft
 * ```
 */
fun List<Token>.parseType(fromIndex: Int): ParseResult<kwasm.ast.Type>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++

    getOrNull(currentIndex)?.asKeywordMatching("type") ?: return null
    currentIndex++

    val identifier = getOrNull(currentIndex) as? Identifier
    if (identifier != null) currentIndex++

    val funcType = parseFunctionType(currentIndex)
    currentIndex += funcType.parseLength

    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++

    val astTypeIdentifier = identifier?.let { kwasm.ast.Identifier.Type(identifier.value) }

    return ParseResult(
        kwasm.ast.Type(astTypeIdentifier, funcType.astNode),
        currentIndex - fromIndex
    )
}
