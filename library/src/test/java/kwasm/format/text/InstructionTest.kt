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
import kwasm.format.ParseContext
import kwasm.format.ParseException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class InstructionTest {
    private val tokenizer = Tokenizer()
    private val context = ParseContext("InstructionTest.wat", 1, 1)

    @Test
    fun parsePlural_canParseEmpty_instructionList() {
        val parsed = tokenizer.tokenize("", context).parseInstructions(0)
        assertThat(parsed.parseLength).isEqualTo(0)
        assertThat(parsed.astNode).isEmpty()
    }

    @Test
    fun parsePlural_canParseEmpty_instructionList_whenNotInstruction() {
        val parsed = tokenizer.tokenize("end", context).parseInstructions(0)
        assertThat(parsed.parseLength).isEqualTo(0)
        assertThat(parsed.astNode).isEmpty()
    }

    @Test
    fun parsePlural_canParseSingle_instructionList() {
        val parsed = tokenizer.tokenize("unreachable", context).parseInstructions(0)
        assertThat(parsed.parseLength).isEqualTo(1)
        assertThat(parsed.astNode).hasSize(1)
        assertThat(parsed.astNode[0]).isEqualTo(ControlInstruction.Unreachable)
    }

    @Test
    fun parsePlural_canParse_multipleInstructions() {
        val parsed = tokenizer.tokenize(
            """
                unreachable
                nop
                return
            """.trimIndent(),
            context
        ).parseInstructions(0)

        assertThat(parsed.parseLength).isEqualTo(3)
        assertThat(parsed.astNode).hasSize(3)
        assertThat(parsed.astNode[0]).isEqualTo(ControlInstruction.Unreachable)
        assertThat(parsed.astNode[1]).isEqualTo(ControlInstruction.NoOp)
        assertThat(parsed.astNode[2]).isEqualTo(ControlInstruction.Return)
    }

    @Test
    fun parsePlural_throwsIf_fromIndexInvalid_withPositiveMin() {
        assertThatThrownBy {
            tokenizer.tokenize("", context).parseInstructions(0, min = 1)
        }.isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Expected at least 1 instruction, found 0")
    }

    @Test
    fun parsePlural_throwsIf_insufficientInstructionsFound() {
        assertThatThrownBy {
            tokenizer.tokenize("""unreachable not-an-instruction nop""", context)
                .parseInstructions(0, min = 2)
        }.isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Expected at least 2 instructions, found 1")
    }

    @Test
    fun parsePlural_parsesFoldedInstruction_intoList() {
        val result = tokenizer.tokenize(
            """(br_if $3 (br_if $1 (br_if $0)) (br_if $2))""",
            context
        ).parseInstructions(0)
        assertThat(result.astNode).hasSize(4)
        repeat(4) {
            assertThat(
                result.astNode[it]
            ).isEqualTo(ControlInstruction.BreakIf(Index.ByIdentifier(Identifier.Label("\$$it"))))
        }
        assertThat(result.parseLength).isEqualTo(16)
    }
}
