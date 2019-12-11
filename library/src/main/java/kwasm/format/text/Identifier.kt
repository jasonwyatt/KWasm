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
import kwasm.format.ParseContext
import kwasm.format.ParseException

/**
 * From [the docs](https://webassembly.github.io/spec/core/text/values.html#text-id):
 *
 * Indices can be given in both numeric and symbolic form. Symbolic identifiers that stand in lieu
 * of indices start with `$`, followed by any sequence of printable ASCII characters that does not
 * contain a space, quotation mark, comma, semicolon, or bracket.
 *
 * ```
 *   id     ::= '$' idchar+
 *   idchar ::= '0', '1', ..., '9'
 *              'A', 'B', ..., 'Z'
 *              'a', 'b', ..., 'z'
 *              '!', '#', '$', '%', '&', ''', '*', '+', '-', '.', '/'
 *              ':', '<', '=', '>', '?', '@', '\', '^', '_', '`', '|', '~'
 * ```
 */
class Identifier(
    private val sequence: CharSequence,
    val context: ParseContext? = null
) {
    val value: String by lazy {
        if (sequence.first() != '$') throw ParseException("Identifier must begin with $", context)

        if (!ID_PATTERN.get().matches(sequence)) {
            throw ParseException("Invalid identifier: $sequence", context)
        }

        "$sequence"
    }

    /** Gets an instance of a [kwasm.ast.Identifier] based on the [value]. */
    inline fun <reified IdentifierType : Identifier> getAstValue(): IdentifierType =
        when (IdentifierType::class) {
            Identifier.Type::class -> Identifier.Type(stringRepr = value)
            Identifier.Function::class -> Identifier.Function(stringRepr = value)
            Identifier.Global::class -> Identifier.Global(stringRepr = value)
            Identifier.Label::class -> Identifier.Label(stringRepr = value)
            Identifier.Local::class -> Identifier.Local(stringRepr = value)
            Identifier.Memory::class -> Identifier.Memory(stringRepr = value)
            Identifier.Table::class -> Identifier.Table(stringRepr = value)
            else -> throw ParseException(
                "Unsupported AST Identifier type: ${IdentifierType::class.java}",
                context
            )
        } as IdentifierType

    companion object {
        val ID_PATTERN = object : ThreadLocal<Regex>() {
            override fun initialValue(): Regex =
                "\\\$[0-9a-zA-Z!#$%&'*+-./:<=>?@^_`|~\\\\]+".toRegex()
        }
    }
}
