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

import kwasm.ast.Memory
import kwasm.format.ParseException
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Paren
import kwasm.format.text.token.Token

/**
 * From [the spec](https://webassembly.github.io/spec/core/text/types.html#memory-types):
 * ```
 *   memtype ::= lim:limits => lim
 * ```
 */
@UseExperimental(ExperimentalUnsignedTypes::class)
fun List<Token>.parseMemoryType(startingIndex: Int): ParseResult<Memory> {
    this[startingIndex] as? Paren.Open ?: throw ParseException("Expected open paren", this[startingIndex].context)
    val keywordIndex = startingIndex + 1
    val keyword =
        this[keywordIndex] as? Keyword ?: throw ParseException("Expected keyword", this[keywordIndex].context)
    if (keyword.value != "memory") throw ParseException(
        "Expected 'memory' keyword but found ${keyword.value}",
        keyword.context
    )
    val limitsIndex = startingIndex + 2
    val limits = this.parseLimits(limitsIndex)
    this[limitsIndex + limits.parseLength] as? Paren.Closed ?: throw ParseException(
        "Expected Close Paren",
        this[limitsIndex + limits.parseLength].context
    )
    return ParseResult(Memory(limits.astNode), limits.parseLength + 3)
}
