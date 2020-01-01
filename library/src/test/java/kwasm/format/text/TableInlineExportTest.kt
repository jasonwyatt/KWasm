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

package kwasm.format.text

import com.google.common.truth.Truth.assertThat
import kwasm.ast.ElementSegment
import kwasm.ast.ElementType
import kwasm.ast.Export
import kwasm.ast.ExportDescriptor
import kwasm.ast.Expression
import kwasm.ast.Identifier
import kwasm.ast.Import
import kwasm.ast.ImportDescriptor
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
import org.assertj.core.api.Assertions
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")
@RunWith(JUnit4::class)
class TableInlineExportTest {
    private val tokenizer = Tokenizer()
    private val context = ParseContext("TableInlineExportTest.wat")
    
    @Test
    fun parse_returnsNullIf_openingParenNotFound() {
        val result = tokenizer.tokenize("table $0 (export \"a\"))", context)
            .parseInlineTableExport(0)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNullIf_tableKeywordNotFound() {
        val result = tokenizer.tokenize("(nontable $0 (export \"a\"))", context)
            .parseInlineTableExport(0)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNullIf_exportOpeningParenNotFound() {
        val result = tokenizer.tokenize("(table $0 export \"a\"))", context)
            .parseInlineTableExport(0)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNullIf_exportKeywordNotFound() {
        val result = tokenizer.tokenize("(table $0 (nonexport \"a\"))", context)
            .parseInlineTableExport(0)
        assertThat(result).isNull()
    }

    @Test
    fun parse_throwsIf_exportNameNotFound() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(table $0 (export))", context)
                .parseInlineTableExport(0)
        }
    }

    @Test
    fun parse_throwsIf_exportClosureNotFound() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(table $0 (export \"a\"", context)
                .parseInlineTableExport(0)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parse_minimal() {
        val result = tokenizer.tokenize("(table (export \"a\"))", context)
            .parseInlineTableExport(0)
            ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(7)
        assertThat(result.astNode).containsExactly(
            Export(
                "a",
                ExportDescriptor.Table(
                    Index.ByIdentifier(Identifier.Table(null, null))
                )
            )
        ).inOrder()
    }

    @Test
    fun parse_simple() {
        val result = tokenizer.tokenize("(table $0 (export \"a\"))", context)
            .parseInlineTableExport(0)
            ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(8)
        assertThat(result.astNode).containsExactly(
            Export(
                "a",
                ExportDescriptor.Table(
                    Index.ByIdentifier(Identifier.Table("$0"))
                )
            )
        ).inOrder()
    }

    @Test
    fun parse_multipleExports() {
        val result = tokenizer.tokenize(
            "(table $0 (export \"a\") (export \"b\") (export \"c\"))",
            context
        ).parseInlineTableExport(0) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(16)
        assertThat(result.astNode).containsExactly(
            Export(
                "a",
                ExportDescriptor.Table(
                    Index.ByIdentifier(Identifier.Table("$0"))
                )
            ),
            Export(
                "b",
                ExportDescriptor.Table(
                    Index.ByIdentifier(Identifier.Table("$0"))
                )
            ),
            Export(
                "c",
                ExportDescriptor.Table(
                    Index.ByIdentifier(Identifier.Table("$0"))
                )
            )
        ).inOrder()
    }

    @Test
    fun parse_exportThenImport() {
        val result = tokenizer.tokenize(
            "(table $0 (export \"a\") (import \"b\" \"c\") 1 funcref)",
            context
        ).parseInlineTableExport(0) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(15)
        assertThat(result.astNode).containsExactly(
            Export(
                "a",
                ExportDescriptor.Table(
                    Index.ByIdentifier(Identifier.Table("$0"))
                )
            ),
            Import(
                "b",
                "c",
                ImportDescriptor.Table(
                    Identifier.Table("$0"),
                    TableType(
                        Limit(1u, UInt.MAX_VALUE),
                        ElementType.FunctionReference
                    )
                )
            )
        ).inOrder()
    }

    @Test
    fun parse_exportThenInlineElements() {
        val result = tokenizer.tokenize(
            "(table $0 (export \"a\") funcref (elem $1 $2))",
            context
        ).parseInlineTableExport(0) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(14)
        assertThat(result.astNode).containsExactly(
            Export(
                "a",
                ExportDescriptor.Table(
                    Index.ByIdentifier(Identifier.Table("$0"))
                )
            ),
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
    fun parse_multipleExportsThenInlineElements() {
        val result = tokenizer.tokenize(
            "(table $0 (export \"a\") (export \"b\") funcref (elem $1 $2))",
            context
        ).parseInlineTableExport(0) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(18)
        assertThat(result.astNode).containsExactly(
            Export(
                "a",
                ExportDescriptor.Table(
                    Index.ByIdentifier(Identifier.Table("$0"))
                )
            ),
            Export(
                "b",
                ExportDescriptor.Table(
                    Index.ByIdentifier(Identifier.Table("$0"))
                )
            ),
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