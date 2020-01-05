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
import kwasm.ast.instruction.Expression
import kwasm.ast.Identifier
import kwasm.ast.module.Index
import kwasm.ast.IntegerLiteral
import kwasm.ast.instruction.NumericConstantInstruction
import kwasm.ast.module.Offset
import kwasm.ast.astNodeListOf
import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.text.Tokenizer
import org.assertj.core.api.Assertions.fail
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")
@RunWith(JUnit4::class)
class DataSegmentTest {
    private val tokenizer = Tokenizer()
    private val context = ParseContext("DataSegmentTest.wast")

    @Test
    fun parse_returnsNull_ifStartToken_isNotOpenParen() {
        val result = tokenizer.tokenize("data $0 (offset i32.const 0x44) \"test\")", context)
            .parseDataSegment(0)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNull_ifTokenAfterOpeningParen_isNotDataKeyword() {
        val result = tokenizer.tokenize("(notdata $0 (offset i32.const 0x44) \"test\")", context)
            .parseDataSegment(0)
        assertThat(result).isNull()
    }

    @Test
    fun throws_whenMemoryIndex_doesntFollow_dataKeyword() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(data \"test\" (offset i32.const 0x44) \"test\")")
                .parseDataSegment(0)
        }
    }

    @Test
    fun throws_whenOffset_doesntFollow_memIndex() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(data offset i32.const 0x44) \"test\")").parseDataSegment(0)
        }

        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(data (notoffset i32.const 0x44) \"test\")")
                .parseDataSegment(0)
        }

        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(data \"test\")")
                .parseDataSegment(0)
        }
    }

    @Test
    fun throws_whenNotClosedWithParen() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(data (offset i32.const 0x44) \"test\"")
                .parseDataSegment(0)
        }
    }

    @Test
    fun parses_withEmptyMemIndex() {
        val result = tokenizer.tokenize("(data (offset i32.const 0) \"test\")")
            .parseDataSegment(0) ?: fail("Expected data segment")
        assertThat(result.parseLength).isEqualTo(9)
        assertThat(result.astNode.init).isEqualTo("test".toByteArray(Charsets.UTF_8))
        assertThat(result.astNode.memoryIndex).isEqualTo(Index.ByInt(0))
        assertThat(result.astNode.offset).isEqualTo(
            Offset(
                Expression(
                    astNodeListOf(NumericConstantInstruction.I32(IntegerLiteral.S32(0)))
                )
            )
        )
    }

    @Test
    fun parses_withoutOffsetKeyword() {
        val result = tokenizer.tokenize("(data $1 (i32.const 0) \"test\")")
            .parseDataSegment(0) ?: fail("Expected data segment")
        assertThat(result.parseLength).isEqualTo(9)
        assertThat(result.astNode.init).isEqualTo("test".toByteArray(Charsets.UTF_8))
        assertThat(result.astNode.memoryIndex)
            .isEqualTo(Index.ByIdentifier(Identifier.Memory("$1")))
        assertThat(result.astNode.offset).isEqualTo(
            Offset(
                Expression(
                    astNodeListOf(NumericConstantInstruction.I32(IntegerLiteral.S32(0)))
                )
            )
        )
    }

    @Test
    fun parses_withoutInitStrings() {
        val result = tokenizer.tokenize("(data $1 (i32.const 0))")
            .parseDataSegment(0) ?: fail("Expected data segment")
        assertThat(result.parseLength).isEqualTo(8)
        assertThat(result.astNode.init).isEqualTo(ByteArray(0))
        assertThat(result.astNode.memoryIndex)
            .isEqualTo(Index.ByIdentifier(Identifier.Memory("$1")))
        assertThat(result.astNode.offset).isEqualTo(
            Offset(
                Expression(
                    astNodeListOf(NumericConstantInstruction.I32(IntegerLiteral.S32(0)))
                )
            )
        )
    }

    @Test
    fun parses_minimal() {
        val result = tokenizer.tokenize("(data (i32.const 0))")
            .parseDataSegment(0) ?: fail("Expected data segment")
        assertThat(result.parseLength).isEqualTo(7)
        assertThat(result.astNode.init).isEqualTo(ByteArray(0))
        assertThat(result.astNode.memoryIndex)
            .isEqualTo(Index.ByInt(0))
        assertThat(result.astNode.offset).isEqualTo(
            Offset(
                Expression(
                    astNodeListOf(NumericConstantInstruction.I32(IntegerLiteral.S32(0)))
                )
            )
        )
    }

    @Test
    fun parses_multipleStrings() {
        val result = tokenizer.tokenize("(data (i32.const 0) \"a\" \"b\" \"c\")")
            .parseDataSegment(0) ?: fail("Expected data segment")
        assertThat(result.parseLength).isEqualTo(10)
        assertThat(result.astNode.init).isEqualTo("abc".toByteArray(Charsets.UTF_8))
        assertThat(result.astNode.memoryIndex)
            .isEqualTo(Index.ByInt(0))
        assertThat(result.astNode.offset).isEqualTo(
            Offset(
                Expression(
                    astNodeListOf(NumericConstantInstruction.I32(IntegerLiteral.S32(0)))
                )
            )
        )
    }
}