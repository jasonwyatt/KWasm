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

package kwasm.ast

/**
 * Base for all numeric [Constant] [Instruction] implementations.
 *
 * See
 * [the docs](https://webassembly.github.io/spec/core/syntax/instructions.html#numeric-instructions)
 * for more information.
 */
sealed class NumericConstantInstruction : Instruction {
    /**
     * Represents a 32-bit integer constant instruction.
     *
     * Even though [I32] can technically hold unsigned 32-bit integers as well, use
     * [Int] here for simplicity, and if the number needs to be unsigned - convert it
     * at the use-site.
     */
    data class I32(
        override val valueAstNode: Literal<Int>
    ) : NumericConstantInstruction(), Constant<Int>

    /**
     * Represents a 64-bit integer constant instruction.
     *
     * Even though [I64] can technically hold unsigned 64-bit integers as well, use
     * [Long] here for simplicity, and if the number needs to be unsigned - convert it
     * at the use-site.
     */
    data class I64(
        override val valueAstNode: Literal<Long>
    ) : NumericConstantInstruction(), Constant<Long>

    /**
     * Represents a 32-bit floating-point constant instruction.
     */
    data class F32(
        override val valueAstNode: Literal<Float>
    ) : NumericConstantInstruction(), Constant<Float>

    /**
     * Represents a 64-bit floating-point constant instruction.
     */
    data class F64(
        override val valueAstNode: Literal<Double>
    ) : NumericConstantInstruction(), Constant<Double>
}