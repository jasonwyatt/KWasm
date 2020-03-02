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

package kwasm.runtime.instruction

import com.google.common.truth.Truth.assertThat
import kwasm.KWasmRuntimeException
import kwasm.ParseRule
import kwasm.ast.util.toGlobalIndex
import kwasm.runtime.Address
import kwasm.runtime.EmptyExecutionContext
import kwasm.runtime.ExecutionContext
import kwasm.runtime.Global
import kwasm.runtime.Store
import kwasm.runtime.toValue
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class VariableInstructionTest {
    @get:Rule
    val parser = ParseRule()

    private val executionContext: ExecutionContext
        get() = EmptyExecutionContext().let {
            it.moduleInstance.globalAddresses.addAll(
                listOf(
                    Address.Global(0),
                    Address.Global(1),
                    Address.Global(2),
                    Address.Global(3),
                    Address.Global(4),
                    Address.Global(5),
                    Address.Global(6),
                    Address.Global(7)
                )
            )
            it.copy(
                store = Store(
                    globals = listOf(
                        Global.Int(10, true),
                        Global.Long(10, true),
                        Global.Float(10f, true),
                        Global.Double(10.0, true),
                        Global.Int(10, false),
                        Global.Long(10, false),
                        Global.Float(10f, false),
                        Global.Double(10.0, false)
                    )
                )
            )
        }

    @Test
    fun globalGet_throws_onInvalidIndex() = parser.with {
        val instruction = "global.get \$foo".parseInstruction()
        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(executionContext)
        }.also {
            assertThat(it).hasMessageThat()
                .contains("Global with index ${"\$foo".toGlobalIndex()} not found")
        }
    }

    @Test
    fun globalGet_int_valid() = parser.with {
        val instruction = "global.get 0".parseInstruction()
        val result = instruction.execute(executionContext)
        assertThat(result.stacks.operands.pop()).isEqualTo(10.toValue())
    }

    @Test
    fun globalGet_long_valid() = parser.with {
        val instruction = "global.get 1".parseInstruction()
        val result = instruction.execute(executionContext)
        assertThat(result.stacks.operands.pop()).isEqualTo(10L.toValue())
    }

    @Test
    fun globalGet_float_valid() = parser.with {
        val instruction = "global.get 2".parseInstruction()
        val result = instruction.execute(executionContext)
        assertThat(result.stacks.operands.pop()).isEqualTo(10f.toValue())
    }

    @Test
    fun globalGet_double_valid() = parser.with {
        val instruction = "global.get 3".parseInstruction()
        val result = instruction.execute(executionContext)
        assertThat(result.stacks.operands.pop()).isEqualTo(10.0.toValue())
    }

    @Test
    fun globalSet_throws_onInvalidIndex() = parser.with {
        val instruction = "global.set \$foo".parseInstruction()
        val context = executionContext
        context.stacks.operands.push(11.toValue())

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(context)
        }.also {
            assertThat(it).hasMessageThat()
                .contains("Global with index ${"\$foo".toGlobalIndex()} not found")
        }
    }

    @Test
    fun globalSet_throws_onImmutableGlobal() = parser.with {
        val instruction = "global.set 4".parseInstruction()
        val context = executionContext
        context.stacks.operands.push(11.toValue())

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(context)
        }.also {
            assertThat(it).hasMessageThat()
                .contains("Global with index 4 is not mutable")
        }
    }

    @Test
    fun globalSet_throws_onEmptyStack() = parser.with {
        val instruction = "global.set 4".parseInstruction()
        val context = executionContext

        // TODO: throw a KWasmRuntimeException
        assertThrows(Exception::class.java) {
            instruction.execute(context)
        }
    }

    @Test
    fun globalSet_int_throwsOnIncorrectValueType_inStack() = parser.with {
        val instruction = "global.set 0".parseInstruction()
        val context = executionContext
        context.stacks.operands.push(11L.toValue())

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(context)
        }.also {
            assertThat(it).hasMessageThat().contains("Top of stack is not expected type")
        }
    }

    @Test
    fun globalSet_int() = parser.with {
        val instruction = "global.set 0".parseInstruction()
        val context = executionContext
        context.stacks.operands.push(11.toValue())

        val resultContext = instruction.execute(context)

        assertThat(resultContext).isSameInstanceAs(context)
        assertThat(resultContext.stacks.operands.height).isEqualTo(0)
        assertThat(resultContext.store.globals[0].value).isEqualTo(11)
    }

    @Test
    fun globalSet_long_throwsOnIncorrectValueType_inStack() = parser.with {
        val instruction = "global.set 1".parseInstruction()
        val context = executionContext
        context.stacks.operands.push(11.toValue())

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(context)
        }.also {
            assertThat(it).hasMessageThat().contains("Top of stack is not expected type")
        }
    }

    @Test
    fun globalSet_long() = parser.with {
        val instruction = "global.set 1".parseInstruction()
        val context = executionContext
        context.stacks.operands.push(11L.toValue())

        val resultContext = instruction.execute(context)

        assertThat(resultContext).isSameInstanceAs(context)
        assertThat(resultContext.stacks.operands.height).isEqualTo(0)
        assertThat(resultContext.store.globals[1].value).isEqualTo(11L)
    }

    @Test
    fun globalSet_float_throwsOnIncorrectValueType_inStack() = parser.with {
        val instruction = "global.set 2".parseInstruction()
        val context = executionContext
        context.stacks.operands.push(11.0.toValue())

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(context)
        }.also {
            assertThat(it).hasMessageThat().contains("Top of stack is not expected type")
        }
    }

    @Test
    fun globalSet_float() = parser.with {
        val instruction = "global.set 2".parseInstruction()
        val context = executionContext
        context.stacks.operands.push(11.0f.toValue())

        val resultContext = instruction.execute(context)

        assertThat(resultContext).isSameInstanceAs(context)
        assertThat(resultContext.stacks.operands.height).isEqualTo(0)
        assertThat(resultContext.store.globals[2].value).isEqualTo(11.0f)
    }

    @Test
    fun globalSet_double_throwsOnIncorrectValueType_inStack() = parser.with {
        val instruction = "global.set 3".parseInstruction()
        val context = executionContext
        context.stacks.operands.push(11.0f.toValue())

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(context)
        }.also {
            assertThat(it).hasMessageThat().contains("Top of stack is not expected type")
        }
    }

    @Test
    fun globalSet_double() = parser.with {
        val instruction = "global.set 3".parseInstruction()
        val context = executionContext
        context.stacks.operands.push(11.0.toValue())

        val resultContext = instruction.execute(context)

        assertThat(resultContext).isSameInstanceAs(context)
        assertThat(resultContext.stacks.operands.height).isEqualTo(0)
        assertThat(resultContext.store.globals[3].value).isEqualTo(11.0)
    }
}
