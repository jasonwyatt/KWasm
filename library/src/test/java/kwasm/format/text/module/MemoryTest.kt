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
import kwasm.ast.instruction.Expression
import kwasm.ast.instruction.NumericConstantInstruction
import kwasm.ast.module.DataSegment
import kwasm.ast.module.Index
import kwasm.ast.module.Memory
import kwasm.ast.module.Offset
import kwasm.ast.type.Limits
import kwasm.ast.type.MemoryType
import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.text.Tokenizer
import org.assertj.core.api.Assertions
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")
class MemoryTest {
    private val tokenizer = Tokenizer()
    private val context = ParseContext("MemoryTest.wast")

    @Test
    fun parseMemoryBasic_returnsNull_ifOpenParenNotFound() {
        val result = tokenizer.tokenize("memory $0 0 1)", context)
            .parseMemoryBasic(0)
        assertThat(result).isNull()
    }

    @Test
    fun parseMemoryBasic_returnsNull_ifMemoryKeywordNotFound() {
        val result = tokenizer.tokenize("(non-memory $0 0 1)", context)
            .parseMemoryBasic(0)
        assertThat(result).isNull()
    }

    @Test
    fun parseMemoryBasic_returnsNull_ifMemoryTypeCouldntBeFound() {
        val result = tokenizer.tokenize("(memory $0)", context)
            .parseMemoryBasic(0)
        assertThat(result).isNull()
    }

    @Test
    fun parseMemoryBasic_throws_ifNoClosingParenFound() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(memory $0 0 1", context).parseMemoryBasic(0)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parseMemoryBasic() {
        val result = tokenizer.tokenize("(memory $0 0 1)", context)
            .parseMemoryBasic(0) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(6)
        assertThat(result.astNode.id).isEqualTo(Identifier.Memory("$0"))
        assertThat(result.astNode.memoryType).isEqualTo(
            MemoryType(
                Limits(0, 1)
            )
        )
    }

    @Test
    fun parseMemoryAndDataSegment_returnsNullIf_openingParen_notFound() {
        val result = tokenizer.tokenize("memory $0 (data \"first\" \"second\"))", context)
            .parseMemoryAndDataSegment(0)
        assertThat(result).isNull()
    }

    @Test
    fun parseMemoryAndDataSegment_returnsNullIf_memoryKeyword_notFound() {
        val result = tokenizer.tokenize("(non-memory $0 (data \"first\" \"second\"))", context)
            .parseMemoryAndDataSegment(0)
        assertThat(result).isNull()
    }

    @Test
    fun parseMemoryAndDataSegment_throwsIf_dataOpeningParen_notFound() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(memory $0 data \"first\" \"second\"))", context)
                .parseMemoryAndDataSegment(0)
        }
        assertThat(e).hasMessageThat().contains("Expected inline data segment")
    }

    @Test
    fun parseMemoryAndDataSegment_throwsIf_dataKeyword_notFound() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(memory $0 (non-data \"first\" \"second\"))", context)
                .parseMemoryAndDataSegment(0)
        }
        assertThat(e).hasMessageThat().contains("Expected inline data segment")
    }

    @Test
    fun parseMemoryAndDataSegment_throwsIf_dataClosingParen_notFound() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(memory $0 (data \"first\" \"second\")", context)
                .parseMemoryAndDataSegment(0)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parseMemoryAndDataSegment_throwsIf_closingParen_notFound() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(memory $0 (data \"first\" \"second\"", context)
                .parseMemoryAndDataSegment(0)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parseMemoryAndDataSegment_minimal() {
        val result = tokenizer.tokenize("(memory (data))", context)
            .parseMemoryAndDataSegment(0) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(6)
        assertThat(result.astNode).containsExactly(
            Memory(
                Identifier.Memory(null, null),
                MemoryType(
                    Limits(0, 0)
                )
            ),
            DataSegment(
                Index.ByIdentifier(Identifier.Memory(null, null)),
                Offset(
                    Expression(
                        astNodeListOf(NumericConstantInstruction.I32(IntegerLiteral.S32(0)))
                    )
                ),
                "".toByteArray(Charsets.UTF_8)
            )
        ).inOrder()
    }

    @Test
    fun parseMemoryAndDataSegment() {
        val result = tokenizer.tokenize("(memory $0 (data \"first\" \"second\"))", context)
            .parseMemoryAndDataSegment(0) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(9)
        assertThat(result.astNode).containsExactly(
            Memory(
                Identifier.Memory("$0"),
                MemoryType(
                    Limits(1, 1)
                )
            ),
            DataSegment(
                Index.ByIdentifier(Identifier.Memory("$0")),
                Offset(
                    Expression(
                        astNodeListOf(NumericConstantInstruction.I32(IntegerLiteral.S32(0)))
                    )
                ),
                "firstsecond".toByteArray(Charsets.UTF_8)
            )
        ).inOrder()
    }

    @Test
    fun parseMemory_parsesPlain() {
        val result = tokenizer.tokenize("(memory $0 0 1)", context)
            .parseMemory(0) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(6)
        assertThat(result.astNode.size).isEqualTo(1)
        assertThat((result.astNode.first() as Memory).id).isEqualTo(Identifier.Memory("$0"))
        assertThat((result.astNode.first() as Memory).memoryType).isEqualTo(
            MemoryType(
                Limits(0, 1)
            )
        )
    }

    @Test
    fun parseTable_parsesWithElementSegment() {
        val result = tokenizer.tokenize("(memory $0 (data \"first\" \"second\"))", context)
            .parseMemory(0) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(9)
        assertThat(result.astNode).containsExactly(
            Memory(
                Identifier.Memory("$0"),
                MemoryType(
                    Limits(1, 1)
                )
            ),
            DataSegment(
                Index.ByIdentifier(Identifier.Memory("$0")),
                Offset(
                    Expression(
                        astNodeListOf(NumericConstantInstruction.I32(IntegerLiteral.S32(0)))
                    )
                ),
                "firstsecond".toByteArray(Charsets.UTF_8)
            )
        ).inOrder()
    }
}
