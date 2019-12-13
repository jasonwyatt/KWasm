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

import com.google.common.truth.Truth
import kwasm.ast.ValueTypeEnum
import kwasm.format.ParseException
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Paren
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ResultTypeTest {
    @Test
    fun parseValidResultType_Exists() {
        val expected = ParseResult(kwasm.ast.ResultType(kwasm.ast.Result(kwasm.ast.ValueType(ValueTypeEnum.I32))), 4)
        val actual = listOf(
            Paren.Open(), Keyword("result"),
            Keyword("i32"), Paren.Closed()
        ).parseResultType(0)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseInvalidResultType_DifferentFunction() {
        val expected = ParseResult(kwasm.ast.ResultType(null), 0)
        val actual = listOf(
            Paren.Open(), Keyword("foo"),
            Keyword("bar"), Paren.Closed()
        ).parseResultType(0)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseInvalidResultType_BadValueType() {
        Assertions.assertThatThrownBy {
            listOf(
                Paren.Open(), Keyword("result"),
                Keyword("blah"), Paren.Closed()
            ).parseResultType(0)
        }.isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Invalid ValueType: Expecting i32, i64, f32, or f64")
    }
}