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
import kotlin.math.sqrt
import kotlin.random.Random
import kwasm.KWasmRuntimeException
import kwasm.ParseRule
import kwasm.runtime.EmptyExecutionContext
import kwasm.runtime.ExecutionContext
import kwasm.runtime.FloatValue
import kwasm.runtime.IntValue
import kwasm.runtime.Value
import kwasm.runtime.toValue
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@ExperimentalStdlibApi
@Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")
@RunWith(JUnit4::class)
class NumericInstructionTest {
    @get:Rule
    val parser = ParseRule()

    private val executionContext: ExecutionContext
        get() = EmptyExecutionContext()

    @Test
    fun i32Add() = parser.with {
        val instruction = "i32.add".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(0.toValue(), 0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0.toValue(), 1.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1.toValue(), 0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0.toValue(), (-1).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-1).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-1).toValue(), 0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-1).toValue())
    }

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
    fun i32EqualsZero() = parser.with {
        val instruction = "i32.eqz".parseInstruction()

        var resultContext = instruction.execute(executionContextWithOpStack(0.toValue()))
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(executionContextWithOpStack(1.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-1).toValue()))
        assertThat(resultContext).hasOpStackContaining(0.toValue())
    }

    @Test
    fun i32Equals() = parser.with {
        val instruction = "i32.eq".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42.toValue(), 42.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.toValue(), 41.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41.toValue(), 42.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())
    }

    @Test
    fun i32NotEquals() = parser.with {
        val instruction = "i32.ne".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42.toValue(), 42.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.toValue(), 41.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41.toValue(), 42.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())
    }

    @Test
    fun i32LessThanSigned() = parser.with {
        val instruction = "i32.lt_s".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42.toValue(), 42.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.toValue(), 41.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41.toValue(), 42.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41).toValue(), 42.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42).toValue(), (-41).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())
    }

    @Test
    fun i32LessThanUnsigned() = parser.with {
        val instruction = "i32.lt_u".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42.toValue(), 42.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.toValue(), 41.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41.toValue(), 42.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41).toValue(), 42.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42).toValue(), (-41).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0.toValue(), (-1).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())
    }

    @Test
    fun i32GreaterThanSigned() = parser.with {
        val instruction = "i32.gt_s".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42.toValue(), 42.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.toValue(), 41.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41.toValue(), 42.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41).toValue(), 42.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42).toValue(), (-41).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())
    }

    @Test
    fun i32GreaterThanUnsigned() = parser.with {
        val instruction = "i32.gt_u".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42.toValue(), 42.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.toValue(), 41.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41.toValue(), 42.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41).toValue(), 42.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42).toValue(), (-41).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0.toValue(), (-1).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())
    }

    @Test
    fun i32LessThanEqualToSigned() = parser.with {
        val instruction = "i32.le_s".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42.toValue(), 42.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.toValue(), 41.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41.toValue(), 42.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41).toValue(), 42.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42).toValue(), (-41).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())
    }

    @Test
    fun i32LessThanEqualToUnsigned() = parser.with {
        val instruction = "i32.le_u".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42.toValue(), 42.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.toValue(), 41.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41.toValue(), 42.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41).toValue(), 42.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42).toValue(), (-41).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0.toValue(), (-1).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())
    }

    @Test
    fun i32GreaterThanEqualToSigned() = parser.with {
        val instruction = "i32.ge_s".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42.toValue(), 42.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.toValue(), 41.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41.toValue(), 42.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41).toValue(), 42.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42).toValue(), (-41).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())
    }

    @Test
    fun i32GreaterThanEqualToUnsigned() = parser.with {
        val instruction = "i32.ge_u".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42.toValue(), 42.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.toValue(), 41.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41.toValue(), 42.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41).toValue(), 42.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42).toValue(), (-41).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0.toValue(), (-1).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())
    }

    @Test
    fun i32Subtract() = parser.with {
        val instruction = "i32.sub".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(0.toValue(), 0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0.toValue(), 1.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-1).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1.toValue(), 0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0.toValue(), (-1).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-1).toValue(), 0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-1).toValue())
    }

    @Test
    fun i32Multiply() = parser.with {
        val instruction = "i32.mul".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(0.toValue(), 0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0.toValue(), 1.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1.toValue(), 0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2.toValue(), (-1).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-2).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2.toValue(), 2.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(4.toValue())
    }

    @Test
    fun i32DivideSigned() = parser.with {
        val instruction = "i32.div_s".parseInstruction()
        var resultContext: ExecutionContext

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(
                executionContextWithOpStack(1.toValue(), 0.toValue())
            )
        }.also { assertThat(it).hasMessageThat().contains("Cannot divide by zero.") }

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(
                executionContextWithOpStack(Int.MIN_VALUE.toValue(), (-1).toValue())
            )
        }.also {
            assertThat(it).hasMessageThat().contains("Quotient unrepresentable as 32bit integer.")
        }

        resultContext = instruction.execute(
            executionContextWithOpStack(0.toValue(), 1.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2.toValue(), (-1).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-2).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2.toValue(), 2.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2.toValue(), 4.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())
    }

    @Test
    fun i32DivideUnsigned() = parser.with {
        val instruction = "i32.div_u".parseInstruction()
        var resultContext: ExecutionContext

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(
                executionContextWithOpStack(1.toValue(), 0.toValue())
            )
        }.also { assertThat(it).hasMessageThat().contains("Cannot divide by zero.") }

        resultContext = instruction.execute(
            executionContextWithOpStack(0.toValue(), 1.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2.toValue(), (-1).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(UInt.MAX_VALUE.toValue(), 1.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(UInt.MAX_VALUE.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2.toValue(), 2.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2.toValue(), 4.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())
    }

    @Test
    fun i32RemainderSigned() = parser.with {
        val instruction = "i32.rem_s".parseInstruction()
        var resultContext: ExecutionContext

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(
                executionContextWithOpStack(1.toValue(), 0.toValue())
            )
        }.also { assertThat(it).hasMessageThat().contains("Cannot divide by zero.") }

        resultContext = instruction.execute(
            executionContextWithOpStack(0.toValue(), 1.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1.toValue(), (-2).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-1).toValue(), 2.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-1).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-1).toValue(), (-2).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-1).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2.toValue(), 2.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2.toValue(), 4.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(2.toValue())
    }

    @Test
    fun i32RemainderUnsigned() = parser.with {
        val instruction = "i32.rem_u".parseInstruction()
        var resultContext: ExecutionContext

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(
                executionContextWithOpStack(1.toValue(), 0.toValue())
            )
        }.also { assertThat(it).hasMessageThat().contains("Cannot divide by zero.") }

        resultContext = instruction.execute(
            executionContextWithOpStack(0.toValue(), 1.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2.toValue(), 2.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2.toValue(), 4.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(2.toValue())
    }

    @Test
    fun i32BitwiseAnd() = parser.with {
        val instruction = "i32.and".parseInstruction()
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextInt()
            val randomRhs = random.nextInt()
            val expected = (randomLhs and randomRhs).toValue()

            val resultContext = instruction.execute(
                executionContextWithOpStack(randomLhs.toValue(), randomRhs.toValue())
            )
            assertThat(resultContext).hasOpStackContaining(expected)
        }
    }

    @Test
    fun i32BitwiseOr() = parser.with {
        val instruction = "i32.or".parseInstruction()
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextInt()
            val randomRhs = random.nextInt()
            val expected = (randomLhs or randomRhs).toValue()

            val resultContext = instruction.execute(
                executionContextWithOpStack(randomLhs.toValue(), randomRhs.toValue())
            )
            assertThat(resultContext).hasOpStackContaining(expected)
        }
    }

    @Test
    fun i32BitwiseXor() = parser.with {
        val instruction = "i32.xor".parseInstruction()
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextInt()
            val randomRhs = random.nextInt()
            val expected = (randomLhs xor randomRhs).toValue()

            val resultContext = instruction.execute(
                executionContextWithOpStack(randomLhs.toValue(), randomRhs.toValue())
            )
            assertThat(resultContext).hasOpStackContaining(expected)
        }
    }

    @Test
    fun i32ShiftLeft() = parser.with {
        val instruction = "i32.shl".parseInstruction()
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextInt()
            val randomRhs = random.nextInt()
            val expected = (randomLhs shl randomRhs).toValue()

            val resultContext = instruction.execute(
                executionContextWithOpStack(randomLhs.toValue(), randomRhs.toValue())
            )
            assertThat(resultContext).hasOpStackContaining(expected)
        }
    }

    @Test
    fun i32ShiftRightSigned() = parser.with {
        val instruction = "i32.shr_s".parseInstruction()
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextInt()
            val randomRhs = random.nextInt()
            val expected = (randomLhs shr randomRhs).toValue()

            val resultContext = instruction.execute(
                executionContextWithOpStack(randomLhs.toValue(), randomRhs.toValue())
            )
            assertThat(resultContext).hasOpStackContaining(expected)
        }
    }

    @Test
    fun i32ShiftRightUnsigned() = parser.with {
        val instruction = "i32.shr_u".parseInstruction()
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextInt()
            val randomRhs = random.nextInt()
            val expected = (randomLhs ushr randomRhs).toValue()

            val resultContext = instruction.execute(
                executionContextWithOpStack(randomLhs.toValue(), randomRhs.toValue())
            )
            assertThat(resultContext).hasOpStackContaining(expected)
        }
    }

    @Test
    fun i32RotateLeft() = parser.with {
        val instruction = "i32.rotl".parseInstruction()
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextInt()
            val randomRhs = random.nextInt()
            val expected = (randomLhs.rotateLeft(randomRhs)).toValue()

            val resultContext = instruction.execute(
                executionContextWithOpStack(randomLhs.toValue(), randomRhs.toValue())
            )
            assertThat(resultContext).hasOpStackContaining(expected)
        }
    }

    @Test
    fun i32RotateRight() = parser.with {
        val instruction = "i32.rotr".parseInstruction()
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextInt()
            val randomRhs = random.nextInt()
            val expected = (randomLhs.rotateRight(randomRhs)).toValue()

            val resultContext = instruction.execute(
                executionContextWithOpStack(randomLhs.toValue(), randomRhs.toValue())
            )
            assertThat(resultContext).hasOpStackContaining(expected)
        }
    }

    @Test
    fun i64CountLeadingZeroes() = parser.with {
        val instruction = "i64.clz".parseInstruction()

        var resultContext = instruction.execute(executionContextWithOpStack(0L.toValue()))
        assertThat(resultContext).hasOpStackContaining(64L.toValue())

        var stackValue = 1L
        (1 until 64).forEach {
            val expected = (64 - it).toLong().toValue()
            resultContext = instruction.execute(executionContextWithOpStack(stackValue.toValue()))
            assertThat(resultContext).hasOpStackContaining(expected)
            stackValue = stackValue shl 1
        }
    }

    @Test
    fun i64CountTrailingZeroes() = parser.with {
        val instruction = "i64.ctz".parseInstruction()

        var resultContext = instruction.execute(executionContextWithOpStack(0L.toValue()))
        assertThat(resultContext).hasOpStackContaining(64L.toValue())

        var stackValue = 1L shl 63
        (1 until 64).forEach {
            val expected = (64 - it).toLong().toValue()
            resultContext = instruction.execute(executionContextWithOpStack(stackValue.toValue()))
            assertThat(resultContext).hasOpStackContaining(expected)
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
                assertThat(resultContext).hasOpStackContaining(expected.toLong().toValue())
                stackValue = stackValue shl 1
            }
            // add a 1-bit to the stack start value
            stackStartValue = (stackStartValue shl 1) or 1
        }
    }

    @Test
    fun i64Add() = parser.with {
        val instruction = "i64.add".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(0L.toValue(), 0L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0L.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0L.toValue(), 1L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1L.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1L.toValue(), 0L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1L.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0L.toValue(), (-1L).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-1L).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-1L).toValue(), 0L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-1L).toValue())
    }

    @Test
    fun i64Subtract() = parser.with {
        val instruction = "i64.sub".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(0L.toValue(), 0L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0L.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0L.toValue(), 1L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-1L).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1L.toValue(), 0L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1L.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0L.toValue(), (-1L).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1L.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-1L).toValue(), 0L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-1L).toValue())
    }

    @Test
    fun i64Multiply() = parser.with {
        val instruction = "i64.mul".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(0L.toValue(), 0L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0L.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0L.toValue(), 1L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0L.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1L.toValue(), 0L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0L.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2L.toValue(), (-1L).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-2L).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2L.toValue(), 2L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(4L.toValue())
    }

    @Test
    fun i64DivideSigned() = parser.with {
        val instruction = "i64.div_s".parseInstruction()
        var resultContext: ExecutionContext

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(
                executionContextWithOpStack(1L.toValue(), 0L.toValue())
            )
        }.also { assertThat(it).hasMessageThat().contains("Cannot divide by zero.") }

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(
                executionContextWithOpStack(Long.MIN_VALUE.toValue(), (-1L).toValue())
            )
        }.also {
            assertThat(it).hasMessageThat().contains("Quotient unrepresentable as 64bit integer.")
        }

        resultContext = instruction.execute(
            executionContextWithOpStack(0L.toValue(), 1L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0L.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2L.toValue(), (-1L).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-2L).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2L.toValue(), 2L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1L.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2L.toValue(), 4L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0L.toValue())
    }

    @Test
    fun i64DivideUnsigned() = parser.with {
        val instruction = "i64.div_u".parseInstruction()
        var resultContext: ExecutionContext

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(
                executionContextWithOpStack(1L.toValue(), 0L.toValue())
            )
        }.also { assertThat(it).hasMessageThat().contains("Cannot divide by zero.") }

        resultContext = instruction.execute(
            executionContextWithOpStack(0L.toValue(), 1L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0L.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2L.toValue(), (-1L).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0L.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(ULong.MAX_VALUE.toValue(), 1L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(ULong.MAX_VALUE.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2L.toValue(), 2L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1L.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2L.toValue(), 4L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0L.toValue())
    }

    @Test
    fun i64RemainderSigned() = parser.with {
        val instruction = "i64.rem_s".parseInstruction()
        var resultContext: ExecutionContext

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(
                executionContextWithOpStack(1L.toValue(), 0L.toValue())
            )
        }.also { assertThat(it).hasMessageThat().contains("Cannot divide by zero.") }

        resultContext = instruction.execute(
            executionContextWithOpStack(0L.toValue(), 1L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0L.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1L.toValue(), (-2L).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1L.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-1L).toValue(), 2L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-1L).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-1L).toValue(), (-2L).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-1L).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2L.toValue(), 2L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0L.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2L.toValue(), 4L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(2L.toValue())
    }

    @Test
    fun i64RemainderUnsigned() = parser.with {
        val instruction = "i64.rem_u".parseInstruction()
        var resultContext: ExecutionContext

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(
                executionContextWithOpStack(1L.toValue(), 0L.toValue())
            )
        }.also { assertThat(it).hasMessageThat().contains("Cannot divide by zero.") }

        resultContext = instruction.execute(
            executionContextWithOpStack(0L.toValue(), 1L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0L.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2L.toValue(), 2L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0L.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2L.toValue(), 4L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(2L.toValue())
    }

    @Test
    fun i64BitwiseAnd() = parser.with {
        val instruction = "i64.and".parseInstruction()
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextLong()
            val randomRhs = random.nextLong()
            val expected = (randomLhs and randomRhs).toValue()

            val resultContext = instruction.execute(
                executionContextWithOpStack(randomLhs.toValue(), randomRhs.toValue())
            )
            assertThat(resultContext).hasOpStackContaining(expected)
        }
    }

    @Test
    fun i64BitwiseOr() = parser.with {
        val instruction = "i64.or".parseInstruction()
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextLong()
            val randomRhs = random.nextLong()
            val expected = (randomLhs or randomRhs).toValue()

            val resultContext = instruction.execute(
                executionContextWithOpStack(randomLhs.toValue(), randomRhs.toValue())
            )
            assertThat(resultContext).hasOpStackContaining(expected)
        }
    }

    @Test
    fun i64BitwiseXor() = parser.with {
        val instruction = "i64.xor".parseInstruction()
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextLong()
            val randomRhs = random.nextLong()
            val expected = (randomLhs xor randomRhs).toValue()

            val resultContext = instruction.execute(
                executionContextWithOpStack(randomLhs.toValue(), randomRhs.toValue())
            )
            assertThat(resultContext).hasOpStackContaining(expected)
        }
    }

    @Test
    fun i64ShiftLeft() = parser.with {
        val instruction = "i64.shl".parseInstruction()
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextLong()
            val randomRhs = random.nextLong().toULong()
            val expected = (randomLhs shl (randomRhs % 64uL).toInt()).toValue()

            val resultContext = instruction.execute(
                executionContextWithOpStack(randomLhs.toValue(), randomRhs.toValue())
            )
            assertThat(resultContext).hasOpStackContaining(expected)
        }
    }

    @Test
    fun i64ShiftRightSigned() = parser.with {
        val instruction = "i64.shr_s".parseInstruction()
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextLong()
            val randomRhs = random.nextLong().toULong()
            val expected = (randomLhs shr (randomRhs % 64uL).toInt()).toValue()

            val resultContext = instruction.execute(
                executionContextWithOpStack(randomLhs.toValue(), randomRhs.toValue())
            )
            assertThat(resultContext).hasOpStackContaining(expected)
        }
    }

    @Test
    fun i64ShiftRightUnsigned() = parser.with {
        val instruction = "i64.shr_u".parseInstruction()
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextLong()
            val randomRhs = random.nextLong().toULong()
            val expected = (randomLhs ushr (randomRhs % 64uL).toInt()).toValue()

            val resultContext = instruction.execute(
                executionContextWithOpStack(randomLhs.toValue(), randomRhs.toValue())
            )
            assertThat(resultContext).hasOpStackContaining(expected)
        }
    }

    @Test
    fun i64RotateLeft() = parser.with {
        val instruction = "i64.rotl".parseInstruction()
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextLong()
            val randomRhs = random.nextLong().toULong()
            val expected = (randomLhs.rotateLeft((randomRhs % 64uL).toInt())).toValue()

            val resultContext = instruction.execute(
                executionContextWithOpStack(randomLhs.toValue(), randomRhs.toValue())
            )
            assertThat(resultContext).hasOpStackContaining(expected)
        }
    }

    @Test
    fun i64RotateRight() = parser.with {
        val instruction = "i64.rotr".parseInstruction()
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextLong()
            val randomRhs = random.nextLong().toULong()
            val expected = (randomLhs.rotateRight((randomRhs % 64uL).toInt())).toValue()

            val resultContext = instruction.execute(
                executionContextWithOpStack(randomLhs.toValue(), randomRhs.toValue())
            )
            assertThat(resultContext).hasOpStackContaining(expected)
        }
    }

    @Test
    fun i64EqualsZero() = parser.with {
        val instruction = "i64.eqz".parseInstruction()

        var resultContext = instruction.execute(executionContextWithOpStack(0L.toValue()))
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(executionContextWithOpStack(1L.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-1L).toValue()))
        assertThat(resultContext).hasOpStackContaining(0.toValue())
    }

    @Test
    fun i64Equals() = parser.with {
        val instruction = "i64.eq".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42L.toValue(), 42L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42L.toValue(), 41L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41L.toValue(), 42L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())
    }

    @Test
    fun i64NotEquals() = parser.with {
        val instruction = "i64.ne".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42L.toValue(), 42L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42L.toValue(), 41L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41L.toValue(), 42L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())
    }

    @Test
    fun i64LessThanSigned() = parser.with {
        val instruction = "i64.lt_s".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42L.toValue(), 42L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42L.toValue(), 41L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41L.toValue(), 42L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41L).toValue(), 42L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42L).toValue(), (-41L).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())
    }

    @Test
    fun i64LessThanUnsigned() = parser.with {
        val instruction = "i64.lt_u".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42L.toValue(), 42L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42L.toValue(), 41L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41L.toValue(), 42L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41L).toValue(), 42L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42L).toValue(), (-41L).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0L.toValue(), (-1L).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())
    }

    @Test
    fun i64GreaterThanSigned() = parser.with {
        val instruction = "i64.gt_s".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42L.toValue(), 42L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42L.toValue(), 41L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41L.toValue(), 42L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41L).toValue(), 42L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42L).toValue(), (-41L).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())
    }

    @Test
    fun i64GreaterThanUnsigned() = parser.with {
        val instruction = "i64.gt_u".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42L.toValue(), 42L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42L.toValue(), 41L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41L.toValue(), 42L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41L).toValue(), 42L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42L).toValue(), (-41L).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0L.toValue(), (-1L).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())
    }

    @Test
    fun i64LessThanEqualToSigned() = parser.with {
        val instruction = "i64.le_s".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42L.toValue(), 42L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42L.toValue(), 41L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41L.toValue(), 42L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41L).toValue(), 42L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42L).toValue(), (-41L).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())
    }

    @Test
    fun i64LessThanEqualToUnsigned() = parser.with {
        val instruction = "i64.le_u".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42L.toValue(), 42L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42L.toValue(), 41L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41L.toValue(), 42L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41L).toValue(), 42L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42L).toValue(), (-41L).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0L.toValue(), (-1L).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())
    }

    @Test
    fun i64GreaterThanEqualToSigned() = parser.with {
        val instruction = "i64.ge_s".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42L.toValue(), 42L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42L.toValue(), 41L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41L.toValue(), 42L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41L).toValue(), 42L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42L).toValue(), (-41L).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())
    }

    @Test
    fun i64GreaterThanEqualToUnsigned() = parser.with {
        val instruction = "i64.ge_u".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42L.toValue(), 42L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42L.toValue(), 41L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41L.toValue(), 42L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41L).toValue(), 42L.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42L).toValue(), (-41L).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0L.toValue(), (-1L).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())
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
    fun f32Add() = parser.with {
        val instruction = "f32.add".parseInstruction()
        var resultContext: ExecutionContext

        // NaNs.

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), 1f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1f.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue(), 1f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1f.toValue(), (-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue(), (-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        // Infinites

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                1f.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                1f.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                1f.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                1f.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        // Zeroes

        resultContext = instruction.execute(
            executionContextWithOpStack(0f.toValue(), (-0f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0f.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0f).toValue(), 0f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0f.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0f.toValue(), 0f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0f.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0f).toValue(), (-0f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0f).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0f.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42f.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0f).toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42f.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 0f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42f.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), (-0f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42f.toValue())

        // Regular

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), (-42f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0f.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((42f + 42f).toValue())
    }

    @Test
    fun f32Sub() = parser.with {
        val instruction = "f32.sub".parseInstruction()
        var resultContext: ExecutionContext

        // NaNs.

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), 1f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1f.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue(), 1f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1f.toValue(), (-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue(), (-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        // Infinites

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                1f.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                1f.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                1f.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                1f.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        // Zeroes

        resultContext = instruction.execute(
            executionContextWithOpStack(0f.toValue(), (-0f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0f.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0f).toValue(), 0f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0f).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0f.toValue(), 0f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0f.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0f).toValue(), (-0f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0f.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0f.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-42f).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0f).toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-42f).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 0f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42f.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), (-0f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42f.toValue())

        // Regular

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0f.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42f).toValue(), (-42f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0f.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 12f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((42f - 12f).toValue())
    }

    @Test
    fun f32Mul() = parser.with {
        val instruction = "f32.mul".parseInstruction()
        var resultContext: ExecutionContext

        // NaNs.

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), 1f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1f.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue(), 1f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1f.toValue(), (-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue(), (-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        // Infinites

        resultContext = instruction.execute(
            executionContextWithOpStack(0f.toValue(), Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0f.toValue(), Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0f).toValue(), Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0f).toValue(), Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue(), 0f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue(), 0f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue(), (-0f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue(), (-0f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue(), (-2f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue(), 2f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-2f).toValue(), Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2f.toValue(), Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue(), 2f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue(), (-2f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2f.toValue(), Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-2f).toValue(), Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        // Zeroes

        resultContext = instruction.execute(
            executionContextWithOpStack(0f.toValue(), 0f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0f.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0f).toValue(), (-0f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0f.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0f.toValue(), (-0f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0f).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0f).toValue(), 0f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0f).toValue())

        // Regular

        resultContext = instruction.execute(
            executionContextWithOpStack(2f.toValue(), 3f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((2f * 3f).toValue())
    }

    @Test
    fun f32Div() = parser.with {
        val instruction = "f32.div".parseInstruction()
        var resultContext: ExecutionContext

        // NaNs

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), 1f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1f.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue(), 1f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1f.toValue(), (-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue(), (-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        // Infinites

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue(), (-42f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue(), (-42f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0f.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42f).toValue(), Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0f.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42f).toValue(), Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0f).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0f).toValue())

        // Zeroes

        resultContext = instruction.execute(
            executionContextWithOpStack(0f.toValue(), 0f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0f).toValue(), (-0f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0f.toValue(), (-0f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0f).toValue(), 0f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0f.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0f.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0f).toValue(), (-42f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0f.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0f.toValue(), (-42f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0f).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0f).toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0f).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 0f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42f).toValue(), (-0f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42f).toValue(), 0f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), (-0f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        // Regular

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((42f / 42f).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 12f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((42f / 12f).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(12f.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((12f / 42f).toValue())
    }

    @Test
    fun f32Min() = parser.with {
        val instruction = "f32.min".parseInstruction()
        var resultContext: ExecutionContext

        // NaNs

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), 1f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1f.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue(), 1f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1f.toValue(), (-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue(), (-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        // Infinites

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42f.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42f.toValue())

        // Zeroes

        resultContext = instruction.execute(
            executionContextWithOpStack(0f.toValue(), (-0f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0f).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0f).toValue(), 0f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0f).toValue())

        // Regular

        resultContext = instruction.execute(
            executionContextWithOpStack((-42f).toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-42f).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), (-42f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-42f).toValue())
    }

    @Test
    fun f32Max() = parser.with {
        val instruction = "f32.max".parseInstruction()
        var resultContext: ExecutionContext

        // NaNs

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), 1f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1f.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue(), 1f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1f.toValue(), (-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue(), (-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        // Infinites

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42f.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42f.toValue())

        // Zeroes

        resultContext = instruction.execute(
            executionContextWithOpStack(0f.toValue(), (-0f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0f.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0f).toValue(), 0f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0f.toValue())

        // Regular

        resultContext = instruction.execute(
            executionContextWithOpStack((-42f).toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42f.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), (-42f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42f.toValue())
    }

    @Test
    fun f32CopySign() = parser.with {
        val instruction = "f32.copysign".parseInstruction()
        var resultContext: ExecutionContext

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 1f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42f.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42f).toValue(), (-1f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-42f).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), (-1f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-42f).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42f).toValue(), 1f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42f.toValue())
    }

    @Test
    fun f32Equals() = parser.with {
        val instruction = "f32.eq".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 41f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41f.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())
    }

    @Test
    fun f32NotEquals() = parser.with {
        val instruction = "f32.ne".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 41f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41f.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())
    }

    @Test
    fun f32LessThan() = parser.with {
        val instruction = "f32.lt".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 41f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41f.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41f).toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42f).toValue(), (-41f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        //  Check infinity cases
        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())
    }

    @Test
    fun f32GreaterThan() = parser.with {
        val instruction = "f32.gt".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 41f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41f.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41f).toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42f).toValue(), (-41f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        //  Check infinity cases
        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())
    }

    @Test
    fun f32LessThanEqualTo() = parser.with {
        val instruction = "f32.le".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 41f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41f.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41f).toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42f).toValue(), (-41f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        //  Check infinity cases
        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())
    }

    @Test
    fun f32GreaterThanEqualTo() = parser.with {
        val instruction = "f32.ge".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 41f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41f.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41f).toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42f).toValue(), (-41f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        //  Check infinity cases
        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())
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
    fun f64Add() = parser.with {
        val instruction = "f64.add".parseInstruction()
        var resultContext: ExecutionContext

        // NaNs.

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), 1.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1.0.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue(), 1.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1.0.toValue(), (-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue(), (-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        // Infinites

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                1.0.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                1.0.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                1.0.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                1.0.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        // Zeroes

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42.0.toValue())

        // Regular

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), (-42.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((42.0 + 42.0).toValue())
    }

    @Test
    fun f64Sub() = parser.with {
        val instruction = "f64.sub".parseInstruction()
        var resultContext: ExecutionContext

        // NaNs.

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), 1.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1.0.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue(), 1.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1.0.toValue(), (-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue(), (-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        // Infinites

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                1.0.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                1.0.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                1.0.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                1.0.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        // Zeroes

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-42.0).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-42.0).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42.0.toValue())

        // Regular

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42.0).toValue(), (-42.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 12.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((42.0 - 12.0).toValue())
    }

    @Test
    fun f64Mul() = parser.with {
        val instruction = "f64.mul".parseInstruction()
        var resultContext: ExecutionContext

        // NaNs.

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), 1.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1.0.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue(), 1.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1.0.toValue(), (-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue(), (-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        // Infinites

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue(), (-2.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue(), 2.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-2.0).toValue(), Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2.0.toValue(), Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue(), 2.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue(), (-2.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2.0.toValue(), Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-2.0).toValue(), Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        // Zeroes

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        // Regular

        resultContext = instruction.execute(
            executionContextWithOpStack(2.0.toValue(), 3.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((2.0 * 3.0).toValue())
    }

    @Test
    fun f64Div() = parser.with {
        val instruction = "f64.div".parseInstruction()
        var resultContext: ExecutionContext

        // NaNs

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), 1.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1.0.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue(), 1.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1.0.toValue(), (-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue(), (-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        // Infinites

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue(), (-42.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue(), (-42.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42.0).toValue(), Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42.0).toValue(), Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        // Zeroes

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), (-42.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), (-42.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42.0).toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42.0).toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        // Regular

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((42.0 / 42.0).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 12.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((42.0 / 12.0).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(12.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((12.0 / 42.0).toValue())
    }

    @Test
    fun f64Min() = parser.with {
        val instruction = "f64.min".parseInstruction()
        var resultContext: ExecutionContext

        // NaNs

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), 1.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1.0.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue(), 1.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1.0.toValue(), (-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue(), (-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        // Infinites

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42.0.toValue())

        // Zeroes

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        // Regular

        resultContext = instruction.execute(
            executionContextWithOpStack((-42.0).toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-42.0).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), (-42.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-42.0).toValue())
    }

    @Test
    fun f64Max() = parser.with {
        val instruction = "f64.max".parseInstruction()
        var resultContext: ExecutionContext

        // NaNs

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), 1.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1.0.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue(), 1.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1.0.toValue(), (-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue(), (-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        // Infinites

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42.0.toValue())

        // Zeroes

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        // Regular

        resultContext = instruction.execute(
            executionContextWithOpStack((-42.0).toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), (-42.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42.0.toValue())
    }

    @Test
    fun f64CopySign() = parser.with {
        val instruction = "f64.copysign".parseInstruction()
        var resultContext: ExecutionContext

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 1.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42.0).toValue(), (-1.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-42.0).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), (-1.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-42.0).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42.0).toValue(), 1.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42.0.toValue())
    }

    @Test
    fun f64Equals() = parser.with {
        val instruction = "f64.eq".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 41.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())
    }

    @Test
    fun f64NotEquals() = parser.with {
        val instruction = "f64.ne".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 41.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())
    }

    @Test
    fun f64LessThan() = parser.with {
        val instruction = "f64.lt".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 41.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41.0).toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42.0).toValue(), (-41.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        //  Check infinity cases
        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())
    }

    @Test
    fun f64GreaterThan() = parser.with {
        val instruction = "f64.gt".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 41.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41.0).toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42.0).toValue(), (-41.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        //  Check infinity cases
        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())
    }

    @Test
    fun f64LessThanEqualTo() = parser.with {
        val instruction = "f64.le".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 41.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41.0).toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42.0).toValue(), (-41.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        //  Check infinity cases
        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())
    }

    @Test
    fun f64GreaterThanEqualTo() = parser.with {
        val instruction = "f64.ge".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 41.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41.0).toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42.0).toValue(), (-41.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        //  Check infinity cases
        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())
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

    @Test
    fun testOp_throws_ifStackEmpty() {
        assertThrows(IllegalStateException::class.java) {
            testOp<IntValue>(executionContext) {
                false
            }
        }
    }

    @Test
    fun testOp_throws_ifStackTopInvalidType() {
        assertThrows(KWasmRuntimeException::class.java) {
            testOp<FloatValue>(executionContextWithOpStack(42.toValue())) {
                false
            }
        }.also {
            assertThat(it).hasMessageThat().contains("Top of stack is invalid type")
        }
    }

    @Test
    fun testOp_valid() {
        val result = testOp<IntValue>(executionContextWithOpStack(42.toValue())) {
            true
        }
        assertThat(result).hasOpStackContaining(1.toValue())
    }

    @Test
    fun relOp_throws_ifStackDoesntHaveEnoughOperands() {
        assertThrows(IllegalStateException::class.java) {
            relOp<IntValue>(executionContext) { _, _ -> false }
        }
        assertThrows(IllegalStateException::class.java) {
            relOp<IntValue>(executionContextWithOpStack(1.toValue())) { _, _ -> false }
        }
    }

    @Test
    fun relOp_throws_ifStackHasWrongTypes() {
        assertThrows(KWasmRuntimeException::class.java) {
            relOp<IntValue>(executionContextWithOpStack(1.toValue(), 1L.toValue())) { _, _ ->
                false
            }
        }.also {
            assertThat(it).hasMessageThat().contains("RHS is invalid type")
        }
        assertThrows(KWasmRuntimeException::class.java) {
            relOp<IntValue>(executionContextWithOpStack(1L.toValue(), 1.toValue())) { _, _ ->
                false
            }
        }.also {
            assertThat(it).hasMessageThat().contains("LHS is invalid type")
        }
    }

    @Test
    fun relOp_valid() {
        var result = relOp<IntValue>(
            executionContextWithOpStack(42.toValue(), (-42).toValue())
        ) { _, _ -> true }
        assertThat(result).hasOpStackContaining(1.toValue())

        result = relOp<IntValue>(
            executionContextWithOpStack(42.toValue(), (-42).toValue())
        ) { _, _ -> false }
        assertThat(result).hasOpStackContaining(0.toValue())
    }

    @Test
    fun binaryOp_throws_ifStackDoesntHaveEnoughOperands() {
        assertThrows(IllegalStateException::class.java) {
            binaryOp<IntValue>(executionContext) { _, _ -> 1.toValue() }
        }
        assertThrows(IllegalStateException::class.java) {
            binaryOp<IntValue>(executionContextWithOpStack(1.toValue())) { _, _ -> 1.toValue() }
        }
    }

    @Test
    fun binaryOp_throws_ifStackHasWrongTypes() {
        assertThrows(KWasmRuntimeException::class.java) {
            binaryOp<IntValue>(executionContextWithOpStack(1.toValue(), 1L.toValue())) { _, _ ->
                1.toValue()
            }
        }.also {
            assertThat(it).hasMessageThat().contains("RHS is invalid type")
        }
        assertThrows(KWasmRuntimeException::class.java) {
            binaryOp<IntValue>(executionContextWithOpStack(1L.toValue(), 1.toValue())) { _, _ ->
                1.toValue()
            }
        }.also {
            assertThat(it).hasMessageThat().contains("LHS is invalid type")
        }
    }

    @Test
    fun binaryOp_valid() {
        val result = binaryOp<IntValue>(
            executionContextWithOpStack(42.toValue(), (-42).toValue())
        ) { x, y -> (x.value + y.value).toValue() }
        assertThat(result).hasOpStackContaining(0.toValue())
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
