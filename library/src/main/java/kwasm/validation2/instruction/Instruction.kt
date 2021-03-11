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

import kwasm.ast.instruction.Instruction
import kwasm.ast.instruction.MemoryInstruction
import kwasm.ast.instruction.NumericConstantInstruction
import kwasm.ast.instruction.NumericInstruction
import kwasm.ast.instruction.ParametricInstruction
import kwasm.ast.instruction.VariableInstruction
import kwasm.validation.ValidationContext
import kwasm.validation2.InstructionSequenceState
import kwasm.validation2.instruction.memory.validate
import kwasm.validation2.instruction.numeric.NumericConstantInstructionValidator
import kwasm.validation2.instruction.numeric.NumericInstructionValidator

fun Instruction.validate(context: ValidationContext.FunctionBody, state: InstructionSequenceState) {
    when (this) {
        is MemoryInstruction -> validate(context, state)
        is NumericInstruction -> NumericInstructionValidator.validate(this, context, state)
        is NumericConstantInstruction<*> ->
            NumericConstantInstructionValidator.validate(this, context, state)
        is ParametricInstruction ->
            ParametricInstructionValidator.validate(this, context, state)
        is VariableInstruction ->
            VariableInstructionValidator.validate(this, context, state)
    }
}

/**
 * Validator of sequences of [Instruction] nodes.
 *
 * From
 * [the docs](https://webassembly.github.io/spec/core/valid/instructions.html#instruction-sequences):
 *
 * Typing of instruction sequences is defined recursively.
 *
 * Empty Instruction Sequence: `ϵ`
 * * The empty instruction sequence is valid with type `[t*] => [t*]`, for any sequence of value
 *   types `t*`.
 *
 * Non-empty Instruction Sequence: `instr* instrN`
 * * The instruction sequence `instr*` must be valid with type `[t*^1] => [t*2]`, for some sequences
 *   of value types `t*^1` and `t*^2`.
 * * The instruction `instrN` must be valid with type `[t*] => [t*^3]`, for some sequences of value
 *   types `t*` and `t*^3`.
 * * There must be a sequence of value types `t*^0`, such that `t*^2 = t*^0 t*`.
 * * Then the combined instruction sequence is valid with type `[t*^1] => [t*^0 t*^3]`.
 */
fun List<Instruction>.validate(
    context: ValidationContext.FunctionBody,
    state: InstructionSequenceState
) = forEach { it.validate(context, state) }
