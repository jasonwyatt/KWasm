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
import kwasm.ast.Expression
import kwasm.ast.Global
import kwasm.ast.GlobalType
import kwasm.ast.Identifier
import kwasm.ast.IntegerLiteral
import kwasm.ast.NumericConstantInstruction
import kwasm.ast.NumericInstruction
import kwasm.ast.ValueType
import kwasm.ast.astNodeListOf
import kwasm.format.ParseContext
import kwasm.format.ParseException
import org.assertj.core.api.Assertions.fail
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GlobalTest {
    private val tokenizer = Tokenizer()
    private val context = ParseContext("GlobalTest.wast")

    @Test
    fun parseGlobal_returnsNullIf_openingParen_notFound() {
        val result = tokenizer.tokenize("global $0 i32 i32.const 0)", context)
            .parseGlobal(0)
        assertThat(result).isNull()
    }

    @Test
    fun parseGlobal_returnsNullIf_globalKeyword_notFound() {
        val result = tokenizer.tokenize("(nonglobal $0 i32 i32.const 0)", context)
            .parseGlobal(0)
        assertThat(result).isNull()
    }

    @Test
    fun parseGlobal_throwsIf_globalType_notFound() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(global $0 i32.const 0)", context)
                .parseGlobal(0)
        }
    }

    @Test
    fun parseGlobal_throwsIf_closingParen_notFound() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(global $0 i32 i32.const 0", context)
                .parseGlobal(0)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parseGlobal_minimal() {
        val result = tokenizer.tokenize("(global i32)", context).parseGlobal(0)
            ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(4)
        assertThat(result.astNode).isEqualTo(
            Global(
                Identifier.Global(null, null),
                GlobalType(ValueType.I32, false),
                Expression(astNodeListOf())
            )
        )
    }

    @Test
    fun parseGlobal_mutable() {
        val result = tokenizer.tokenize("(global (mut i32))", context).parseGlobal(0)
            ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(7)
        assertThat(result.astNode).isEqualTo(
            Global(
                Identifier.Global(null, null),
                GlobalType(ValueType.I32, true),
                Expression(astNodeListOf())
            )
        )
    }

    @Test
    fun parseGlobal() {
        val result = tokenizer.tokenize(
            "(global $0 (mut i32) (i32.add (i32.const 0) (i32.const 1)))",
            context
        ).parseGlobal(0) ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(19)
        assertThat(result.astNode).isEqualTo(
            Global(
                Identifier.Global("$0"),
                GlobalType(ValueType.I32, true),
                Expression(
                    astNodeListOf(
                        NumericConstantInstruction.I32(IntegerLiteral.S32(0)),
                        NumericConstantInstruction.I32(IntegerLiteral.S32(1)),
                        NumericInstruction.I32Add
                    )
                )
            )
        )
    }
}