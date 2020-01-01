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
import kwasm.ast.DataSegment
import kwasm.ast.ElementSegment
import kwasm.ast.ElementType
import kwasm.ast.Export
import kwasm.ast.ExportDescriptor
import kwasm.ast.Expression
import kwasm.ast.FunctionType
import kwasm.ast.Global
import kwasm.ast.GlobalType
import kwasm.ast.Identifier
import kwasm.ast.Import
import kwasm.ast.ImportDescriptor
import kwasm.ast.Index
import kwasm.ast.IntegerLiteral
import kwasm.ast.Limit
import kwasm.ast.Local
import kwasm.ast.Memory
import kwasm.ast.MemoryType
import kwasm.ast.NumericConstantInstruction
import kwasm.ast.Offset
import kwasm.ast.Param
import kwasm.ast.Result
import kwasm.ast.StartFunction
import kwasm.ast.Table
import kwasm.ast.TableType
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

@Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")
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
                    ;; inline abbreviations
                    (func $6 (import "a" "b")) ;; 9 tokens
                    (func $7 (export "a") return) ;; 9 tokens
                    (table $8 (import "a" "b") 1 funcref) ;; 9 tokens
                    (table $9 (export "a") 1 funcref) ;; 10 tokens
                    (memory $10 (import "a" "b") 1) ;; 10 tokens
                    (memory $11 (export "a") 1) ;; 9 tokens
                )
            """.trimIndent(),
            context
        ).parseModule(0) ?: fail("Expected a result")

        assertThat(result.parseLength).isEqualTo(156)
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
                ),
                Import(
                    "a",
                    "b",
                    ImportDescriptor.Function(
                        Identifier.Function("$6"),
                        TypeUse(null, astNodeListOf(), astNodeListOf())
                    )
                ),
                Import(
                    "a",
                    "b",
                    ImportDescriptor.Table(
                        Identifier.Table("$8"),
                        TableType(
                            Limit(1u, UInt.MAX_VALUE),
                            ElementType.FunctionReference
                        )
                    )
                ),
                Import(
                    "a",
                    "b",
                    ImportDescriptor.Memory(
                        Identifier.Memory("$10"),
                        MemoryType(Limit(1u, UInt.MAX_VALUE))
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
                ),
                WasmFunction(
                    Identifier.Function("$7"),
                    TypeUse(null, astNodeListOf(), astNodeListOf()),
                    astNodeListOf(),
                    astNodeListOf(ControlInstruction.Return)
                )
            ).inOrder()
        assertThat(module.tables)
            .containsExactly(
                Table(
                    Identifier.Table("$3"),
                    TableType(
                        Limit(0u, 1u),
                        ElementType.FunctionReference
                    )
                ),
                Table(
                    Identifier.Table("$9"),
                    TableType(
                        Limit(1u, UInt.MAX_VALUE),
                        ElementType.FunctionReference
                    )
                )
            ).inOrder()
        assertThat(module.memories)
            .containsExactly(
                Memory(
                    Identifier.Memory("$4"),
                    MemoryType(Limit(0u, 1u))
                ),
                Memory(
                    Identifier.Memory("$11"),
                    MemoryType(Limit(1u, UInt.MAX_VALUE))
                )
            ).inOrder()
        assertThat(module.globals)
            .containsExactly(
                Global(
                    Identifier.Global("$5"),
                    GlobalType(ValueType.I32, true),
                    Expression(
                        astNodeListOf(
                            NumericConstantInstruction.I32(IntegerLiteral.S32(1))
                        )
                    )
                )
            ).inOrder()
        assertThat(module.exports)
            .containsExactly(
                Export(
                    "num2",
                    ExportDescriptor.Function(
                        Index.ByIdentifier(Identifier.Function("$2"))
                    )
                ),
                Export(
                    "a",
                    ExportDescriptor.Function(
                        Index.ByIdentifier(Identifier.Function("$7"))
                    )
                ),
                Export(
                    "a",
                    ExportDescriptor.Table(
                        Index.ByIdentifier(Identifier.Table("$9"))
                    )
                ),
                Export(
                    "a",
                    ExportDescriptor.Memory(
                        Index.ByIdentifier(Identifier.Memory("$11"))
                    )
                )
            ).inOrder()
        assertThat(module.start)
            .isEqualTo(
                StartFunction(Index.ByIdentifier(Identifier.Function("$2")))
            )
        assertThat(module.elements)
            .containsExactly(
                ElementSegment(
                    Index.ByIdentifier(Identifier.Table("$3")),
                    Offset(
                        Expression(
                            astNodeListOf(
                                NumericConstantInstruction.I32(IntegerLiteral.S32(0))
                            )
                        )
                    ),
                    astNodeListOf(
                        Index.ByIdentifier(Identifier.Function("$0"))
                    )
                )
            ).inOrder()
        assertThat(module.data)
            .containsExactly(
                DataSegment(
                    Index.ByIdentifier(Identifier.Memory("$4")),
                    Offset(
                        Expression(
                            astNodeListOf(
                                NumericConstantInstruction.I32(IntegerLiteral.S32(0))
                            )
                        )
                    ),
                    "test".toByteArray(Charsets.UTF_8)
                )
            ).inOrder()
    }
}