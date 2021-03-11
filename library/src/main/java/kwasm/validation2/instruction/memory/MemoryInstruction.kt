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

package kwasm.validation2.instruction.memory

import kwasm.ast.instruction.MemoryInstruction
import kwasm.validation.ValidationContext
import kwasm.validation2.InstructionSequenceState

fun MemoryInstruction.validate(
    context: ValidationContext.FunctionBody,
    state: InstructionSequenceState
) {
    when (this) {
        is MemoryInstruction.LoadInt -> LoadIntValidator.validate(this, context, state)
        is MemoryInstruction.StoreInt -> StoreIntValidator.validate(this, context, state)
        is MemoryInstruction.LoadFloat -> LoadFloatValidator.validate(this, context, state)
        is MemoryInstruction.StoreFloat -> StoreFloatValidator.validate(this, context, state)
        is MemoryInstruction.Size -> SizeValidator.validate(this, context, state)
        is MemoryInstruction.Grow -> GrowValidator.validate(this, context, state)
    }
}
