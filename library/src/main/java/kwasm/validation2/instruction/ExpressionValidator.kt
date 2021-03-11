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

package kwasm.validation2.instruction

import kwasm.ast.instruction.Expression
import kwasm.ast.instruction.Instruction
import kwasm.ast.instruction.NumericConstantInstruction
import kwasm.ast.instruction.VariableInstruction
import kwasm.ast.type.ValueType
import kwasm.validation.ValidationContext
import kwasm.validation.ValidationVisitor
import kwasm.validation.validate
import kwasm.validation2.InstructionSequenceState
import kwasm.validation2.InstructionValidator
import kwasm.validation2.ValidationValueType
import kwasm.validation2.toValidationValueType

/**
 * Validator of [Expression] nodes.
 *
 * From [the docs](https://webassembly.github.io/spec/core/valid/instructions.html#expressions):
 *
 * Expressions `expr` are classified by result types of the form `[t?]`.
 *
 * ```
 *   instr* end
 * ```
 * * The instruction sequence `instr*` must be valid with type `[] => [t?]`, for some optional
 *   value type `t?`.
 * * Then the expression is valid with result type `[t?]`.
 *
 * ### Constant Expressions
 *
 * * In a constant expression `instr* end` all instructions in `instr*` must be constant.
 * * A constant instruction `instr` must be:
 *    * either of the form `t.const c`,
 *    * or of the form `global.get x`, in which case `C.globals\[x]` must be a global type of the
 *      form `const t`.
 */
object ExpressionValidator {
    fun validate(
        expression: Expression,
        isConstant: Boolean,
        expectedResult: ValidationValueType?,
        context: ValidationContext.FunctionBody,
        state: InstructionSequenceState
    ) {
        validate(
            !isConstant || expression.instructions.all { it.isConstant(context) },
            parseContext = null,
            message = "Constant expressions may only contain ((i|f)(32|64)).const and " +
                "global.get instructions operating on non-mutable global values. " +
                "(constant expression required)"
        )

        expression.instructions.validate(context, state)

        if (expectedResult != null) {
            state.popOperand(expectedResult)
            state.pushOperand(expectedResult)
        }
    }

    private fun Instruction.isConstant(context: ValidationContext): Boolean {
        if (this is NumericConstantInstruction<*>) return true
        if (this !is VariableInstruction.GlobalGet) return false
        return context.globals[this.valueAstNode]?.mutable == false
    }
}
