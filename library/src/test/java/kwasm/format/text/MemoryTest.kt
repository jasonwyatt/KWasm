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
import kwasm.ast.DataSegment
import kwasm.ast.ElementSegment
import kwasm.ast.ElementType
import kwasm.ast.Expression
import kwasm.ast.Identifier
import kwasm.ast.Index
import kwasm.ast.IntegerLiteral
import kwasm.ast.Limit
import kwasm.ast.Memory
import kwasm.ast.MemoryType
import kwasm.ast.NumericConstantInstruction
import kwasm.ast.Offset
import kwasm.ast.Table
import kwasm.ast.TableType
import kwasm.ast.astNodeListOf
import kwasm.format.ParseContext
import kwasm.format.ParseException
import org.assertj.core.api.Assertions
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.Base64
import kotlin.random.Random

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
                Limit(0u, 1u)
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
                    Limit(0u, 0u)
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
                    Limit(1u, 1u)
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
                Limit(0u, 1u)
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
                    Limit(1u, 1u)
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