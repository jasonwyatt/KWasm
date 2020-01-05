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

package kwasm.ast.instruction

import kwasm.ast.type.ValueType.F32
import kwasm.ast.type.ValueType.F64
import kwasm.ast.type.ValueType.I32
import kwasm.ast.type.ValueType.I64

/**
 * Defines the various numeric [Instruction] variants.
 *
 * See more in [the docs](https://webassembly.github.io/spec/core/exec/numerics.html).
 */
sealed class NumericInstruction : Instruction {
    /** i32.clz */
    object I32CountLeadingZeroes :
        NumericInstruction(),
        UnaryOperation<I32, I32>
    /** i32.ctz */
    object I32CountTrailingZeroes :
        NumericInstruction(),
        UnaryOperation<I32, I32>
    /** i32.popcnt */
    object I32CountNonZeroBits :
        NumericInstruction(),
        UnaryOperation<I32, I32>
    /** i32.add */
    object I32Add :
        NumericInstruction(),
        BinaryOperation<I32, I32, I32>
    /** i32.sub */
    object I32Subtract :
        NumericInstruction(),
        BinaryOperation<I32, I32, I32>
    /** i32.mul */
    object I32Multiply :
        NumericInstruction(),
        BinaryOperation<I32, I32, I32>
    /** i32.div_s */
    object I32DivideSigned :
        NumericInstruction(),
        BinaryOperation<I32, I32, I32>
    /** i32.div_u */
    object I32DivideUnsigned :
        NumericInstruction(),
        BinaryOperation<I32, I32, I32>
    /** i32.rem_s */
    object I32RemainderSigned :
        NumericInstruction(),
        BinaryOperation<I32, I32, I32>
    /** i32.rem_u */
    object I32RemainderUnsigned :
        NumericInstruction(),
        BinaryOperation<I32, I32, I32>
    /** i32.and */
    object I32BitwiseAnd :
        NumericInstruction(),
        BinaryOperation<I32, I32, I32>
    /** i32.or */
    object I32BitwiseOr :
        NumericInstruction(),
        BinaryOperation<I32, I32, I32>
    /** i32.xor */
    object I32BitwiseXor :
        NumericInstruction(),
        BinaryOperation<I32, I32, I32>
    /** i32.shl */
    object I32ShiftLeft :
        NumericInstruction(),
        BinaryOperation<I32, I32, I32>
    /** i32.shr_s */
    object I32ShiftRightSigned :
        NumericInstruction(),
        BinaryOperation<I32, I32, I32>
    /** i32.shr_u */
    object I32ShiftRightUnsigned :
        NumericInstruction(),
        BinaryOperation<I32, I32, I32>
    /** i32.rotl */
    object I32RotateLeft :
        NumericInstruction(),
        BinaryOperation<I32, I32, I32>
    /** i32.rotr */
    object I32RotateRight :
        NumericInstruction(),
        BinaryOperation<I32, I32, I32>
    /** i32.eqz */
    object I32EqualsZero :
        NumericInstruction(),
        TestInstruction<I32>
    /** i32.eq */
    object I32Equals :
        NumericInstruction(),
        Comparison<I32, I32>
    /** i32.ne */
    object I32NotEquals :
        NumericInstruction(),
        Comparison<I32, I32>
    /** i32.lt_s */
    object I32LessThanSigned :
        NumericInstruction(),
        Comparison<I32, I32>
    /** i32.lt_u */
    object I32LessThanUnsigned :
        NumericInstruction(),
        Comparison<I32, I32>
    /** i32.gt_s */
    object I32GreaterThanSigned :
        NumericInstruction(),
        Comparison<I32, I32>
    /** i32.gt_u */
    object I32GreaterThanUnsigned :
        NumericInstruction(),
        Comparison<I32, I32>
    /** i32.le_s */
    object I32LessThanEqualToSigned :
        NumericInstruction(),
        Comparison<I32, I32>
    /** i32.le_u */
    object I32LessThanEqualToUnsigned :
        NumericInstruction(),
        Comparison<I32, I32>
    /** i32.ge_s */
    object I32GreaterThanEqualToSigned :
        NumericInstruction(),
        Comparison<I32, I32>
    /** i32.ge_u */
    object I32GreaterThanEqualToUnsigned :
        NumericInstruction(),
        Comparison<I32, I32>

    /** i64.clz */
    object I64CountLeadingZeroes :
        NumericInstruction(),
        UnaryOperation<I64, I64>
    /** i64.ctz */
    object I64CountTrailingZeroes :
        NumericInstruction(),
        UnaryOperation<I64, I64>
    /** i64.popcnt */
    object I64CountNonZeroBits :
        NumericInstruction(),
        UnaryOperation<I64, I64>
    /** i64.add */
    object I64Add :
        NumericInstruction(),
        BinaryOperation<I64, I64, I64>
    /** i64.sub */
    object I64Subtract :
        NumericInstruction(),
        BinaryOperation<I64, I64, I64>
    /** i64.mul */
    object I64Multiply :
        NumericInstruction(),
        BinaryOperation<I64, I64, I64>
    /** i64.div_s */
    object I64DivideSigned :
        NumericInstruction(),
        BinaryOperation<I64, I64, I64>
    /** i64.div_u */
    object I64DivideUnsigned :
        NumericInstruction(),
        BinaryOperation<I64, I64, I64>
    /** i64.rem_s */
    object I64RemainderSigned :
        NumericInstruction(),
        BinaryOperation<I64, I64, I64>
    /** i64.rem_u */
    object I64RemainderUnsigned :
        NumericInstruction(),
        BinaryOperation<I64, I64, I64>
    /** i64.and */
    object I64BitwiseAnd :
        NumericInstruction(),
        BinaryOperation<I64, I64, I64>
    /** i64.or */
    object I64BitwiseOr :
        NumericInstruction(),
        BinaryOperation<I64, I64, I64>
    /** i64.xor */
    object I64BitwiseXor :
        NumericInstruction(),
        BinaryOperation<I64, I64, I64>
    /** i64.shl */
    object I64ShiftLeft :
        NumericInstruction(),
        BinaryOperation<I64, I64, I64>
    /** i64.shr_s */
    object I64ShiftRightSigned :
        NumericInstruction(),
        BinaryOperation<I64, I64, I64>
    /** i64.shr_u */
    object I64ShiftRightUnsigned :
        NumericInstruction(),
        BinaryOperation<I64, I64, I64>
    /** i64.rotl */
    object I64RotateLeft :
        NumericInstruction(),
        BinaryOperation<I64, I64, I64>
    /** i64.rotr */
    object I64RotateRight :
        NumericInstruction(),
        BinaryOperation<I64, I64, I64>
    /** i64.eqz */
    object I64EqualsZero :
        NumericInstruction(),
        TestInstruction<I64>
    /** i64.eq */
    object I64Equals :
        NumericInstruction(),
        Comparison<I64, I64>
    /** i64.eq */
    object I64NotEquals :
        NumericInstruction(),
        Comparison<I64, I64>
    /** i64.lt_s */
    object I64LessThanSigned :
        NumericInstruction(),
        Comparison<I64, I64>
    /** i64.lt_u */
    object I64LessThanUnsigned :
        NumericInstruction(),
        Comparison<I64, I64>
    /** i64.gt_s */
    object I64GreaterThanSigned :
        NumericInstruction(),
        Comparison<I64, I64>
    /** i64.gt_u */
    object I64GreaterThanUnsigned :
        NumericInstruction(),
        Comparison<I64, I64>
    /** i64.le_s */
    object I64LessThanEqualToSigned :
        NumericInstruction(),
        Comparison<I64, I64>
    /** i64.le_u */
    object I64LessThanEqualToUnsigned :
        NumericInstruction(),
        Comparison<I64, I64>
    /** i64.ge_s */
    object I64GreaterThanEqualToSigned :
        NumericInstruction(),
        Comparison<I64, I64>
    /** i64.ge_u */
    object I64GreaterThanEqualToUnsigned :
        NumericInstruction(),
        Comparison<I64, I64>

    /** f32.abs */
    object F32AbsoluteValue :
        NumericInstruction(),
        UnaryOperation<F32, F32>
    /** f32.neg */
    object F32Negative :
        NumericInstruction(),
        UnaryOperation<F32, F32>
    /** f32.ceil */
    object F32Ceiling :
        NumericInstruction(),
        UnaryOperation<F32, F32>
    /** f32.floor */
    object F32Floor :
        NumericInstruction(),
        UnaryOperation<F32, F32>
    /** f32.trunc */
    object F32Truncate :
        NumericInstruction(),
        UnaryOperation<F32, F32>
    /** f32.nearest */
    object F32Nearest :
        NumericInstruction(),
        UnaryOperation<F32, F32>
    /** f32.sqrt */
    object F32SquareRoot :
        NumericInstruction(),
        UnaryOperation<F32, F32>
    /** f32.add */
    object F32Add :
        NumericInstruction(),
        BinaryOperation<F32, F32, F32>
    /** f32.sub */
    object F32Subtract :
        NumericInstruction(),
        BinaryOperation<F32, F32, F32>
    /** f32.mul */
    object F32Multiply :
        NumericInstruction(),
        BinaryOperation<F32, F32, F32>
    /** f32.div */
    object F32Divide :
        NumericInstruction(),
        BinaryOperation<F32, F32, F32>
    /** f32.min */
    object F32Min :
        NumericInstruction(),
        BinaryOperation<F32, F32, F32>
    /** f32.max */
    object F32Max :
        NumericInstruction(),
        BinaryOperation<F32, F32, F32>
    /** f32.copysign */
    object F32CopySign :
        NumericInstruction(),
        BinaryOperation<F32, F32, F32>
    /** f32.eq */
    object F32Equals :
        NumericInstruction(),
        Comparison<F32, F32>
    /** f32.ne */
    object F32NotEquals :
        NumericInstruction(),
        Comparison<F32, F32>
    /** f32.lt */
    object F32LessThan :
        NumericInstruction(),
        Comparison<F32, F32>
    /** f32.gt */
    object F32GreaterThan :
        NumericInstruction(),
        Comparison<F32, F32>
    /** f32.le */
    object F32LessThanEqualTo :
        NumericInstruction(),
        Comparison<F32, F32>
    /** f32.ge */
    object F32GreaterThanEqualTo :
        NumericInstruction(),
        Comparison<F32, F32>

    /** f64.abs */
    object F64AbsoluteValue :
        NumericInstruction(),
        UnaryOperation<F64, F64>
    /** f64.neg */
    object F64Negative :
        NumericInstruction(),
        UnaryOperation<F64, F64>
    /** f64.ceil */
    object F64Ceiling :
        NumericInstruction(),
        UnaryOperation<F64, F64>
    /** f64.floor */
    object F64Floor :
        NumericInstruction(),
        UnaryOperation<F64, F64>
    /** f64.trunc */
    object F64Truncate :
        NumericInstruction(),
        UnaryOperation<F64, F64>
    /** f64.nearest */
    object F64Nearest :
        NumericInstruction(),
        UnaryOperation<F64, F64>
    /** f64.sqrt */
    object F64SquareRoot :
        NumericInstruction(),
        UnaryOperation<F64, F64>
    /** f64.add */
    object F64Add :
        NumericInstruction(),
        BinaryOperation<F64, F64, F64>
    /** f64.sub */
    object F64Subtract :
        NumericInstruction(),
        BinaryOperation<F64, F64, F64>
    /** f64.mul */
    object F64Multiply :
        NumericInstruction(),
        BinaryOperation<F64, F64, F64>
    /** f64.div */
    object F64Divide :
        NumericInstruction(),
        BinaryOperation<F64, F64, F64>
    /** f64.min */
    object F64Min :
        NumericInstruction(),
        BinaryOperation<F64, F64, F64>
    /** f64.max */
    object F64Max :
        NumericInstruction(),
        BinaryOperation<F64, F64, F64>
    /** f64.copysign */
    object F64CopySign :
        NumericInstruction(),
        BinaryOperation<F64, F64, F64>
    /** f64.eq */
    object F64Equals :
        NumericInstruction(),
        Comparison<F64, F64>
    /** f64.ne */
    object F64NotEquals :
        NumericInstruction(),
        Comparison<F64, F64>
    /** f64.lt */
    object F64LessThan :
        NumericInstruction(),
        Comparison<F64, F64>
    /** f64.gt */
    object F64GreaterThan :
        NumericInstruction(),
        Comparison<F64, F64>
    /** f64.le */
    object F64LessThanEqualTo :
        NumericInstruction(),
        Comparison<F64, F64>
    /** f64.ge */
    object F64GreaterThanEqualTo :
        NumericInstruction(),
        Comparison<F64, F64>

    /*
     * Conversions
     */

    /** i32.wrap_i64 */
    object I32WrapI64 :
        NumericInstruction(),
        Conversion<I64, I32>
    /** i32.trunc_f32_s */
    object I32TruncateF32Signed :
        NumericInstruction(),
        Conversion<F32, I32>
    /** i32.trunc_f32_u */
    object I32TruncateF32Unsigned :
        NumericInstruction(),
        Conversion<F32, I32>
    /** i32.trunc_f64_s */
    object I32TruncateF64Signed :
        NumericInstruction(),
        Conversion<F64, I32>
    /** i32.trunc_f64_u */
    object I32TruncateF64Unsigned :
        NumericInstruction(),
        Conversion<F64, I32>
    /** i32.reinterpret_f32 */
    object I32ReinterpretF32 :
        NumericInstruction(),
        Conversion<F32, I32>

    /** i64.extend_i32_s */
    object I64ExtendI32Signed :
        NumericInstruction(),
        Conversion<I32, I64>
    /** i64.extend_i32_u */
    object I64ExtendI32Unsigned :
        NumericInstruction(),
        Conversion<I32, I64>
    /** i64.trunc_f32_s */
    object I64TruncateF32Signed :
        NumericInstruction(),
        Conversion<F32, I64>
    /** i64.trunc_f32_u */
    object I64TruncateF32Unsigned :
        NumericInstruction(),
        Conversion<F32, I64>
    /** i64.trunc_f64_s */
    object I64TruncateF64Signed :
        NumericInstruction(),
        Conversion<F64, I64>
    /** i64.trunc_f64_u */
    object I64TruncateF64Unsigned :
        NumericInstruction(),
        Conversion<F64, I64>
    /** i64.reinterpret_f64 */
    object I64ReinterpretF64 :
        NumericInstruction(),
        Conversion<F64, I64>

    /** f32.convert_i32_s */
    object F32ConvertI32Signed :
        NumericInstruction(),
        Conversion<I32, F32>
    /** f32.convert_i32_u */
    object F32ConvertI32Unsigned :
        NumericInstruction(),
        Conversion<I32, F32>
    /** f32.convert_i64_s */
    object F32ConvertI64Signed :
        NumericInstruction(),
        Conversion<I64, F32>
    /** f32.convert_i64_u */
    object F32ConvertI64Unsigned :
        NumericInstruction(),
        Conversion<I64, F32>
    /** f32.demote_f64 */
    object F32DemoteF64 :
        NumericInstruction(),
        Conversion<F64, F32>
    /** f32.reinterpret_i32 */
    object F32ReinterpretI32 :
        NumericInstruction(),
        Conversion<I32, F32>

    /** f64.convert_i32_s */
    object F64ConvertI32Signed :
        NumericInstruction(),
        Conversion<I32, F64>
    /** f64.convert_i32_u */
    object F64ConvertI32Unsigned :
        NumericInstruction(),
        Conversion<I32, F64>
    /** f64.convert_i64_s */
    object F64ConvertI64Signed :
        NumericInstruction(),
        Conversion<I64, F64>
    /** f64.convert_i64_u */
    object F64ConvertI64Unsigned :
        NumericInstruction(),
        Conversion<I64, F64>
    /** f64.promote_f32 */
    object F64PromoteF32 :
        NumericInstruction(),
        Conversion<F32, F64>
    /** f64.reinterpret_i64 */
    object F64ReinterpretI64 :
        NumericInstruction(),
        Conversion<I64, F64>
}
