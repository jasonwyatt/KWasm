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
import kwasm.ast.Identifier
import kwasm.ast.Identifier.Label
import kwasm.format.ParseException
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Reserved
import kwasm.format.text.token.Token

/**
 * Parses an [Identifier.Label] from the tokens.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/instructions.html#labels):
 *
 * ```
 *   labelI ::= v:id    => compose({ labels v }, I) (if v ∉ I.labels)
 *              ϵ       => compose({ labels (ϵ) }, I)
 * ```
 *
 * When the [Token] at [startIndex] is not an [kwasm.format.text.token.Identifier], this function:
 * * returns an *empty* identifier, if and only if the it is not a [Keyword] nor a [Reserved], or
 * * throws a [ParseException].
 *
 * **Note:** Composition will be left up to the interpreter.
 */
fun List<Token>.parseLabel(startIndex: Int): ParseResult<Label> {
    val idString = when (val identifier = getOrNull(startIndex)) {
        is kwasm.format.text.token.Identifier -> identifier.value
        is Reserved -> throw ParseException("Invalid identifier", identifier.context)
        else -> null
    }

    val parseLength = if (idString.isNullOrEmpty()) 0 else 1
    val identifierAst = Label(idString)

    return ParseResult(identifierAst, parseLength)
}

/**
 * Parses a non-empty [Label] from the tokens at the given [startIndex]. If there is no non-empty
 * [Label], returns `null`.
 *
 * See [parseLabel] for details of the grammar.
 */
fun List<Token>.parseNonEmptyLabel(startIndex: Int): ParseResult<Label>? =
    (this[startIndex] as? kwasm.format.text.token.Identifier)
        ?.let { ParseResult(Label(it.value), 1) }

/**
 * Parses a [List] of non-empty [Label]s from the tokens at the given [startIndex].  If there are
 * no non-empty [Label]s, an empty list is returned.
 */
fun List<Token>.parseNonEmptyLabelList(startIndex: Int): ParseResult<AstNodeList<Label>> {
    val result = mutableListOf<Label>()
    var tokensRead = 0

    while (true) {
        val parsedLabel = parseNonEmptyLabel(startIndex + tokensRead) ?: break
        result.add(parsedLabel.astNode)
        tokensRead += parsedLabel.parseLength
    }

    return ParseResult(AstNodeList(result), tokensRead)
}
