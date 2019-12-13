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
import kwasm.format.ParseException
import kwasm.format.text.token.Identifier
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Paren
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FunctionTypeTest {
    @Test
    fun parseValidFunctionType_OneParamOneReturn() {
        val onlyParam = listOf(Param(kwasm.ast.Identifier.Local("\$val1"), ValueType(ValueTypeEnum.I32)))
        val onlyReturnType = listOf(Result(ValueType(ValueTypeEnum.I32)))
        val expected = ParseResult(FunctionType(onlyParam, onlyReturnType), 12)
        val actual = listOf(
            Paren.Open(), Keyword("func"), Paren.Open(),
            Keyword("param"), Identifier("\$val1"), Keyword("i32"),
            Paren.Closed(), Paren.Open(), Keyword("result"), Keyword("i32"),
            Paren.Closed(), Paren.Closed()
        ).parseFunctionType(0)
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
        val actual = listOf(
            Paren.Open(), Keyword("func"), Paren.Open(),
            Keyword("param"), Identifier("\$val1"), Keyword("i32"),
            Paren.Closed(), Paren.Open(), Keyword("param"), Identifier("\$val2"),
            Keyword("i64"), Paren.Closed(), Paren.Open(), Keyword("result"),
            Keyword("i32"), Paren.Closed(), Paren.Open(), Keyword("result"),
            Keyword("i64"), Paren.Closed(), Paren.Closed()
        ).parseFunctionType(0)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseValidFunctionType_SingleParamVoidReturn() {
        val onlyParam = listOf(Param(kwasm.ast.Identifier.Local("\$val1"), ValueType(ValueTypeEnum.I32)))
        val expected = ParseResult(FunctionType(onlyParam, listOf()), 8)
        val actual = listOf(
            Paren.Open(), Keyword("func"), Paren.Open(),
            Keyword("param"), Identifier("\$val1"), Keyword("i32"),
            Paren.Closed(), Paren.Closed()
        ).parseFunctionType(0)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseValidFunctionType_NoParamSingleReturn() {
        val onlyReturnType = listOf(Result(ValueType(ValueTypeEnum.I32)))
        val expected = ParseResult(FunctionType(listOf(), onlyReturnType), 7)
        val actual = listOf(
            Paren.Open(), Keyword("func"), Paren.Open(),
            Keyword("result"), Keyword("i32"),
            Paren.Closed(), Paren.Closed()
        ).parseFunctionType(0)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseValidFunctionType_NoParamNoReturn() {
        val expected = ParseResult(FunctionType(listOf(), listOf()), 3)
        val actual = listOf(
            Paren.Open(), Keyword("func"),
            Paren.Closed()
        ).parseFunctionType(0)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseInvalidFunctionType_FlippedParams() {
        Assertions.assertThatThrownBy {
            listOf(
                Paren.Open(), Keyword("func"), Paren.Open(),
                Keyword("result"), Keyword("i32"),
                Paren.Closed(), Paren.Open(), Keyword("param"),
                Identifier("\$val1"), Keyword("i32"),
                Paren.Closed(), Paren.Closed()
            ).parseFunctionType(0)
        }.isInstanceOf(ParseException::class.java).hasMessageContaining("Invalid FunctionType: Expecting result token")
    }
}