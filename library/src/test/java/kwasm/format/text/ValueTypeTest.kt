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
import kwasm.format.ParseContext
import kwasm.format.ParseException
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ValueTypeTest {

    private val context = ParseContext("ValueTypeTest.wasm", 1, 1)
    private val tokenizer = Tokenizer()

    @Test
    fun parseValidValueType() {
        var expected = ParseResult(kwasm.ast.ValueType(ValueTypeEnum.I32), 1)
        var actual = tokenizer.tokenize("i32", context).parseValueType(0)
        assertThat(actual).isEqualTo(expected)

        expected = ParseResult(kwasm.ast.ValueType(ValueTypeEnum.I64), 1)
        actual = tokenizer.tokenize("i64", context).parseValueType(0)
        assertThat(actual).isEqualTo(expected)

        expected = ParseResult(kwasm.ast.ValueType(ValueTypeEnum.F32), 1)
        actual = tokenizer.tokenize("f32", context).parseValueType(0)
        assertThat(actual).isEqualTo(expected)

        expected = ParseResult(kwasm.ast.ValueType(ValueTypeEnum.F64), 1)
        actual = tokenizer.tokenize("f64", context).parseValueType(0)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseInvalidValueType_ValidKeyword() {
        Assertions.assertThatThrownBy {
            tokenizer.tokenize("abc", context).parseValueType(0)
        }.isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Invalid ValueType: Expecting i32, i64, f32, or f64")
    }

    @Test
    fun parseInvalidValueType_invalidKeyword() {
        Assertions.assertThatThrownBy {
            tokenizer.tokenize("\$abc", context).parseValueType(0)
        }.isInstanceOf(ParseException::class.java).hasMessageContaining("Invalid ValueType: Expecting keyword token")
    }
}