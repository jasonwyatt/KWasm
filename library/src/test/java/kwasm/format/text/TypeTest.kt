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
import kwasm.ast.ValueTypeEnum
import kwasm.ast.astNodeListOf
import kwasm.format.ParseContext
import kwasm.format.ParseException
import org.assertj.core.api.Assertions.fail
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TypeTest {
    private val tokenizer = Tokenizer()
    private val context = ParseContext("TypeTest.wat")

    @Test
    fun parses_type_withoutId() {
        val result = tokenizer.tokenize("(type (func (param $0 i32)))", context)
            .parseType(0) ?: fail("Expected a type")

        assertThat(result.parseLength).isEqualTo(11)
        assertThat(result.astNode.id).isNull()
        assertThat(result.astNode.functionType)
            .isEqualTo(
                FunctionType(
                    astNodeListOf(Param(Identifier.Local("$0"), ValueType(ValueTypeEnum.I32))),
                    astNodeListOf()
                )
            )
    }

    @Test
    fun parses_type_withId() {
        val result = tokenizer.tokenize("(type \$myId (func (param $0 i32)))", context)
            .parseType(0) ?: fail("Expected a type")

        assertThat(result.parseLength).isEqualTo(12)
        assertThat(result.astNode.id).isEqualTo(Identifier.Type("\$myId"))
        assertThat(result.astNode.functionType)
            .isEqualTo(
                FunctionType(
                    astNodeListOf(Param(Identifier.Local("$0"), ValueType(ValueTypeEnum.I32))),
                    astNodeListOf()
                )
            )
    }

    @Test
    fun throws_whenFuncType_notPresent() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(type \$myId)").parseType(0)
        }
        assertThat(e.parseContext?.lineNumber).isEqualTo(1)
        assertThat(e.parseContext?.column).isEqualTo(12)
    }

    @Test
    fun throws_whenClosingParen_notPresent() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(type \$myId (func (param \$0 i32))").parseType(0)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
        assertThat(e.parseContext?.lineNumber).isEqualTo(1)
        assertThat(e.parseContext?.column).isEqualTo(33)
    }

    @Test
    fun returnsNull_ifOpeningParen_notPresent() {
        val result = tokenizer.tokenize("type \$myId (func (param $0 i32))", context)
            .parseType(0)

        assertThat(result).isNull()
    }

    @Test
    fun returnsNull_ifTypeKeyword_doesntFollowOpeningParen() {
        val result = tokenizer.tokenize("(nontype \$myId (func (param $0 i32))", context)
            .parseType(0)

        assertThat(result).isNull()
    }
}