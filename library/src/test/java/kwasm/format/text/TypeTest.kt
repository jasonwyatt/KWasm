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
import kwasm.ast.ValueType
import kwasm.format.ParseException
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TypeTest {
    @Test
    fun parseValidValueType(){
        var expected = ValueType.I32
        var actual = Type.ValueType("i32", null)
        assertThat(actual.value).isEqualTo(expected)

        expected = ValueType.I64
        actual = Type.ValueType("i64", null)
        assertThat(actual.value).isEqualTo(expected)

        expected = ValueType.F32
        actual = Type.ValueType("f32", null)
        assertThat(actual.value).isEqualTo(expected)

        expected = ValueType.F64
        actual = Type.ValueType("f64", null)
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun parseInvalidValueType(){
        Assertions.assertThatThrownBy {
            Type.ValueType("abc", null).value
        }.isInstanceOf(ParseException::class.java).hasMessageContaining("Invalid ValueType")
    }

    @Test
    fun parseValidResultType_Exists(){
        val expected = ValueType.I32
        val actual = Type.ResultType("(result i32)",null)
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun parseInvalidResultType_DifferentFunction(){
        val actual = Type.ResultType("(foo blah)",null)
        assertThat(actual.value).isEqualTo(null)
    }

    @Test
    fun parseInvalidResultType_BadValueType(){
        Assertions.assertThatThrownBy {
            Type.ResultType("(result blah)", null).value
        }.isInstanceOf(ParseException::class.java).hasMessageContaining("Invalid ValueType")
    }
}