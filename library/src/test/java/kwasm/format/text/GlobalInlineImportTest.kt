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
import kwasm.ast.Identifier
import kwasm.ast.Import
import kwasm.ast.ImportDescriptor
import kwasm.ast.GlobalType
import kwasm.ast.ValueType
import kwasm.format.ParseContext
import kwasm.format.ParseException
import org.assertj.core.api.Assertions
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GlobalInlineImportTest {
    private val tokenizer = Tokenizer()
    private val context = ParseContext("GlobalInlineImportTest.wat")

    @Test
    fun parse_returnsNullIf_openingParenNotFound() {
        val result = tokenizer.tokenize("global $0 (import \"a\" \"b\") i32)", context)
            .parseInlineGlobalImport(0)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNullIf_globalKeywordNotFound() {
        val result = tokenizer.tokenize("(nonglobal $0 (import \"a\" \"b\") i32)", context)
            .parseInlineGlobalImport(0)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNullIf_importOpeningParenNotFound() {
        val result = tokenizer.tokenize("(global $0 import \"a\" \"b\") i32)", context)
            .parseInlineGlobalImport(0)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNullIf_importKeywordNotFound() {
        val result = tokenizer.tokenize("(global $0 (nonimport \"a\" \"b\") i32)", context)
            .parseInlineGlobalImport(0)
        assertThat(result).isNull()
    }

    @Test
    fun parse_throwsIf_moduleNameNotFound() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(global $0 (import) i32)", context)
                .parseInlineGlobalImport(0)
        }
    }

    @Test
    fun parse_throwsIf_globalNameNotFound() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(global $0 (import \"a\") i32)", context)
                .parseInlineGlobalImport(0)
        }
    }

    @Test
    fun parse_throwsIf_importNotClosed() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(global $0 (import \"a\" \"b\" i32)", context)
                .parseInlineGlobalImport(0)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parse_throwsIf_notClosed() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(global $0 (import \"a\" \"b\") i32", context)
                .parseInlineGlobalImport(0)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parse_minimal() {
        val result = tokenizer.tokenize("(global (import \"a\" \"b\") i32)", context)
            .parseInlineGlobalImport(0)
            ?: Assertions.fail("Expected a result")

        assertThat(result.parseLength).isEqualTo(9)
        assertThat(result.astNode).isEqualTo(
            Import(
                "a",
                "b",
                ImportDescriptor.Global(
                    Identifier.Global(null, null),
                    GlobalType(
                        ValueType.I32,
                        false
                    )
                )
            )
        )
    }

    @Test
    fun parse() {
        val result = tokenizer.tokenize(
            "(global $0 (import \"a\" \"b\") (mut i32))",
            context
        ).parseInlineGlobalImport(0) ?: Assertions.fail("Expected a result")

        assertThat(result.parseLength).isEqualTo(13)
        assertThat(result.astNode).isEqualTo(
            Import(
                "a",
                "b",
                ImportDescriptor.Global(
                    Identifier.Global("$0"),
                    GlobalType(
                        ValueType.I32,
                        true
                    )
                )
            )
        )
    }
}