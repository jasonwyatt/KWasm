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

import kwasm.ast.TableType
import kwasm.format.ParseException
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Token

/**
 * From [the spec](https://webassembly.github.io/spec/core/text/types.html#table-types):
 * ```
 *   tabletype ::= lim:limits  et:elemtype => lim et
 *   elemtype  ::= ‘funcref’               => funcref
 * ```
 */
fun List<Token>.parseTableType(startingIndex: Int): ParseResult<TableType> {
    val limits = this.parseLimits(startingIndex)
    val funcrefIndex = startingIndex + limits.parseLength
    if (funcrefIndex >= this.size) throw ParseException(
        "Too few arguments",
        this[startingIndex].context
    )
    val funcrefToken = this[funcrefIndex] as? Keyword ?: throw ParseException(
        "Expected keyword",
        this[funcrefIndex].context
    )
    if (funcrefToken.value != "funcref") throw ParseException(
        "Expected 'funcref' found '${funcrefToken.value}'",
        funcrefToken.context
    )
    return ParseResult(TableType(limits.astNode), limits.parseLength + 1)
}