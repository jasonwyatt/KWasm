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
import kwasm.format.ParseException
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ParamTest {
    @Test
    fun parseValidParam_withId() {
        val expected = Param(Identifier.Local("\$val1"), ValueType.I32)
        val actual = Type.Param("(param \$val1 i32)", null)
        Truth.assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun parseValidParam_withoutId() {
        val expected = Param(null, ValueType.I32)
        val actual = Type.Param("(param i32)", null)
        Truth.assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun parseInvalidResultType_DifferentFunction() {
        Assertions.assertThatThrownBy {
            Type.Param("(foo blah)", null).value
        }.isInstanceOf(ParseException::class.java).hasMessageContaining("Invalid Param syntax")
    }

    @Test
    fun parseInvalidResultType_MissingValueType() {
        Assertions.assertThatThrownBy {
            Type.Param("(param \$val1)", null).value
        }.isInstanceOf(ParseException::class.java).hasMessageContaining("Invalid ValueType")
    }
}