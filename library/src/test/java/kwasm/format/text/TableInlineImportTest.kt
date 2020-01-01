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
import kwasm.ast.ElementType
import kwasm.ast.Identifier
import kwasm.ast.Import
import kwasm.ast.ImportDescriptor
import kwasm.ast.Limit
import kwasm.ast.TableType
import kwasm.format.ParseContext
import kwasm.format.ParseException
import org.assertj.core.api.Assertions
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")
@RunWith(JUnit4::class)
class TableInlineImportTest {
    private val tokenizer = Tokenizer()
    private val context = ParseContext("TableInlineImportTest.wat")

    @Test
    fun parse_returnsNullIf_openingParenNotFound() {
        val result = tokenizer.tokenize("table $0 (import \"a\" \"b\") 0 1 funcref)", context)
            .parseInlineTableImport(0)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNullIf_tableKeywordNotFound() {
        val result = tokenizer.tokenize("(nontable $0 (import \"a\" \"b\") 0 1 funcref)", context)
            .parseInlineTableImport(0)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNullIf_importOpeningParenNotFound() {
        val result = tokenizer.tokenize("(table $0 import \"a\" \"b\") 0 1 funcref)", context)
            .parseInlineTableImport(0)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNullIf_importKeywordNotFound() {
        val result = tokenizer.tokenize("(table $0 (nonimport \"a\" \"b\") 0 1 funcref)", context)
            .parseInlineTableImport(0)
        assertThat(result).isNull()
    }

    @Test
    fun parse_throwsIf_moduleNameNotFound() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(table $0 (import) 0 1 funcref)", context)
                .parseInlineTableImport(0)
        }
    }

    @Test
    fun parse_throwsIf_tableNameNotFound() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(table $0 (import \"a\") 0 1 funcref)", context)
                .parseInlineTableImport(0)
        }
    }

    @Test
    fun parse_throwsIf_importNotClosed() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(table $0 (import \"a\" \"b\" 0 1 funcref)", context)
                .parseInlineTableImport(0)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parse_throwsIf_notClosed() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(table $0 (import \"a\" \"b\") 0 1 funcref", context)
                .parseInlineTableImport(0)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parse_minimal() {
        val result = tokenizer.tokenize("(table (import \"a\" \"b\") 1 funcref)", context)
            .parseInlineTableImport(0)
            ?: Assertions.fail("Expected a result")

        assertThat(result.parseLength).isEqualTo(10)
        assertThat(result.astNode).isEqualTo(
            Import(
                "a",
                "b",
                ImportDescriptor.Table(
                    Identifier.Table(null, null),
                    TableType(
                        Limit(1u, UInt.MAX_VALUE),
                        ElementType.FunctionReference
                    )
                )
            )
        )
    }

    @Test
    fun parse() {
        val result = tokenizer.tokenize(
            "(table $0 (import \"a\" \"b\") 0 1 funcref)",
            context
        ).parseInlineTableImport(0) ?: Assertions.fail("Expected a result")

        assertThat(result.parseLength).isEqualTo(12)
        assertThat(result.astNode).isEqualTo(
            Import(
                "a",
                "b",
                ImportDescriptor.Table(
                    Identifier.Table("$0"),
                    TableType(
                        Limit(0u, 1u),
                        ElementType.FunctionReference
                    )
                )
            )
        )
    }
}