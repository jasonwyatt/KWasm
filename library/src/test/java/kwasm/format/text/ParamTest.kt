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
import kwasm.ast.Identifier
import kwasm.ast.Param
import kwasm.ast.ValueType
import kwasm.ast.ValueTypeEnum
import kwasm.format.ParseContext
import kwasm.format.ParseException
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ParamTest {

    private val context = ParseContext("ParamTest.wasm", 1, 1)
    private val tokenizer = Tokenizer()

    @Test
    fun parseValidParam_withId() {
        val expected = ParseResult(Param(Identifier.Local("\$val1"), ValueType(ValueTypeEnum.I32)), 5)
        val actual = tokenizer.tokenize("(param \$val1 i32)", context).parseParam(0)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseValidParam_withoutId() {
        val expected = ParseResult(Param(null, ValueType(ValueTypeEnum.I32)), 4)
        val actual = tokenizer.tokenize("(param i32)", context).parseParam(0)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseInvalidResultType_DifferentFunction() {
        val exception = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(foo blah)", context).parseParam(0)
        }
        assertThat(exception).hasMessageThat().contains("Invalid Param: Expecting \"param\"")
    }

    @Test
    fun parseInvalidResultType_MissingValueType() {
        val exception = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(param \$val1)", context).parseParam(0)
        }
        assertThat(exception).hasMessageThat().contains("Invalid ValueType: Expecting keyword token")
    }
}