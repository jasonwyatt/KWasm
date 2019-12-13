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
import kwasm.ast.Identifier
import kwasm.ast.Index
import kwasm.ast.Param
import kwasm.ast.TypeUse
import kwasm.ast.ValueType
import kwasm.ast.ValueTypeEnum
import kwasm.ast.astNodeListOf
import kwasm.format.ParseContext
import kwasm.format.ParseException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TypeUseTest {
    private val tokenizer = Tokenizer()
    private val context = ParseContext("TypeUseTest.wat", 1, 1)

    @Test
    fun parses_emptyTypeUse_fromEmptySource() {
        val result = tokenizer.tokenize("", context).parseTypeUse(0)
        assertThat(result.parseLength).isEqualTo(0)
        assertThat(result.astNode.index).isNull()
        assertThat(result.astNode.params).isEmpty()
        assertThat(result.astNode.results).isEmpty()
    }

    @Test
    fun parses_emptyTypeUse_fromNonTypeUseToken() {
        val result = tokenizer.tokenize("\$not-a-typeuse", context).parseTypeUse(0)
        assertThat(result.parseLength).isEqualTo(0)
        assertThat(result.astNode.index).isNull()
        assertThat(result.astNode.params).isEmpty()
        assertThat(result.astNode.results).isEmpty()
    }

    @Test
    fun parses_emptyTypeUse_fromNonTypeUseToken_partDeux() {
        val result = tokenizer.tokenize("(func)", context).parseTypeUse(0)
        assertThat(result.parseLength).isEqualTo(0)
        assertThat(result.astNode.index).isNull()
        assertThat(result.astNode.params).isEmpty()
        assertThat(result.astNode.results).isEmpty()
    }

    @Test
    fun parses_typeUse_withNoTypeIndex_onlyParams() {
        val result = tokenizer.tokenize("(param i32) (param f32)").parseTypeUse(0)
        assertThat(result.parseLength).isEqualTo(8)
        assertThat(result.astNode.index).isNull()
        assertThat(result.astNode.params).hasSize(2)
        assertThat(result.astNode.params).isEqualTo(
            astNodeListOf(
                Param(null, ValueType(ValueTypeEnum.I32)),
                Param(null, ValueType(ValueTypeEnum.F32))
            )
        )
        assertThat(result.astNode.results).isEmpty()
    }

    @Test
    fun parses_typeUse_withNoTypeIndex_onlyResults() {
        val result = tokenizer.tokenize("(result i32) (result f32)").parseTypeUse(0)
        assertThat(result.parseLength).isEqualTo(8)
        assertThat(result.astNode.index).isNull()
        assertThat(result.astNode.results).hasSize(2)
        assertThat(result.astNode.results).isEqualTo(
            astNodeListOf(
                kwasm.ast.Result(ValueType(ValueTypeEnum.I32)),
                kwasm.ast.Result(ValueType(ValueTypeEnum.F32))
            )
        )
        assertThat(result.astNode.params).isEmpty()
    }

    @Test
    fun parses_typeUse_withNoTypeIndex_paramsAndResults() {
        val result = tokenizer.tokenize("(param i32) (param f64) (result i32) (result f32)")
                .parseTypeUse(0)
        assertThat(result.parseLength).isEqualTo(16)
        assertThat(result.astNode.index).isNull()
        assertThat(result.astNode.params).hasSize(2)
        assertThat(result.astNode.params).isEqualTo(
            astNodeListOf(
                Param(null, ValueType(ValueTypeEnum.I32)),
                Param(null, ValueType(ValueTypeEnum.F64))
            )
        )
        assertThat(result.astNode.results).hasSize(2)
        assertThat(result.astNode.results).isEqualTo(
            astNodeListOf(
                kwasm.ast.Result(ValueType(ValueTypeEnum.I32)),
                kwasm.ast.Result(ValueType(ValueTypeEnum.F32))
            )
        )
    }

    @Test
    fun parses_typeUse_allParamsBeforeResults() {
        val result = tokenizer.tokenize(
            """
                (param i32) (result i64) (; 
                    the next params/results won't be picked up 
                ;) (param f64) (result i32) (result f32)
            """.trimIndent(),
            context
        ).parseTypeUse(0)
        assertThat(result.parseLength).isEqualTo(8)
        assertThat(result.astNode.index).isNull()
        assertThat(result.astNode.params).hasSize(1)
        assertThat(result.astNode.params).isEqualTo(
            astNodeListOf(
                Param(null, ValueType(ValueTypeEnum.I32))
            )
        )
        assertThat(result.astNode.results).hasSize(1)
        assertThat(result.astNode.results).isEqualTo(
            astNodeListOf(
                kwasm.ast.Result(ValueType(ValueTypeEnum.I64))
            )
        )
    }

    @Test
    fun parses_typeUse_onlyIndex() {
        val result = tokenizer.tokenize("(type $0)").parseTypeUse(0)
        assertThat(result.parseLength).isEqualTo(4)
        val index = result.astNode.index as? Index.ByIdentifier<Identifier.Type>
            ?: fail("No index parsed.")
        assertThat(index.indexVal.stringRepr).isEqualTo("$0")
    }

    @Test
    fun throws_whenTypeIndex_isInvalid() {
        assertThatThrownBy { tokenizer.tokenize("(type not-an-id)").parseTypeUse(0) }
            .isInstanceOf(ParseException::class.java).hasMessageContaining("Expected an index")
        assertThatThrownBy {
            tokenizer.tokenize("(type \$anId ;; but no closing paren").parseTypeUse(0)
        }.isInstanceOf(ParseException::class.java).hasMessageContaining("Expected \")\"")
    }

    @Test
    fun parses_longform() {
        val result = tokenizer.tokenize(
            """
                (type ${'$'}myId) (param i32) (param i64) (result f32) (result f64)
            """.trimIndent(),
            context
        ).parseTypeUse(0)

        assertThat(result.parseLength).isEqualTo(20)
        assertThat(result.astNode).isEqualTo(
            TypeUse(
                Index.ByIdentifier(Identifier.Type("\$myId")),
                astNodeListOf(
                    Param(null, ValueType(ValueTypeEnum.I32)),
                    Param(null, ValueType(ValueTypeEnum.I64))
                ),
                astNodeListOf(
                    kwasm.ast.Result(ValueType(ValueTypeEnum.F32)),
                    kwasm.ast.Result(ValueType(ValueTypeEnum.F64))
                )
            )
        )
    }
}
