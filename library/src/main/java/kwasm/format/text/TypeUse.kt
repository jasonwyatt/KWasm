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

import kwasm.ast.TypeUse
import kwasm.format.text.token.Token

/**
 * Parses a [TypeUse] out of the [Token] [List] according to the following grammar from
 * [the docs](https://webassembly.github.io/spec/core/text/modules.html#text-typeuse), but showing
 * the format-only. For the required runtime checks are see [TypeUse]:
 *
 * ```
 *   typeuse(I) ::=
 *          empty                                                   => TypeUse(null)
 *          (t_1:param)* (t_2:result)*                              => TypeUse(x, [t_1], [t_2])
 *          ‘(’ ‘type’ x:typeidx(I) ‘)’                             => TypeUse(x)
 *          ‘(’ ‘type’ x:typeidx(I) ‘)’ (t_1:param)* (t_2:result)*  => TypeUse(x, [t_1], [t_2])
 * ```
 */
fun List<Token>.parseTypeUse(atIndex: Int): ParseResult<TypeUse> {
    TODO("Implement me when Type, Param, and Result are merged.")
}
