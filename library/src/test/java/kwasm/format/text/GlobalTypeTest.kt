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
import kwasm.ast.GlobalType
import kwasm.ast.ValueType
import kwasm.ast.ValueTypeEnum
import kwasm.format.ParseException
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Paren
import kwasm.format.text.token.Token
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GlobalTypeTest {
    @Test
    fun parseValidGlobalType_NonMutable() {
        val expectedValuetype = ParseResult(GlobalType(ValueType(ValueTypeEnum.I32), false), 1)
        val actual = listOf<Token>(Keyword("i32", null)).parseGlobalType(0)
        Truth.assertThat(actual).isEqualTo(expectedValuetype)
    }

    @Test
    fun parseValidResultType_Mutable() {
        val expectedValuetype = ParseResult(GlobalType(ValueType(ValueTypeEnum.I32), true), 4)
        val actual = listOf(
            Paren.Open(), Keyword("mut"),
            Keyword("i32"), Paren.Closed()
        ).parseGlobalType(0)
        Truth.assertThat(actual).isEqualTo(expectedValuetype)
    }

    @Test
    fun parseInvalidResultType_DifferentFunction() {
        Assertions.assertThatThrownBy {
            listOf(
                Paren.Open(), Keyword("foo"),
                Keyword("bar"), Paren.Closed()
            ).parseGlobalType(0)
        }.isInstanceOf(ParseException::class.java).hasMessageContaining("Invalid GlobalType: Expecting mut token")
    }

    @Test
    fun parseInvalidResultType_BadValueType() {
        Assertions.assertThatThrownBy {
            listOf(
                Paren.Open(), Keyword("mut"),
                Keyword("blah"), Paren.Closed()
            ).parseGlobalType(0)
        }.isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Invalid ValueType: Expecting i32, i64, f32, or f64")
    }
}