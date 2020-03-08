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

@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")

package kwasm.runtime.instruction

import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.sqrt
import kotlin.math.truncate
import kwasm.KWasmRuntimeException
import kwasm.ast.instruction.NumericInstruction
import kwasm.runtime.DoubleValue
import kwasm.runtime.ExecutionContext
import kwasm.runtime.FloatValue
import kwasm.runtime.IntValue
import kwasm.runtime.LongValue
import kwasm.runtime.Value
import kwasm.runtime.toValue

/**
 * See
 * [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#numeric-instructions):
 */
internal fun NumericInstruction.execute(context: ExecutionContext): ExecutionContext {
    when (this) {
        NumericInstruction.I32Add -> TODO()
        NumericInstruction.I32CountLeadingZeroes -> unaryOp(context) { x: IntValue ->
            if (x.value == 0) 32.toValue()
            else {
                var xVal = x.unsignedValue
                var total = 0
                var startWidth = 16
                var mask = 0xFFFF0000u
                while (startWidth > 0) {
                    if (xVal and mask == 0u) {
                        xVal = xVal shl startWidth
                        total += startWidth
                    }
                    startWidth = startWidth ushr 1
                    mask = mask shl startWidth
                }
                total.toValue()
            }
        }
        NumericInstruction.I32CountTrailingZeroes -> unaryOp(context) { x: IntValue ->
            if (x.value == 0) 32.toValue()
            else {
                var xVal = x.unsignedValue
                var total = 0
                var endWidth = 16
                var mask = 0xFFFFu
                while (endWidth > 0) {
                    if (xVal and mask == 0u) {
                        xVal = xVal shr endWidth
                        total += endWidth
                    }
                    endWidth = endWidth ushr 1
                    mask = mask shr endWidth
                }
                total.toValue()
            }
        }
        NumericInstruction.I32CountNonZeroBits -> unaryOp(context) { x: IntValue ->
            when (x.value) {
                0 -> 0.toValue()
                -1 -> 32.toValue()
                else -> {
                    var mask = 1
                    var total = 0
                    repeat(31) {
                        if (mask and x.value != 0) total++
                        mask = mask shl 1
                    }
                    total.toValue()
                }
            }
        }
        NumericInstruction.I32Subtract -> TODO()
        NumericInstruction.I32Multiply -> TODO()
        NumericInstruction.I32DivideSigned -> TODO()
        NumericInstruction.I32DivideUnsigned -> TODO()
        NumericInstruction.I32RemainderSigned -> TODO()
        NumericInstruction.I32RemainderUnsigned -> TODO()
        NumericInstruction.I32BitwiseAnd -> TODO()
        NumericInstruction.I32BitwiseOr -> TODO()
        NumericInstruction.I32BitwiseXor -> TODO()
        NumericInstruction.I32ShiftLeft -> TODO()
        NumericInstruction.I32ShiftRightSigned -> TODO()
        NumericInstruction.I32ShiftRightUnsigned -> TODO()
        NumericInstruction.I32RotateLeft -> TODO()
        NumericInstruction.I32RotateRight -> TODO()
        NumericInstruction.I32EqualsZero -> testOp(context) { x: IntValue -> x.value == 0 }
        NumericInstruction.I32Equals -> relOp<IntValue>(context) { x, y -> x.value == y.value }
        NumericInstruction.I32NotEquals -> relOp<IntValue>(context) { x, y -> x.value != y.value }
        NumericInstruction.I32LessThanSigned -> relOp<IntValue>(context) { x, y ->
            x.value < y.value
        }
        NumericInstruction.I32LessThanUnsigned -> relOp<IntValue>(context) { x, y ->
            x.unsignedValue < y.unsignedValue
        }
        NumericInstruction.I32GreaterThanSigned -> relOp<IntValue>(context) { x, y ->
            x.value > y.value
        }
        NumericInstruction.I32GreaterThanUnsigned -> relOp<IntValue>(context) { x, y ->
            x.unsignedValue > y.unsignedValue
        }
        NumericInstruction.I32LessThanEqualToSigned -> relOp<IntValue>(context) { x, y ->
            x.value <= y.value
        }
        NumericInstruction.I32LessThanEqualToUnsigned -> relOp<IntValue>(context) { x, y ->
            x.unsignedValue <= y.unsignedValue
        }
        NumericInstruction.I32GreaterThanEqualToSigned -> relOp<IntValue>(context) { x, y ->
            x.value >= y.value
        }
        NumericInstruction.I32GreaterThanEqualToUnsigned -> relOp<IntValue>(context) { x, y ->
            x.unsignedValue >= y.unsignedValue
        }
        NumericInstruction.I64CountLeadingZeroes -> unaryOp(context) { x: LongValue ->
            if (x.value == 0L) 64L.toValue()
            else {
                var xVal = x.unsignedValue
                var total = 0L
                var mask = 0xFFFFFFFF00000000uL
                var startWidth = 32
                while (startWidth > 0) {
                    if (xVal and mask == 0uL) {
                        xVal = xVal shl startWidth
                        total += startWidth
                    }
                    startWidth = startWidth ushr 1
                    mask = mask shl startWidth
                }
                total.toValue()
            }
        }
        NumericInstruction.I64CountTrailingZeroes -> unaryOp(context) { x: LongValue ->
            if (x.value == 0L) 64L.toValue()
            else {
                var xVal = x.unsignedValue
                var total = 0L
                var endWidth = 32
                var mask = 0xFFFFFFFFuL
                while (endWidth > 0) {
                    if (xVal and mask == 0uL) {
                        xVal = xVal shr endWidth
                        total += endWidth
                    }
                    endWidth = endWidth ushr 1
                    mask = mask shr endWidth
                }
                total.toValue()
            }
        }
        NumericInstruction.I64CountNonZeroBits -> unaryOp(context) { x: LongValue ->
            when (x.value) {
                0L -> 0L.toValue()
                -1L -> 64L.toValue()
                else -> {
                    var mask = 1L
                    var total = 0L
                    repeat(63) {
                        if (mask and x.value != 0L) total++
                        mask = mask shl 1
                    }
                    total.toValue()
                }
            }
        }
        NumericInstruction.I64Add -> TODO()
        NumericInstruction.I64Subtract -> TODO()
        NumericInstruction.I64Multiply -> TODO()
        NumericInstruction.I64DivideSigned -> TODO()
        NumericInstruction.I64DivideUnsigned -> TODO()
        NumericInstruction.I64RemainderSigned -> TODO()
        NumericInstruction.I64RemainderUnsigned -> TODO()
        NumericInstruction.I64BitwiseAnd -> TODO()
        NumericInstruction.I64BitwiseOr -> TODO()
        NumericInstruction.I64BitwiseXor -> TODO()
        NumericInstruction.I64ShiftLeft -> TODO()
        NumericInstruction.I64ShiftRightSigned -> TODO()
        NumericInstruction.I64ShiftRightUnsigned -> TODO()
        NumericInstruction.I64RotateLeft -> TODO()
        NumericInstruction.I64RotateRight -> TODO()
        NumericInstruction.I64EqualsZero -> testOp(context) { x: LongValue -> x.value == 0L }
        NumericInstruction.I64Equals -> relOp<LongValue>(context) { x, y -> x.value == y.value }
        NumericInstruction.I64NotEquals -> relOp<LongValue>(context) { x, y -> x.value != y.value }
        NumericInstruction.I64LessThanSigned -> relOp<LongValue>(context) { x, y ->
            x.value < y.value
        }
        NumericInstruction.I64LessThanUnsigned -> relOp<LongValue>(context) { x, y ->
            x.unsignedValue < y.unsignedValue
        }
        NumericInstruction.I64GreaterThanSigned -> relOp<LongValue>(context) { x, y ->
            x.value > y.value
        }
        NumericInstruction.I64GreaterThanUnsigned -> relOp<LongValue>(context) { x, y ->
            x.unsignedValue > y.unsignedValue
        }
        NumericInstruction.I64LessThanEqualToSigned -> relOp<LongValue>(context) { x, y ->
            x.value <= y.value
        }
        NumericInstruction.I64LessThanEqualToUnsigned -> relOp<LongValue>(context) { x, y ->
            x.unsignedValue <= y.unsignedValue
        }
        NumericInstruction.I64GreaterThanEqualToSigned -> relOp<LongValue>(context) { x, y ->
            x.value >= y.value
        }
        NumericInstruction.I64GreaterThanEqualToUnsigned -> relOp<LongValue>(context) { x, y ->
            x.unsignedValue >= y.unsignedValue
        }
        NumericInstruction.F32AbsoluteValue -> unaryOp(context) { x: FloatValue ->
            x.value.absoluteValue.toValue()
        }
        NumericInstruction.F32Negative -> unaryOp(context) { x: FloatValue ->
            (-x.value).toValue()
        }
        NumericInstruction.F32Ceiling -> unaryOp(context) { x: FloatValue ->
            ceil(x.value).toValue()
        }
        NumericInstruction.F32Floor -> unaryOp(context) { x: FloatValue ->
            floor(x.value).toValue()
        }
        NumericInstruction.F32Truncate -> unaryOp(context) { x: FloatValue ->
            truncate(x.value).toValue()
        }
        NumericInstruction.F32Nearest -> unaryOp(context) { x: FloatValue ->
            round(x.value).toValue()
        }
        NumericInstruction.F32SquareRoot -> unaryOp(context) { x: FloatValue ->
            sqrt(x.value).toValue()
        }
        NumericInstruction.F32Add -> TODO()
        NumericInstruction.F32Subtract -> TODO()
        NumericInstruction.F32Multiply -> TODO()
        NumericInstruction.F32Divide -> TODO()
        NumericInstruction.F32Min -> TODO()
        NumericInstruction.F32Max -> TODO()
        NumericInstruction.F32CopySign -> TODO()
        NumericInstruction.F32Equals -> TODO()
        NumericInstruction.F32NotEquals -> TODO()
        NumericInstruction.F32LessThan -> TODO()
        NumericInstruction.F32GreaterThan -> TODO()
        NumericInstruction.F32LessThanEqualTo -> TODO()
        NumericInstruction.F32GreaterThanEqualTo -> TODO()
        NumericInstruction.F64AbsoluteValue -> unaryOp(context) { x: DoubleValue ->
            x.value.absoluteValue.toValue()
        }
        NumericInstruction.F64Negative -> unaryOp(context) { x: DoubleValue ->
            (-x.value).toValue()
        }
        NumericInstruction.F64Ceiling -> unaryOp(context) { x: DoubleValue ->
            ceil(x.value).toValue()
        }
        NumericInstruction.F64Floor -> unaryOp(context) { x: DoubleValue ->
            floor(x.value).toValue()
        }
        NumericInstruction.F64Truncate -> unaryOp(context) { x: DoubleValue ->
            truncate(x.value).toValue()
        }
        NumericInstruction.F64Nearest -> unaryOp(context) { x: DoubleValue ->
            round(x.value).toValue()
        }
        NumericInstruction.F64SquareRoot -> unaryOp(context) { x: DoubleValue ->
            sqrt(x.value).toValue()
        }
        NumericInstruction.F64Add -> TODO()
        NumericInstruction.F64Subtract -> TODO()
        NumericInstruction.F64Multiply -> TODO()
        NumericInstruction.F64Divide -> TODO()
        NumericInstruction.F64Min -> TODO()
        NumericInstruction.F64Max -> TODO()
        NumericInstruction.F64CopySign -> TODO()
        NumericInstruction.F64Equals -> TODO()
        NumericInstruction.F64NotEquals -> TODO()
        NumericInstruction.F64LessThan -> TODO()
        NumericInstruction.F64GreaterThan -> TODO()
        NumericInstruction.F64LessThanEqualTo -> TODO()
        NumericInstruction.F64GreaterThanEqualTo -> TODO()
        NumericInstruction.I32WrapI64 -> TODO()
        NumericInstruction.I32TruncateF32Signed -> TODO()
        NumericInstruction.I32TruncateF32Unsigned -> TODO()
        NumericInstruction.I32TruncateF64Signed -> TODO()
        NumericInstruction.I32TruncateF64Unsigned -> TODO()
        NumericInstruction.I32ReinterpretF32 -> TODO()
        NumericInstruction.I64ExtendI32Signed -> TODO()
        NumericInstruction.I64ExtendI32Unsigned -> TODO()
        NumericInstruction.I64TruncateF32Signed -> TODO()
        NumericInstruction.I64TruncateF32Unsigned -> TODO()
        NumericInstruction.I64TruncateF64Signed -> TODO()
        NumericInstruction.I64TruncateF64Unsigned -> TODO()
        NumericInstruction.I64ReinterpretF64 -> TODO()
        NumericInstruction.F32ConvertI32Signed -> TODO()
        NumericInstruction.F32ConvertI32Unsigned -> TODO()
        NumericInstruction.F32ConvertI64Signed -> TODO()
        NumericInstruction.F32ConvertI64Unsigned -> TODO()
        NumericInstruction.F32DemoteF64 -> TODO()
        NumericInstruction.F32ReinterpretI32 -> TODO()
        NumericInstruction.F64ConvertI32Signed -> TODO()
        NumericInstruction.F64ConvertI32Unsigned -> TODO()
        NumericInstruction.F64ConvertI64Signed -> TODO()
        NumericInstruction.F64ConvertI64Unsigned -> TODO()
        NumericInstruction.F64PromoteF32 -> TODO()
        NumericInstruction.F64ReinterpretI64 -> TODO()
    }
    return context
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#exec-unop):
 *
 * ```
 *   t.unop
 * ```
 *
 * 1. Assert: due to validation, a value of value type `t` is on the top of the stack.
 * 1. Pop the value `t.const c_1` from the stack.
 * 1. If `unopt(c_1)` is defined, then:
 *    * Let `c` be a possible result of computing `unopt(c_1)`.
 *    * Push the value `t.const c` to the stack.
 * 1. Else:
 *    * Trap.
 *
 * Also functions as the conversion operator util.
 *
 * From [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#exec-cvtop):
 *
 * ```
 *   t2.cvtop_t1_sx?
 * ```
 *
 * 1. Assert: due to validation, a value of value type `t1` is on the top of the stack.
 * 1. Pop the value `t1.const c1` from the stack.
 * 1. If `cvtop^sx?_t1,_t2(c1)` is defined:
 *    * Let `c_2` be a possible result of computing `cvtop^sx?_t1,_t2(c_1)`.
 *    * Push the value `t_2.const c_2` to the stack.
 * 1. Else:
 *    * Trap.
 */
internal inline fun <reified In : Value<*>, reified Out : Value<*>> unaryOp(
    executionContext: ExecutionContext,
    crossinline op: (In) -> Out
): ExecutionContext {
    val stackTop = executionContext.stacks.operands.pop()
    if (stackTop !is In) throw KWasmRuntimeException("Top of stack is invalid type")

    val newTop = op(stackTop)
    executionContext.stacks.operands.push(newTop)

    return executionContext
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#exec-testop):
 *
 * ```
 *   t.testop
 * ```
 *
 * 1. Assert: due to validation, a value of value type `t` is on the top of the stack.
 * 1. Pop the value `t.const c_1` from the stack.
 * 1. Let `c` be the result of computing `testopt(c_1)`.
 * 1. Push the value `i32.const c` to the stack.
 */
internal inline fun <reified In : Value<*>> testOp(
    executionContext: ExecutionContext,
    crossinline op: (In) -> Boolean
): ExecutionContext {
    val stackTop = executionContext.stacks.operands.pop()
    if (stackTop !is In) throw KWasmRuntimeException("Top of stack is invalid type")

    val testResult = op(stackTop)
    executionContext.stacks.operands.push(
        if (testResult) 1.toValue() else 0.toValue()
    )

    return executionContext
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#exec-relop):
 *
 * ```
 *   t.relop
 * ```
 *
 * 1. Assert: due to validation, two values of value type `t` are on the top of the stack.
 * 1. Pop the value `t.const c_2` from the stack.
 * 1. Pop the value `t.const c_1` from the stack.
 * 1. Let `c` be the result of computing `relopt(c_1, c_2)`.
 * 1. Push the value `i32.const c` to the stack.
 */
internal inline fun <reified In : Value<*>> relOp(
    executionContext: ExecutionContext,
    crossinline op: (In, In) -> Boolean
): ExecutionContext {
    val arg2 = executionContext.stacks.operands.pop()
    if (arg2 !is In) throw KWasmRuntimeException("RHS is invalid type")

    val arg1 = executionContext.stacks.operands.pop()
    if (arg1 !is In) throw KWasmRuntimeException("LHS is invalid type")

    val result = op(arg1, arg2)
    executionContext.stacks.operands.push(
        if (result) 1.toValue() else 0.toValue()
    )

    return executionContext
}
