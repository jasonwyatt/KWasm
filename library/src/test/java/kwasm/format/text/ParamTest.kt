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
import kwasm.ast.Identifier
import kwasm.ast.Param
import kwasm.ast.ValueType
import kwasm.ast.ValueTypeEnum
import kwasm.format.ParseException
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Paren
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ParamTest {
    @Test
    fun parseValidParam_withId() {
        val expected = ParseResult(Param(Identifier.Local("\$val1"), ValueType(ValueTypeEnum.I32)), 5)
        val actual = listOf(
            Paren.Open(), Keyword("param"),
            kwasm.format.text.token.Identifier("\$val1"),
            Keyword("i32"), Paren.Closed()
        ).parseParam(0)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseValidParam_withoutId() {
        val expected = ParseResult(Param(null, ValueType(ValueTypeEnum.I32)), 4)
        val actual = listOf(
            Paren.Open(), Keyword("param"),
            Keyword("i32"), Paren.Closed()
        ).parseParam(0)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseInvalidResultType_DifferentFunction() {
        Assertions.assertThatThrownBy {
            listOf(
                Paren.Open(), Keyword("foo"),
                Keyword("blah"), Paren.Closed()
            ).parseParam(0)
        }.isInstanceOf(ParseException::class.java).hasMessageContaining("Invalid Param: Expecting param token")
    }

    @Test
    fun parseInvalidResultType_MissingValueType() {
        Assertions.assertThatThrownBy {
            listOf(
                Paren.Open(), Keyword("param"),
                kwasm.format.text.token.Identifier("\$val1"), Paren.Closed()
            ).parseParam(0)
        }.isInstanceOf(ParseException::class.java).hasMessageContaining("Invalid ValueType: Expecting keyword token")
    }
}