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
import kwasm.ast.Index
import kwasm.format.ParseException
import kwasm.format.text.token.Identifier
import kwasm.format.text.token.IntegerLiteral
import kwasm.format.text.token.Token

/**
 * Parses an [Index] from the receiving [List] of [Token]s.
 *
 * For details on the grammar, see [Index].
 */
@UseExperimental(ExperimentalUnsignedTypes::class)
inline fun <reified T : kwasm.ast.Identifier> List<Token>.parseIndex(
    fromIndex: Int
): ParseResult<out Index<T>> = when (val token = this[fromIndex]) {
    is IntegerLiteral.Unsigned -> {
        token.magnitude = 32
        @Suppress("UNCHECKED_CAST")
        ParseResult(Index.ByInt(token.value.toUInt()) as Index<T>, 1)
    }
    is Identifier -> {
        val id = when (T::class) {
            kwasm.ast.Identifier.Type::class -> kwasm.ast.Identifier.Type(token.value)
            kwasm.ast.Identifier.Function::class -> kwasm.ast.Identifier.Function(token.value)
            kwasm.ast.Identifier.Table::class -> kwasm.ast.Identifier.Table(token.value)
            kwasm.ast.Identifier.Memory::class -> kwasm.ast.Identifier.Memory(token.value)
            kwasm.ast.Identifier.Global::class -> kwasm.ast.Identifier.Global(token.value)
            kwasm.ast.Identifier.Local::class -> kwasm.ast.Identifier.Local(token.value)
            kwasm.ast.Identifier.Label::class -> kwasm.ast.Identifier.Label(token.value)
            kwasm.ast.Identifier.TypeDef::class -> kwasm.ast.Identifier.TypeDef(token.value)
            else -> throw ParseException("Invalid identifier type expected", token.context)
        }

        ParseResult(Index.ByLabel(id as T), 1)
    }
    else -> throw ParseException("Expected index", token.context)
}

/**
 * Parses up to [max] [Index]es from the receiving [List] of [Token]s starting at [fromIndex].
 * If no indices are found, returns a [ParseResult] with an empty [AstNodeList].
 *
 * @throws ParseException if fewer than [min] [Index]es are found.
 */
inline fun <reified T : kwasm.ast.Identifier> List<Token>.parseIndices(
    fromIndex: Int,
    min: Int = 0,
    max: Int = Int.MAX_VALUE
): ParseResult<AstNodeList<out Index<T>>> {
    var indicesFound = 0
    var tokensRead = 0
    val indices = mutableListOf<Index<T>>()

    while (indicesFound < max) {
        try {
            val index = parseIndex<T>(fromIndex + tokensRead)
            tokensRead += index.parseLength
            indicesFound++
        } catch (e: ParseException) { break }
    }

    if (indices.size < min) {
        throw ParseException(
            "Expected at least $min indices, found ${indices.size}",
            this[fromIndex].context
        )
    }

    return ParseResult(AstNodeList(indices), tokensRead)
}

