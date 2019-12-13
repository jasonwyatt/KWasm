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

import com.google.common.truth.Truth.assertThat
import kwasm.ast.ValueTypeEnum
import kwasm.format.ParseException
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Reserved
import kwasm.format.text.token.Token
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ValueTypeTest {
    @Test
    fun parseValidValueType() {
        var expected = ParseResult(kwasm.ast.ValueType(ValueTypeEnum.I32), 1)
        var actual = listOf<Token>(Keyword("i32", null)).parseValueType(0)
        assertThat(actual).isEqualTo(expected)

        expected = ParseResult(kwasm.ast.ValueType(ValueTypeEnum.I64), 1)
        actual = listOf<Token>(Keyword("i64", null)).parseValueType(0)
        assertThat(actual).isEqualTo(expected)

        expected = ParseResult(kwasm.ast.ValueType(ValueTypeEnum.F32), 1)
        actual = listOf<Token>(Keyword("f32", null)).parseValueType(0)
        assertThat(actual).isEqualTo(expected)

        expected = ParseResult(kwasm.ast.ValueType(ValueTypeEnum.F64), 1)
        actual = listOf<Token>(Keyword("f64", null)).parseValueType(0)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseInvalidValueType_ValidKeyword() {
        Assertions.assertThatThrownBy {
            listOf<Token>(Keyword("abc", null)).parseValueType(0)
        }.isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Invalid ValueType: Expecting i32, i64, f32, or f64")
    }

    @Test
    fun parseInvalidValueType_invalidKeyword() {
        Assertions.assertThatThrownBy {
            listOf<Token>(Reserved("\$abc", null)).parseValueType(0)
        }.isInstanceOf(ParseException::class.java).hasMessageContaining("Invalid ValueType: Expecting keyword token")
    }
}