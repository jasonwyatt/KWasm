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

import kwasm.ast.instruction.ControlInstruction
import kwasm.ast.instruction.Instruction
import kwasm.ast.instruction.MemoryInstruction
import kwasm.ast.instruction.NumericConstantInstruction
import kwasm.ast.instruction.NumericInstruction
import kwasm.ast.instruction.ParametricInstruction
import kwasm.ast.instruction.VariableInstruction
import kwasm.runtime.ExecutionContext

/**
 * Executes the receiving [Instruction] by multiplexing to implementation-specific variants.
 */
internal fun Instruction.execute(
    context: ExecutionContext
): ExecutionContext = when (this) {
    is NumericConstantInstruction<*> -> this.execute(context)
    is VariableInstruction -> this.execute(context)
    is NumericInstruction -> this.execute(context)
    is MemoryInstruction -> this.execute(context)
    is ParametricInstruction -> this.execute(context)
    is ControlInstruction -> this.execute(context)
    else -> TODO("Instruction: $this not supported yet.")
}

/** Executes a sequence of [Instruction]s. */
internal fun List<Instruction>.execute(context: ExecutionContext): ExecutionContext =
    fold(context) { soFar, instruction ->
        // Capture the activation/label stack heights before execution.
        val activationHeightBefore = soFar.stacks.activations.height
        val labelHeightBefore = soFar.stacks.labels.height

        // Execute the instruction
        val resultContext = instruction.execute(soFar)

        if (resultContext.stacks.activations.height < activationHeightBefore) {
            // No need to continue executing after a return (would've popped from the activation
            // stack)
            return resultContext
        }
        if (resultContext.stacks.labels.height != labelHeightBefore) {
            // No need to continue executing after a break (would've popped from the label stack)
            return resultContext
        }
        // Continue execution
        resultContext
    }
