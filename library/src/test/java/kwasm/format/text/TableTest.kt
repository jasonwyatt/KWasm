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
import kwasm.ast.ElementSegment
import kwasm.ast.ElementType
import kwasm.ast.Expression
import kwasm.ast.Identifier
import kwasm.ast.Index
import kwasm.ast.IntegerLiteral
import kwasm.ast.Limit
import kwasm.ast.NumericConstantInstruction
import kwasm.ast.Offset
import kwasm.ast.Table
import kwasm.ast.TableType
import kwasm.ast.astNodeListOf
import kwasm.format.ParseContext
import kwasm.format.ParseException
import org.assertj.core.api.Assertions.fail
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")
@RunWith(JUnit4::class)
class TableTest {
    private val tokenizer = Tokenizer()
    private val context = ParseContext("TableTest.wast")

    @Test
    fun parseTableBasic_returnsNull_ifOpenParenNotFound() {
        val result = tokenizer.tokenize("table $0 0 1 funcref)", context)
            .parseTableBasic(0)
        assertThat(result).isNull()
    }

    @Test
    fun parseTableBasic_returnsNull_ifTableKeywordNotFound() {
        val result = tokenizer.tokenize("(non-table $0 0 1 funcref)", context)
            .parseTableBasic(0)
        assertThat(result).isNull()
    }

    @Test
    fun parseTableBasic_returnsNull_ifTableTypeCouldntBeFound() {
        val result = tokenizer.tokenize("(table $0)", context)
            .parseTableBasic(0)
        assertThat(result).isNull()
    }

    @Test
    fun parseTableBasic_returnsNull_ifTableTypeInvalid() {
        val result = tokenizer.tokenize("(table $0 0 1 nonfuncref)", context)
            .parseTableBasic(0)
        assertThat(result).isNull()
    }

    @Test
    fun parseTableBasic_throws_ifNoClosingParenFound() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(table $0 0 1 funcref", context).parseTableBasic(0)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parseTableBasic() {
        val result = tokenizer.tokenize("(table $0 0 1 funcref)", context)
            .parseTableBasic(0) ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(7)
        assertThat(result.astNode.id).isEqualTo(Identifier.Table("$0"))
        assertThat(result.astNode.tableType).isEqualTo(
            TableType(
                Limit(0u, 1u),
                ElementType.FunctionReference
            )
        )
    }

    @Test
    fun parseTableAndElementSegment_returnsNullIf_openingParen_notFound() {
        val result = tokenizer.tokenize("table $0 funcref (elem $1 $2))", context)
            .parseTableAndElementSegment(0)
        assertThat(result).isNull()
    }

    @Test
    fun parseTableAndElementSegment_returnsNullIf_tableKeyword_notFound() {
        val result = tokenizer.tokenize("(non-table $0 funcref (elem $1 $2))", context)
            .parseTableAndElementSegment(0)
        assertThat(result).isNull()
    }

    @Test
    fun parseTableAndElementSegment_returnsNullIf_funcrefKeyword_notFound() {
        val result = tokenizer.tokenize("(table $0 nonfuncref (elem $1 $2))", context)
            .parseTableAndElementSegment(0)
        assertThat(result).isNull()
    }

    @Test
    fun parseTableAndElementSegment_throwsIf_elemOpeningParen_notFound() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(table $0 funcref elem $1 $2))", context)
                .parseTableAndElementSegment(0)
        }
        assertThat(e).hasMessageThat().contains("Expected inline element segment")
    }

    @Test
    fun parseTableAndElementSegment_throwsIf_elemKeyword_notFound() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(table $0 funcref (non-elem $1 $2))", context)
                .parseTableAndElementSegment(0)
        }
        assertThat(e).hasMessageThat().contains("Expected inline element segment")
    }

    @Test
    fun parseTableAndElementSegment_throwsIf_elemClosingParen_notFound() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(table $0 funcref (elem $1 $2)", context)
                .parseTableAndElementSegment(0)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parseTableAndElementSegment_throwsIf_closingParen_notFound() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(table $0 funcref (elem $1 $2", context)
                .parseTableAndElementSegment(0)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parseTableAndElementSegment_minimal() {
        val result = tokenizer.tokenize("(table funcref (elem))", context)
            .parseTableAndElementSegment(0) ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(7)
        assertThat(result.astNode).containsExactly(
            Table(
                Identifier.Table(null, null),
                TableType(
                    Limit(0u, 0u),
                    ElementType.FunctionReference
                )
            ),
            ElementSegment(
                Index.ByIdentifier(Identifier.Table(null, null)),
                Offset(
                    Expression(
                        astNodeListOf(NumericConstantInstruction.I32(IntegerLiteral.S32(0)))
                    )
                ),
                astNodeListOf()
            )
        ).inOrder()
    }

    @Test
    fun parseTableAndElementSegment() {
        val result = tokenizer.tokenize("(table $0 funcref (elem $1 $2))", context)
            .parseTableAndElementSegment(0) ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(10)
        assertThat(result.astNode).containsExactly(
            Table(
                Identifier.Table("$0"),
                TableType(
                    Limit(2u, 2u),
                    ElementType.FunctionReference
                )
            ),
            ElementSegment(
                Index.ByIdentifier(Identifier.Table("$0")),
                Offset(
                    Expression(
                        astNodeListOf(NumericConstantInstruction.I32(IntegerLiteral.S32(0)))
                    )
                ),
                astNodeListOf(
                    Index.ByIdentifier(Identifier.Function("$1")),
                    Index.ByIdentifier(Identifier.Function("$2"))
                )
            )
        ).inOrder()
    }

    @Test
    fun parseTable_parsesPlain() {
        val result = tokenizer.tokenize("(table $0 0 1 funcref)", context)
            .parseTable(0) ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(7)
        assertThat(result.astNode.size).isEqualTo(1)
        assertThat((result.astNode.first() as Table).id).isEqualTo(Identifier.Table("$0"))
        assertThat((result.astNode.first() as Table).tableType).isEqualTo(
            TableType(
                Limit(0u, 1u),
                ElementType.FunctionReference
            )
        )
    }

    @Test
    fun parseTable_parsesWithElementSegment() {
        val result = tokenizer.tokenize("(table $0 funcref (elem $1 $2))", context)
            .parseTable(0) ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(10)
        assertThat(result.astNode).containsExactly(
            Table(
                Identifier.Table("$0"),
                TableType(
                    Limit(2u, 2u),
                    ElementType.FunctionReference
                )
            ),
            ElementSegment(
                Index.ByIdentifier(Identifier.Table("$0")),
                Offset(
                    Expression(
                        astNodeListOf(NumericConstantInstruction.I32(IntegerLiteral.S32(0)))
                    )
                ),
                astNodeListOf(
                    Index.ByIdentifier(Identifier.Function("$1")),
                    Index.ByIdentifier(Identifier.Function("$2"))
                )
            )
        ).inOrder()
    }
}