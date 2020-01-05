/*
 * Copyright 2020 Google LLC
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

package kwasm.format.text.module

import com.google.common.truth.Truth.assertThat
import kwasm.ast.Identifier
import kwasm.ast.IntegerLiteral
import kwasm.ast.astNodeListOf
import kwasm.ast.instruction.ControlInstruction
import kwasm.ast.instruction.NumericConstantInstruction
import kwasm.ast.instruction.NumericInstruction
import kwasm.ast.module.Index
import kwasm.ast.module.Local
import kwasm.ast.module.TypeUse
import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.text.Tokenizer
import org.assertj.core.api.Assertions.fail
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class WasmFunctionTest {
    private val tokenizer = Tokenizer()
    private val context = ParseContext("WasmFunctionTest.wast")

    @Test
    fun parse_returnsNull_ifOpeningParenNotFound() {
        val result = tokenizer.tokenize("func)", context).parseWasmFunction(0)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNull_ifFuncKeywordDoesntFollowOpeningParen() {
        val result = tokenizer.tokenize("(notfunc)", context).parseWasmFunction(0)
        assertThat(result).isNull()
    }

    @Test
    fun throws_whenNoClosingParenFound() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(func", context).parseWasmFunction(0)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun throws_whenIdentifierIsNegativeInt() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(func -1)", context).parseWasmFunction(0)
        }
        assertThat(e).hasMessageThat().contains("Identifier must not be negative")
    }

    @Test
    fun parse_parsesMinimal() {
        val result = tokenizer.tokenize("(func)", context).parseWasmFunction(0)
            ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(3)
        assertThat(result.astNode.id).isEqualTo(Identifier.Function(null, null))
        assertThat(result.astNode.typeUse).isEqualTo(
            TypeUse(null, astNodeListOf(), astNodeListOf())
        )
        assertThat(result.astNode.locals).isEmpty()
        assertThat(result.astNode.instructions).isEmpty()
    }

    @Test
    fun parse_parsesIdOnly() {
        val result = tokenizer.tokenize("(func $0)", context).parseWasmFunction(0)
            ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(4)
        assertThat(result.astNode.id).isEqualTo(Identifier.Function("$0"))
        assertThat(result.astNode.typeUse).isEqualTo(
            TypeUse(null, astNodeListOf(), astNodeListOf())
        )
        assertThat(result.astNode.locals).isEmpty()
        assertThat(result.astNode.instructions).isEmpty()
    }

    @Test
    fun parse_parsesTypeUseOnly() {
        val result = tokenizer.tokenize("(func (type $0))", context).parseWasmFunction(0)
            ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(7)
        assertThat(result.astNode.id).isEqualTo(Identifier.Function(null, null))
        assertThat(result.astNode.typeUse).isEqualTo(
            TypeUse(
                Index.ByIdentifier(Identifier.Type("$0")),
                astNodeListOf(),
                astNodeListOf()
            )
        )
        assertThat(result.astNode.locals).isEmpty()
        assertThat(result.astNode.instructions).isEmpty()
    }

    @Test
    fun parse_parsesLocalsOnly() {
        val result = tokenizer.tokenize("(func (local) (local))", context).parseWasmFunction(0)
            ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(9)
        assertThat(result.astNode.id).isEqualTo(Identifier.Function(null, null))
        assertThat(result.astNode.typeUse).isEqualTo(
            TypeUse(
                null,
                astNodeListOf(),
                astNodeListOf()
            )
        )
        assertThat(result.astNode.locals).containsExactly(
            Local(null, null),
            Local(null, null)
        )
        assertThat(result.astNode.instructions).isEmpty()
    }

    @Test
    fun parse_parsesInstructionsOnly() {
        val result = tokenizer.tokenize("(func (unreachable))", context).parseWasmFunction(0)
            ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(6)
        assertThat(result.astNode.id).isEqualTo(Identifier.Function(null, null))
        assertThat(result.astNode.typeUse).isEqualTo(
            TypeUse(
                null,
                astNodeListOf(),
                astNodeListOf()
            )
        )
        assertThat(result.astNode.locals).isEmpty()
        assertThat(result.astNode.instructions).containsExactly(
            ControlInstruction.Unreachable
        )
    }

    @Test
    fun parse_parsesTheWholeShebang() {
        val result = tokenizer.tokenize(
            """
            (func $0 (type $1) (local) (local)
                (i32.add
                    (i32.const 1)
                    (i32.const 2)
                )
            )
            """.trimIndent(),
            context
        ).parseWasmFunction(0) ?: fail("Expected a result")

        assertThat(result.parseLength).isEqualTo(25)
        assertThat(result.astNode.id).isEqualTo(Identifier.Function("$0"))
        assertThat(result.astNode.typeUse).isEqualTo(
            TypeUse(
                Index.ByIdentifier(Identifier.Type("$1")),
                astNodeListOf(),
                astNodeListOf()
            )
        )
        assertThat(result.astNode.locals).containsExactly(
            Local(null, null),
            Local(null, null)
        ).inOrder()
        assertThat(result.astNode.instructions).containsExactly(
            NumericConstantInstruction.I32(IntegerLiteral.S32(1)),
            NumericConstantInstruction.I32(IntegerLiteral.S32(2)),
            NumericInstruction.I32Add
        ).inOrder()
    }
}
