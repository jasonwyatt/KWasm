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

import kwasm.ast.instruction.ParametricInstruction
import kwasm.ast.type.ValueType
import kwasm.validation.ValidationContext
import kwasm.validation2.InstructionSequenceState
import kwasm.validation2.InstructionValidator
import kwasm.validation2.ValidationValueType

/**
 * Validator of [ParametricInstruction] nodes.
 *
 * From [the docs](https://webassembly.github.io/spec/core/valid/instructions.html#parametric-instructions):
 *
 * [ParametricInstruction.Drop]
 * * The instruction is valid with type `\[t] =>[]`, for any value type `t`.
 *
 * [ParametricInstruction.Select]
 * * The instruction is valid with type `[t t i32] => \[t]`, for any value type `t`.
 */
object ParametricInstructionValidator : InstructionValidator<ParametricInstruction> {
    override fun validate(
        instruction: ParametricInstruction,
        context: ValidationContext.FunctionBody,
        state: InstructionSequenceState
    ) = when(instruction) {
        is ParametricInstruction.Drop -> validateDrop(state)
        is ParametricInstruction.Select -> validateSelect(state)
    }

    private fun validateDrop(state: InstructionSequenceState) {
        state.popOperand()
    }

    private fun validateSelect(state: InstructionSequenceState) {
        state.popOperand(ValidationValueType.I32)
        val second = state.popOperand()
        val first = state.popOperand()
        kwasm.validation.validate(
            condition = first == second,
            parseContext = null,
            message = "Select expects the 2nd and 3rd ValueTypes to be the same, but " +
                "$first != $second"
        )
        state.pushOperand(first)
    }
}
