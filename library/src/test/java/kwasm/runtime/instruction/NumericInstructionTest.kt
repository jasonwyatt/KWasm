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
import kwasm.runtime.EmptyExecutionContext
import kwasm.runtime.ExecutionContext
import kwasm.runtime.IntValue
import kwasm.runtime.Value
import kwasm.runtime.stack.OperandStack
import kwasm.runtime.toValue
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.sqrt

@RunWith(JUnit4::class)
class NumericInstructionTest {
    @get:Rule
    val parser = ParseRule()

    private val executionContext: ExecutionContext
        get() = EmptyExecutionContext()

    @Test
    fun i32CountLeadingZeroes() = parser.with {
        val instruction = "i32.clz".parseInstruction()

        var resultContext = instruction.execute(executionContextWithOpStack(0.toValue()))
        assertThat(resultContext).hasOpStackContaining(32.toValue())

        var stackValue = 1
        (1 until 32).forEach {
            val expected = (32 - it).toValue()
            resultContext = instruction.execute(executionContextWithOpStack(stackValue.toValue()))
            assertThat(resultContext.stacks.operands.height).isEqualTo(1)
            assertThat(resultContext.stacks.operands.peek()).isEqualTo(expected)
            stackValue = stackValue shl 1
        }
    }

    @Test
    fun i32CountTrailingZeroes() = parser.with {
        val instruction = "i32.ctz".parseInstruction()

        var resultContext = instruction.execute(executionContextWithOpStack(0.toValue()))
        assertThat(resultContext).hasOpStackContaining(32.toValue())

        var stackValue = 1 shl 31
        (1 until 32).forEach {
            val expected = (32 - it).toValue()
            resultContext = instruction.execute(executionContextWithOpStack(stackValue.toValue()))
            assertThat(resultContext).hasOpStackContaining(expected)
            stackValue = stackValue ushr 1
        }
    }

    @Test
    fun i32CountNonZeroBits() = parser.with {
        val instruction = "i32.popcnt".parseInstruction()

        var resultContext = instruction.execute(executionContextWithOpStack(0.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-1).toValue()))
        assertThat(resultContext).hasOpStackContaining(32.toValue())

        var stackStartValue = 1
        (1 until 32).forEach { expected ->
            var stackValue = stackStartValue
            // shift the stack value across the space
            repeat(32 - expected) {
                resultContext = instruction.execute(
                    executionContextWithOpStack(stackValue.toValue())
                )
                assertThat(resultContext).hasOpStackContaining(expected.toValue())
                stackValue = stackValue shl 1
            }
            // add a 1-bit to the stack start value
            stackStartValue = (stackStartValue shl 1) or 1
        }
    }

    @Test
    fun i64CountLeadingZeroes() = parser.with {
        val instruction = "i64.clz".parseInstruction()

        var resultContext = instruction.execute(executionContextWithOpStack(0L.toValue()))
        assertThat(resultContext.stacks.operands.height).isEqualTo(1)
        assertThat(resultContext.stacks.operands.peek()).isEqualTo(64L.toValue())

        var stackValue = 1L
        (1 until 64).forEach {
            val expected = (64 - it).toLong().toValue()
            resultContext = instruction.execute(executionContextWithOpStack(stackValue.toValue()))
            assertThat(resultContext.stacks.operands.height).isEqualTo(1)
            assertThat(resultContext.stacks.operands.peek()).isEqualTo(expected)
            stackValue = stackValue shl 1
        }
    }

    @Test
    fun i64CountTrailingZeroes() = parser.with {
        val instruction = "i64.ctz".parseInstruction()

        var resultContext = instruction.execute(executionContextWithOpStack(0L.toValue()))
        assertThat(resultContext.stacks.operands.height).isEqualTo(1)
        assertThat(resultContext.stacks.operands.peek()).isEqualTo(64L.toValue())

        var stackValue = 1L shl 63
        (1 until 64).forEach {
            val expected = (64 - it).toLong().toValue()
            resultContext = instruction.execute(executionContextWithOpStack(stackValue.toValue()))
            assertThat(resultContext.stacks.operands.height).isEqualTo(1)
            assertThat(resultContext.stacks.operands.peek()).isEqualTo(expected)
            stackValue = stackValue ushr 1
        }
    }

    @Test
    fun i64CountNonZeroBits() = parser.with {
        val instruction = "i64.popcnt".parseInstruction()

        var resultContext = instruction.execute(executionContextWithOpStack(0L.toValue()))
        assertThat(resultContext).hasOpStackContaining(0L.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-1L).toValue()))
        assertThat(resultContext).hasOpStackContaining(64L.toValue())

        var stackStartValue = 1L
        (1 until 64).forEach { expected ->
            var stackValue = stackStartValue
            // shift the stack value across the space
            repeat(64 - expected) {
                resultContext = instruction.execute(
                    executionContextWithOpStack(stackValue.toValue())
                )
                assertThat(resultContext.stacks.operands.height).isEqualTo(1)
                assertThat(resultContext.stacks.operands.pop())
                    .isEqualTo(expected.toLong().toValue())
                stackValue = stackValue shl 1
            }
            // add a 1-bit to the stack start value
            stackStartValue = (stackStartValue shl 1) or 1
        }
    }

    @Test
    fun f32AbsoluteValue() = parser.with {
        val instruction = "f32.abs".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0f.toValue()))
        assertThat(resultContext).hasOpStackContaining(0f.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0f).toValue()))
        assertThat(resultContext).hasOpStackContaining(0f.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-1f).toValue()))
        assertThat(resultContext).hasOpStackContaining(1f.toValue())

        resultContext = instruction.execute(executionContextWithOpStack(1f.toValue()))
        assertThat(resultContext).hasOpStackContaining(1f.toValue())
    }

    @Test
    fun f32Negative() = parser.with {
        val instruction = "f32.neg".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0f.toValue()))
        assertThat(resultContext).hasOpStackContaining((-0f).toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0f).toValue()))
        assertThat(resultContext).hasOpStackContaining(0f.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-1f).toValue()))
        assertThat(resultContext).hasOpStackContaining(1f.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-1f).toValue()))
        assertThat(resultContext).hasOpStackContaining(1f.toValue())
    }

    @Test
    fun f32Ceil() = parser.with {
        val instruction = "f32.ceil".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.0f.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0f.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.0f).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0f).toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.1f).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0f).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.1f.toValue()))
        assertThat(resultContext).hasOpStackContaining(1.0f.toValue())
    }

    @Test
    fun f32Floor() = parser.with {
        val instruction = "f32.floor".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.0f.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0f.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.0f).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0f).toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.1f).toValue()))
        assertThat(resultContext).hasOpStackContaining((-1.0f).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.1f.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0f.toValue())
    }

    @Test
    fun f32Truncate() = parser.with {
        val instruction = "f32.trunc".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.0f.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0f.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.0f).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0f).toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.1f).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0f).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.1f.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0f.toValue())

        resultContext = instruction.execute(executionContextWithOpStack(1.1f.toValue()))
        assertThat(resultContext).hasOpStackContaining(1.0f.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-1.1f).toValue()))
        assertThat(resultContext).hasOpStackContaining((-1.0f).toValue())
    }

    @Test
    fun f32Nearest() = parser.with {
        val instruction = "f32.nearest".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.0f.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0f.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.0f).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0f).toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.1f).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0f).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.1f.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0f.toValue())

        resultContext = instruction.execute(executionContextWithOpStack(1.5f.toValue()))
        assertThat(resultContext).hasOpStackContaining(2.0f.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-1.5f).toValue()))
        assertThat(resultContext).hasOpStackContaining((-2.0f).toValue())
    }

    @Test
    fun f32SquareRoot() = parser.with {
        val instruction = "f32.sqrt".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.0f.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0f.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.0f).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0f).toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.1f).toValue()))
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.1f.toValue()))
        assertThat(resultContext).hasOpStackContaining(sqrt(0.1f).toValue())
    }

    @Test
    fun f64AbsoluteValue() = parser.with {
        val instruction = "f64.abs".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.0.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.0).toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-1.0).toValue()))
        assertThat(resultContext).hasOpStackContaining(1.0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack(1.0.toValue()))
        assertThat(resultContext).hasOpStackContaining(1.0.toValue())
    }

    @Test
    fun f64Negative() = parser.with {
        val instruction = "f64.neg".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.0.toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.0).toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-1.0).toValue()))
        assertThat(resultContext).hasOpStackContaining(1.0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-1.0).toValue()))
        assertThat(resultContext).hasOpStackContaining(1.0.toValue())
    }

    @Test
    fun f64Ceil() = parser.with {
        val instruction = "f64.ceil".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext.stacks.operands.height).isEqualTo(1)
        assertThat(resultContext.stacks.operands.pop())
            .isEqualTo(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.0.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.0).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.1).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.1.toValue()))
        assertThat(resultContext).hasOpStackContaining(1.0.toValue())
    }

    @Test
    fun f64Floor() = parser.with {
        val instruction = "f64.floor".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.0.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.0).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.1).toValue()))
        assertThat(resultContext).hasOpStackContaining((-1.0).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.1.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())
    }

    @Test
    fun f64Truncate() = parser.with {
        val instruction = "f64.trunc".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.0.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.0).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.1).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.1.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack(1.1.toValue()))
        assertThat(resultContext).hasOpStackContaining(1.0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-1.1).toValue()))
        assertThat(resultContext).hasOpStackContaining((-1.0).toValue())
    }

    @Test
    fun f64Nearest() = parser.with {
        val instruction = "f64.nearest".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.0.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.0).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.1).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.1.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack(1.5.toValue()))
        assertThat(resultContext).hasOpStackContaining(2.0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-1.5).toValue()))
        assertThat(resultContext).hasOpStackContaining((-2.0).toValue())
    }

    @Test
    fun f64SquareRoot() = parser.with {
        val instruction = "f64.sqrt".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.0.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.0).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.1).toValue()))
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.1.toValue()))
        assertThat(resultContext).hasOpStackContaining(sqrt(0.1).toValue())
    }

    @Test
    fun unaryOp_throws_ifStackEmpty() {
        assertThrows(IllegalStateException::class.java) {
            unaryOp<IntValue, IntValue>(executionContext) {
                1.toValue()
            }
        }
    }

    @Test
    fun unaryOp_throws_ifStackTopInvalidType() {
        assertThrows(KWasmRuntimeException::class.java) {
            unaryOp<IntValue, IntValue>(executionContextWithOpStack(42L.toValue())) {
                1.toValue()
            }
        }.also {
            assertThat(it).hasMessageThat().contains("Top of stack is invalid type")
        }
    }

    @Test
    fun unaryOp_valid() {
        val result = unaryOp<IntValue, IntValue>(executionContextWithOpStack(13.toValue())) {
            42.toValue()
        }
        assertThat(result).hasOpStackContaining(42.toValue())
    }

    private fun assertThat(context: ExecutionContext) = object {
        fun hasOpStackContaining(vararg vals: Value<*>) {
            val stack = mutableListOf<Value<*>>()
            while (context.stacks.operands.peek() != null) {
                stack += context.stacks.operands.pop()
            }
            stack.reverse()
            assertThat(stack).containsExactly(*vals).inOrder()
        }
    }

    private fun executionContextWithOpStack(vararg stackVals: Value<*>) =
        executionContext.also {
            stackVals.forEach { stackVal ->
                it.stacks.operands.push(stackVal)
            }
        }
}
