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

import kwasm.ast.ValueType
import kwasm.ast.ValueTypeEnum
import kwasm.format.ParseException
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Token


fun List<Token>.parseValueType(currentIndex: Int): ParseResult<ValueType> {
    val valueTypeToken = this[currentIndex]
    if (valueTypeToken !is Keyword) {
        throw ParseException("Invalid ValueType: Expecting keyword token", valueTypeToken.context)
    }
    val valueType: ValueTypeEnum

    valueType = when (valueTypeToken.value) {
        "i32" -> ValueTypeEnum.I32
        "i64" -> ValueTypeEnum.I64
        "f32" -> ValueTypeEnum.F32
        "f64" -> ValueTypeEnum.F64
        else -> throw ParseException("Invalid ValueType: Expecting i32, i64, f32, or f64", valueTypeToken.context)
    }
    return ParseResult(ValueType(valueType), 1)

}