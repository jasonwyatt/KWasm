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
import kwasm.ast.FunctionType
import kwasm.ast.Identifier
import kwasm.ast.Import
import kwasm.ast.ImportDescriptor
import kwasm.ast.Local
import kwasm.ast.Param
import kwasm.ast.Result
import kwasm.ast.ResultType
import kwasm.ast.Type
import kwasm.ast.TypeUse
import kwasm.ast.ValueType
import kwasm.ast.WasmFunction
import kwasm.ast.astNodeListOf
import kwasm.format.ParseContext
import kwasm.format.ParseException
import org.assertj.core.api.Assertions.fail
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class WasmModuleTest {
    private val tokenizer = Tokenizer()
    private val context = ParseContext("WasmModuleTest.wat")

    @Test
    fun parse_returnsNullIf_noOpeningParen() {
        val result = tokenizer.tokenize("module)", context).parseModule(0)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNullIf_moduleKeywordNotFound() {
        val result = tokenizer.tokenize("(nonmodule)", context).parseModule(0)
        assertThat(result).isNull()
    }

    @Test
    fun parse_throwsIf_closingParenNotFound() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(module", context).parseModule(0)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parse_throwsIf_moreThanOneStartFunction_isDefined() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(module (func) (start 0) (start 0))").parseModule(0)
        }
        assertThat(e).hasMessageThat().contains("Modules may only define one start function")
    }

    @Test
    fun parse_minimal() {
        val result = tokenizer.tokenize("(module)", context).parseModule(0)
            ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(3)
        val module = result.astNode
        assertThat(module.identifier?.stringRepr).isNull()
        assertThat(module.types).isEmpty()
        assertThat(module.imports).isEmpty()
        assertThat(module.functions).isEmpty()
        assertThat(module.tables).isEmpty()
        assertThat(module.memories).isEmpty()
        assertThat(module.globals).isEmpty()
        assertThat(module.exports).isEmpty()
        assertThat(module.start).isNull()
        assertThat(module.elements).isEmpty()
        assertThat(module.data).isEmpty()
    }

    @Test
    fun parse_someOfEverything() {
        val result = tokenizer.tokenize(
            """
                (module $10
                    (import "other" "func" (func $0))
                    (type $1 (func (param i32) (result i64)))
                    (func $2 (local i32) return)
                    (table $3 0 1 funcref)
                    (memory $4 0 1)
                    (global $5 (mut i32) (i32.const 1))
                    (export "num2" (func $2))
                    (start $2)
                    (elem $3 (offset (i32.const 0)) $0)
                    (data $4 (offset (i32.const 0)) "test")
                )
            """.trimIndent(),
            context
        ).parseModule(0) ?: fail("Expected a result")

        assertThat(result.parseLength).isEqualTo(98)
        val module = result.astNode
        assertThat(module.identifier).isEqualTo(Identifier.Label("$10"))
        assertThat(module.imports)
            .containsExactly(
                Import(
                    "other",
                    "func",
                    ImportDescriptor.Function(
                        Identifier.Function("$0"),
                        TypeUse(null, astNodeListOf(), astNodeListOf())
                    )
                )
            ).inOrder()
        assertThat(module.types)
            .containsExactly(
                Type(
                    Identifier.Type("$1"),
                    FunctionType(
                        astNodeListOf(Param(Identifier.Local(null, null), ValueType.I32)),
                        astNodeListOf(Result(ValueType.I64))
                    )
                )
            ).inOrder()
        assertThat(module.functions)
            .containsExactly(
                WasmFunction(
                    Identifier.Function("$2"),
                    TypeUse(
                        null,
                        astNodeListOf(),
                        astNodeListOf()
                    ),
                    astNodeListOf(
                        Local(null, ValueType.I32)
                    ),
                    astNodeListOf(ControlInstruction.Return)
                )
            ).inOrder()
    }
}