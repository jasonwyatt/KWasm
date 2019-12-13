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
import kwasm.ast.Param
import kwasm.ast.Result
import kwasm.ast.ValueType
import kwasm.ast.ValueTypeEnum
import kwasm.format.ParseContext
import kwasm.format.ParseException
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FunctionTypeTest {

    private val context = ParseContext("FunctionTypeTest.wasm", 1, 1)
    private val tokenizer = Tokenizer()

    @Test
    fun parseValidFunctionType_OneParamOneReturn() {
        val onlyParam = listOf(Param(kwasm.ast.Identifier.Local("\$val1"), ValueType(ValueTypeEnum.I32)))
        val onlyReturnType = listOf(Result(ValueType(ValueTypeEnum.I32)))
        val expected = ParseResult(FunctionType(onlyParam, onlyReturnType), 12)
        val actual = tokenizer.tokenize("(func (param \$val1 i32) (result i32))", context).parseFunctionType(0)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseValidFunctionType_MultiParamMultiReturn() {
        val params = listOf(
            Param(kwasm.ast.Identifier.Local("\$val1"), ValueType(ValueTypeEnum.I32)),
            Param(kwasm.ast.Identifier.Local("\$val2"), ValueType(ValueTypeEnum.I64))
        )
        val returnTypes = listOf(Result(ValueType(ValueTypeEnum.I32)), Result(ValueType(ValueTypeEnum.I64)))
        val expected = ParseResult(FunctionType(params, returnTypes), 21)
        val actual =
            tokenizer.tokenize("(func (param \$val1 i32) (param \$val2 i64) (result i32) (result i64))", context)
                .parseFunctionType(0)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseValidFunctionType_SingleParamVoidReturn() {
        val onlyParam = listOf(Param(kwasm.ast.Identifier.Local("\$val1"), ValueType(ValueTypeEnum.I32)))
        val expected = ParseResult(FunctionType(onlyParam, listOf()), 8)
        val actual = tokenizer.tokenize("(func (param \$val1 i32))", context).parseFunctionType(0)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseValidFunctionType_NoParamSingleReturn() {
        val onlyReturnType = listOf(Result(ValueType(ValueTypeEnum.I32)))
        val expected = ParseResult(FunctionType(listOf(), onlyReturnType), 7)
        val actual = tokenizer.tokenize("(func (result i32))", context).parseFunctionType(0)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseValidFunctionType_NoParamNoReturn() {
        val expected = ParseResult(FunctionType(listOf(), listOf()), 3)
        val actual = tokenizer.tokenize("(func)", context).parseFunctionType(0)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseInvalidFunctionType_FlippedParams() {
        Assertions.assertThatThrownBy {
            tokenizer.tokenize("(func (result i32) (param \$val1 i32))", context).parseFunctionType(0)
        }.isInstanceOf(ParseException::class.java).hasMessageContaining("Invalid FunctionType: Expecting \")\"")
    }

    @Test
    fun parseValueParamList() {
        val expected = listOf(Param(null, ValueType(ValueTypeEnum.I32)), Param(null, ValueType(ValueTypeEnum.I64)))
        val actual = tokenizer.tokenize("(param i32) (param i64)", context).parseParamList(0)
        assertThat(actual.first).isEqualTo(expected)
        assertThat(actual.second).isEqualTo(8)
    }

    @Test
    fun parseValueParamList_OnlyOneParam() {
        val expected = listOf(Param(null, ValueType(ValueTypeEnum.I32)))
        val actual = tokenizer.tokenize("(param i32) (result i64)", context).parseParamList(0)
        assertThat(actual.first).isEqualTo(expected)
        assertThat(actual.second).isEqualTo(4)
    }

    @Test
    fun parseValueResultList() {
        val expected = listOf(Result(ValueType(ValueTypeEnum.I32)), Result(ValueType(ValueTypeEnum.I64)))
        val actual = tokenizer.tokenize("(result i32) (result i64)", context).parseResultList(0)
        assertThat(actual.first).isEqualTo(expected)
        assertThat(actual.second).isEqualTo(8)
    }

    @Test
    fun parseValueResultList_OnlyOneResult() {
        val expected = listOf(Result(ValueType(ValueTypeEnum.I32)))
        val actual = tokenizer.tokenize("(result i32) (param i64)", context).parseResultList(0)
        assertThat(actual.first).isEqualTo(expected)
        assertThat(actual.second).isEqualTo(4)
    }
}