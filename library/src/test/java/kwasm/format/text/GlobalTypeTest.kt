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
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GlobalTypeTest {
    @Test
    fun parseValidGlobalType_NonMutable() {
        val expectedValuetype = ValueTypeEnum.I32
        val actual = Type.GlobalType("i32", null)
        Truth.assertThat(actual.value.valueTypeEnum).isEqualTo(expectedValuetype)
        Truth.assertThat(actual.value.mutable).isFalse()
    }

    @Test
    fun parseValidResultType_Mutable() {
        val expectedValuetype = ValueTypeEnum.I32
        val actual = Type.GlobalType("(mut i32)", null)
        Truth.assertThat(actual.value.valueTypeEnum).isEqualTo(expectedValuetype)
        Truth.assertThat(actual.value.mutable).isTrue()
    }

    @Test
    fun parseInvalidResultType_DifferentFunction() {
        Assertions.assertThatThrownBy {
            Type.GlobalType("(foo blah)", null).value
        }.isInstanceOf(ParseException::class.java).hasMessageContaining("Invalid GlobalType syntax")
    }

    @Test
    fun parseInvalidResultType_BadValueType() {
        Assertions.assertThatThrownBy {
            Type.GlobalType("(mut blah)", null).value
        }.isInstanceOf(ParseException::class.java).hasMessageContaining("Invalid ValueType")
    }
}