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
import kwasm.ast.ControlInstruction
import kwasm.ast.Identifier
import kwasm.ast.Index
import kwasm.ast.Param
import kwasm.ast.Result
import kwasm.ast.TypeUse
import kwasm.ast.ValueTypeEnum
import kwasm.format.ParseContext
import kwasm.format.ParseException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.fail
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ControlInstructionTest {
    private val tokenizer = Tokenizer()
    private val context = ParseContext("ControlInstructionTest.wat", 1, 1)

    @Test
    fun parse_parsesUnreachable() {
        val parsed = tokenizer.tokenize("unreachable", context)
            .parseControlInstruction(0) ?: fail("Expected an instruction")
        assertThat(parsed.astNode).isInstanceOf(ControlInstruction.Unreachable::class.java)
        assertThat(parsed.parseLength).isEqualTo(1)
    }

    @Test
    fun parse_parsesNoOp() {
        val parsed = tokenizer.tokenize("nop", context)
            .parseControlInstruction(0) ?: fail("Expected an instruction")
        assertThat(parsed.astNode).isInstanceOf(ControlInstruction.NoOp::class.java)
        assertThat(parsed.parseLength).isEqualTo(1)
    }

    @Test
    fun parse_parsesReturn() {
        val parsed = tokenizer.tokenize("return", context)
            .parseControlInstruction(0) ?: fail("Expected an instruction")
        assertThat(parsed.astNode).isInstanceOf(ControlInstruction.Return::class.java)
        assertThat(parsed.parseLength).isEqualTo(1)
    }

    @Test
    fun parse_parsesBreak() {
        val parsed = tokenizer.tokenize("br $1", context)
            .parseControlInstruction(0) ?: fail("Expected an instruction")
        assertThat(parsed.parseLength).isEqualTo(2)
        assertThat(parsed.astNode).isInstanceOf(ControlInstruction.Break::class.java)
        val instruction = parsed.astNode as ControlInstruction.Break
        assertThat(instruction)
            .isEqualTo(
                ControlInstruction.Break(Index.ByIdentifier(Identifier.Label("$1")))
            )
    }

    @Test
    fun parse_throwsOn_break_withNoIndex() {
        assertThatThrownBy {
            tokenizer.tokenize("br ;; \$id").parseControlInstruction(0)
        }.isInstanceOf(ParseException::class.java).hasMessageContaining("Expected an index")

        assertThatThrownBy {
            tokenizer.tokenize("br oops-not-an-id").parseControlInstruction(0)
        }.isInstanceOf(ParseException::class.java).hasMessageContaining("Expected an index")
    }

    @Test
    fun parse_parsesBreakIf() {
        val parsed = tokenizer.tokenize("br_if $1", context)
            .parseControlInstruction(0) ?: fail("Expected an instruction")
        assertThat(parsed.parseLength).isEqualTo(2)
        assertThat(parsed.astNode).isInstanceOf(ControlInstruction.BreakIf::class.java)
        val instruction = parsed.astNode as ControlInstruction.BreakIf
        assertThat(instruction)
            .isEqualTo(
                ControlInstruction.BreakIf(Index.ByIdentifier(Identifier.Label("$1")))
            )
    }

    @Test
    fun parse_throwsOn_breakIf_withNoIndex() {
        assertThatThrownBy {
            tokenizer.tokenize("br_if;; \$id").parseControlInstruction(0)
        }.isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Expected an index")

        assertThatThrownBy {
            tokenizer.tokenize("br_if oops-not-an-id").parseControlInstruction(0)
        }.isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Expected an index")
    }

    @Test
    fun parse_parsesBreakTable_onlyWithDefault() {
        val parsed = tokenizer.tokenize("br_table $1", context)
            .parseControlInstruction(0) ?: fail("Expected an instruction")
        assertThat(parsed.parseLength).isEqualTo(2)
        assertThat(parsed.astNode).isInstanceOf(ControlInstruction.BreakTable::class.java)
        val instruction = parsed.astNode as ControlInstruction.BreakTable
        assertThat(instruction)
            .isEqualTo(
                ControlInstruction.BreakTable(
                    emptyList(),
                    Index.ByIdentifier(Identifier.Label("$1"))
                )
            )
    }

    @Test
    fun parse_throwsOn_breakTable_withNoIndex() {
        assertThatThrownBy {
            tokenizer.tokenize("br_table ;; \$id").parseControlInstruction(0)
        }.isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Expected at least 1 index")

        assertThatThrownBy {
            tokenizer.tokenize("br_table oops-not-an-id").parseControlInstruction(0)
        }.isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Expected at least 1 index")
    }

    @Test
    fun parse_parsesBreakTable_withTargets() {
        val parsed = tokenizer.tokenize("br_table $0 $1 $2", context)
            .parseControlInstruction(0) ?: fail("Expected an instruction")
        assertThat(parsed.parseLength).isEqualTo(4)
        assertThat(parsed.astNode).isInstanceOf(ControlInstruction.BreakTable::class.java)
        val instruction = parsed.astNode as ControlInstruction.BreakTable
        assertThat(instruction)
            .isEqualTo(
                ControlInstruction.BreakTable(
                    listOf(
                        Index.ByIdentifier(Identifier.Label("$0")),
                        Index.ByIdentifier(Identifier.Label("$1"))
                    ),
                    Index.ByIdentifier(Identifier.Label("$2"))
                )
            )
    }

    @Test
    fun parse_parsesCall() {
        val parsed = tokenizer.tokenize("call \$myFun", context)
            .parseControlInstruction(0) ?: fail("Expected an instruction")
        assertThat(parsed.astNode).isInstanceOf(ControlInstruction.Call::class.java)
        assertThat(parsed.parseLength).isEqualTo(2)

        val call = parsed.astNode as ControlInstruction.Call
        assertThat(call.functionIndex)
            .isEqualTo(Index.ByIdentifier(Identifier.Function("\$myFun")))
    }

    @Ignore("TODO: remove ignore annotation once ready.")
    @Test
    fun parse_parsesCallIndirect() {
        val parsed = tokenizer
            .tokenize("call_indirect (type \$myType) (param i32) (result f32)", context)
            .parseControlInstruction(0) ?: fail("Expected an instruction")
        assertThat(parsed.astNode).isInstanceOf(ControlInstruction.CallIndirect::class.java)
        assertThat(parsed.parseLength).isEqualTo(13)

        val call = parsed.astNode as ControlInstruction.CallIndirect
        assertThat(call.typeUse)
            .isEqualTo(
                TypeUse(
                    Index.ByIdentifier(Identifier.Type("\$myType")),
                    listOf(Param(null, ValueTypeEnum.I32)),
                    listOf(Result(ValueTypeEnum.F32))
                )
            )
    }

    @Test
    fun parse_returnsNullIfNotAnInstruction() {
        val parsed = tokenizer.tokenize("0x10", context)
            .parseControlInstruction(0)
        assertThat(parsed).isNull()
    }
}
