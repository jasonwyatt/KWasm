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

import kwasm.ast.ParametricInstruction
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Token

/**
 * Parses a [ParametricInstruction] from the receiving [List] of [Token]s.
 *
 * See
 * [the docs](https://webassembly.github.io/spec/core/text/instructions.html#parametric-instructions)
 * for details.
 */
fun List<Token>.parseParametricInstruction(fromIndex: Int): ParseResult<ParametricInstruction>? {
    val keyword = getOrNull(fromIndex) as? Keyword ?: return null
    return PARAMETRIC_LOOKUP[keyword.value]?.let { ParseResult(it, 1) }
}

private val PARAMETRIC_LOOKUP = mapOf(
    "drop" to ParametricInstruction.Drop,
    "select" to ParametricInstruction.Select
)