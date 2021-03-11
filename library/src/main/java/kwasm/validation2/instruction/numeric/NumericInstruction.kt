/*
 * Copyright 2021 Google LLC
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

package kwasm.validation2.instruction.numeric

import kwasm.ast.instruction.NumericInstruction
import kwasm.ast.type.ValueType
import kwasm.validation.FunctionBodyValidationVisitor
import kwasm.validation.ValidationContext
import kwasm.validation.validate
import kwasm.validation2.InstructionSequenceState
import kwasm.validation2.InstructionValidator
import kwasm.validation2.toValidationValueType

/**
 * Validates [NumericInstruction] nodes.
 *
 * From
 * [the docs](https://webassembly.github.io/spec/core/valid/instructions.html#numeric-instructions):
 *
 * ```
 *   t.unop
 * ```
 * * The [NumericInstruction] is valid with type `\[t] => \[t]`.
 *
 * ```
 *   t.binop
 * ```
 * * The [NumericInstruction] is valid with type `[t t] => \[t]`.
 *
 * ```
 *   t.testop
 * ```
 * * The [NumericInstruction] is valid with type `\[t] => \[i32]`.
 *
 * ```
 *   t.relop
 * ```
 * * The [NumericInstruction] is valid with type `[t t] => \[i32]`.
 *
 * ```
 *   t2.cvtop_t1_sx?
 * ```
 * * The [NumericInstruction] is valid with type `\[t1] => \[t2]`.
 */
object NumericInstructionValidator : InstructionValidator<NumericInstruction> {
    override fun validate(
        instruction: NumericInstruction,
        context: ValidationContext.FunctionBody,
        state: InstructionSequenceState
    ) {
        val (inputs, output) = instruction.getExpectedInsAndOuts()

        state.popOperands(inputs.map { it.toValidationValueType() })
        state.pushOperand(output.toValidationValueType())
    }
}

internal fun NumericInstruction.getExpectedInsAndOuts(): Pair<List<ValueType>, ValueType> =
    when (this) {
        NumericInstruction.I32CountLeadingZeroes,
        NumericInstruction.I32CountTrailingZeroes,
        NumericInstruction.I32EqualsZero,
        NumericInstruction.I32CountNonZeroBits -> listOf(ValueType.I32) to ValueType.I32
        NumericInstruction.I32Add,
        NumericInstruction.I32Subtract,
        NumericInstruction.I32Multiply,
        NumericInstruction.I32DivideSigned,
        NumericInstruction.I32DivideUnsigned,
        NumericInstruction.I32RemainderSigned,
        NumericInstruction.I32RemainderUnsigned,
        NumericInstruction.I32BitwiseAnd,
        NumericInstruction.I32BitwiseOr,
        NumericInstruction.I32BitwiseXor,
        NumericInstruction.I32ShiftLeft,
        NumericInstruction.I32ShiftRightSigned,
        NumericInstruction.I32ShiftRightUnsigned,
        NumericInstruction.I32RotateLeft,
        NumericInstruction.I32RotateRight,
        NumericInstruction.I32Equals,
        NumericInstruction.I32NotEquals,
        NumericInstruction.I32LessThanSigned,
        NumericInstruction.I32LessThanUnsigned,
        NumericInstruction.I32GreaterThanSigned,
        NumericInstruction.I32GreaterThanUnsigned,
        NumericInstruction.I32LessThanEqualToSigned,
        NumericInstruction.I32LessThanEqualToUnsigned,
        NumericInstruction.I32GreaterThanEqualToSigned,
        NumericInstruction.I32GreaterThanEqualToUnsigned -> listOf(ValueType.I32, ValueType.I32) to ValueType.I32

        NumericInstruction.I64CountLeadingZeroes,
        NumericInstruction.I64CountTrailingZeroes,
        NumericInstruction.I64EqualsZero,
        NumericInstruction.I64CountNonZeroBits -> listOf(ValueType.I64) to ValueType.I64
        NumericInstruction.I64Add,
        NumericInstruction.I64Subtract,
        NumericInstruction.I64Multiply,
        NumericInstruction.I64DivideSigned,
        NumericInstruction.I64DivideUnsigned,
        NumericInstruction.I64RemainderSigned,
        NumericInstruction.I64RemainderUnsigned,
        NumericInstruction.I64BitwiseAnd,
        NumericInstruction.I64BitwiseOr,
        NumericInstruction.I64BitwiseXor,
        NumericInstruction.I64ShiftLeft,
        NumericInstruction.I64ShiftRightSigned,
        NumericInstruction.I64ShiftRightUnsigned,
        NumericInstruction.I64RotateLeft,
        NumericInstruction.I64RotateRight -> listOf(ValueType.I64, ValueType.I64) to ValueType.I64
        NumericInstruction.I64Equals,
        NumericInstruction.I64NotEquals,
        NumericInstruction.I64LessThanSigned,
        NumericInstruction.I64LessThanUnsigned,
        NumericInstruction.I64GreaterThanSigned,
        NumericInstruction.I64GreaterThanUnsigned,
        NumericInstruction.I64LessThanEqualToSigned,
        NumericInstruction.I64LessThanEqualToUnsigned,
        NumericInstruction.I64GreaterThanEqualToSigned,
        NumericInstruction.I64GreaterThanEqualToUnsigned -> listOf(ValueType.I64, ValueType.I64) to ValueType.I32

        NumericInstruction.F32AbsoluteValue,
        NumericInstruction.F32Negative,
        NumericInstruction.F32Ceiling,
        NumericInstruction.F32Floor,
        NumericInstruction.F32Truncate,
        NumericInstruction.F32Nearest,
        NumericInstruction.F32SquareRoot -> listOf(ValueType.F32) to ValueType.F32
        NumericInstruction.F32Add,
        NumericInstruction.F32Subtract,
        NumericInstruction.F32Multiply,
        NumericInstruction.F32Divide,
        NumericInstruction.F32Min,
        NumericInstruction.F32Max,
        NumericInstruction.F32CopySign -> listOf(ValueType.F32, ValueType.F32) to ValueType.F32
        NumericInstruction.F32Equals,
        NumericInstruction.F32NotEquals,
        NumericInstruction.F32LessThan,
        NumericInstruction.F32GreaterThan,
        NumericInstruction.F32LessThanEqualTo,
        NumericInstruction.F32GreaterThanEqualTo -> listOf(ValueType.F32, ValueType.F32) to ValueType.I32

        NumericInstruction.F64AbsoluteValue,
        NumericInstruction.F64Negative,
        NumericInstruction.F64Ceiling,
        NumericInstruction.F64Floor,
        NumericInstruction.F64Truncate,
        NumericInstruction.F64Nearest,
        NumericInstruction.F64SquareRoot -> listOf(ValueType.F64) to ValueType.F64
        NumericInstruction.F64Add,
        NumericInstruction.F64Subtract,
        NumericInstruction.F64Multiply,
        NumericInstruction.F64Divide,
        NumericInstruction.F64Min,
        NumericInstruction.F64Max,
        NumericInstruction.F64CopySign -> listOf(ValueType.F64, ValueType.F64) to ValueType.F64
        NumericInstruction.F64Equals,
        NumericInstruction.F64NotEquals,
        NumericInstruction.F64LessThan,
        NumericInstruction.F64GreaterThan,
        NumericInstruction.F64LessThanEqualTo,
        NumericInstruction.F64GreaterThanEqualTo -> listOf(ValueType.F64, ValueType.F64) to ValueType.I32

        NumericInstruction.I32WrapI64 -> listOf(ValueType.I64) to ValueType.I32
        NumericInstruction.I32TruncateF32Signed,
        NumericInstruction.I32ReinterpretF32,
        NumericInstruction.I32TruncateF32Unsigned -> listOf(ValueType.F32) to ValueType.I32
        NumericInstruction.I32TruncateF64Signed,
        NumericInstruction.I32TruncateF64Unsigned -> listOf(ValueType.F64) to ValueType.I32

        NumericInstruction.I64ExtendI32Signed,
        NumericInstruction.I64ExtendI32Unsigned -> listOf(ValueType.I32) to ValueType.I64
        NumericInstruction.I64TruncateF32Signed,
        NumericInstruction.I64TruncateF32Unsigned -> listOf(ValueType.F32) to ValueType.I64
        NumericInstruction.I64TruncateF64Signed,
        NumericInstruction.I64TruncateF64Unsigned,
        NumericInstruction.I64ReinterpretF64 -> listOf(ValueType.F64) to ValueType.I64

        NumericInstruction.F32ConvertI32Signed,
        NumericInstruction.F32ConvertI32Unsigned,
        NumericInstruction.F32ReinterpretI32 -> listOf(ValueType.I32) to ValueType.F32
        NumericInstruction.F32ConvertI64Signed,
        NumericInstruction.F32ConvertI64Unsigned -> listOf(ValueType.I64) to ValueType.F32
        NumericInstruction.F32DemoteF64 -> listOf(ValueType.F64) to ValueType.F32

        NumericInstruction.F64ConvertI32Signed,
        NumericInstruction.F64ConvertI32Unsigned -> listOf(ValueType.I32) to ValueType.F64
        NumericInstruction.F64ConvertI64Signed,
        NumericInstruction.F64ConvertI64Unsigned,
        NumericInstruction.F64ReinterpretI64 -> listOf(ValueType.I64) to ValueType.F64
        NumericInstruction.F64PromoteF32 -> listOf(ValueType.F32) to ValueType.F64

        NumericInstruction.I32Extend8Signed,
        NumericInstruction.I32Extend16Signed -> listOf(ValueType.I32) to ValueType.I32

        NumericInstruction.I64Extend8Signed,
        NumericInstruction.I64Extend16Signed,
        NumericInstruction.I64Extend32Signed -> listOf(ValueType.I64) to ValueType.I64

        NumericInstruction.I32TruncateSaturatedF32Signed,
        NumericInstruction.I32TruncateSaturatedF32Unsigned -> listOf(ValueType.F32) to ValueType.I32
        NumericInstruction.I32TruncateSaturatedF64Signed,
        NumericInstruction.I32TruncateSaturatedF64Unsigned -> listOf(ValueType.F64) to ValueType.I32
        NumericInstruction.I64TruncateSaturatedF32Signed,
        NumericInstruction.I64TruncateSaturatedF32Unsigned -> listOf(ValueType.F32) to ValueType.I64
        NumericInstruction.I64TruncateSaturatedF64Signed,
        NumericInstruction.I64TruncateSaturatedF64Unsigned -> listOf(ValueType.F64) to ValueType.I64
    }
