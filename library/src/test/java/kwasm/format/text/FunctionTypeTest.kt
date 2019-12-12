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
import kwasm.ast.FunctionType
import kwasm.ast.Identifier
import kwasm.ast.Param
import kwasm.ast.ValueType
import kwasm.format.ParseException
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FunctionTypeTest {
    @Test
    fun parseValidFunctionType_OneParamOneReturn() {
        val onlyParam = listOf(Param(Identifier.Local("\$val1"), ValueType.I32))
        val onlyReturnType = listOf(ValueType.I32)
        val expected = FunctionType(onlyParam, onlyReturnType)
        val actual = Type.FunctionType("(func (param \$val1 i32) (result i32))")
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun parseValidFunctionType_MultiParamMultiReturn() {
        val params =
            listOf(Param(Identifier.Local("\$val1"), ValueType.I32), Param(Identifier.Local("\$val2"), ValueType.I64))
        val returnTypes = listOf(ValueType.I32, ValueType.I64)
        val expected = FunctionType(params, returnTypes)
        val actual = Type.FunctionType("(func (param \$val1 i32) (param \$val2 i64) (result i32) (result i64))")
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun parseValidFunctionType_SingleParamVoidReturn() {
        val params = listOf(Param(Identifier.Local("\$val1"), ValueType.I32))
        val returnTypes = listOf<ValueType>()
        val expected = FunctionType(params, returnTypes)
        val actual = Type.FunctionType("(func (param \$val1 i32))")
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun parseValidFunctionType_NoParamSingleReturn() {
        val params = listOf<Param>()
        val returnTypes = listOf(ValueType.I32)
        val expected = FunctionType(params, returnTypes)
        val actual = Type.FunctionType("(func (result i32))")
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun parseInvalidFunctionType_FlippedParams() {
        Assertions.assertThatThrownBy {
            Type.FunctionType("(func (result i32) (param \$val1 i32))").value
        }.isInstanceOf(ParseException::class.java).hasMessageContaining("Invalid FunctionType Syntax")
    }
}