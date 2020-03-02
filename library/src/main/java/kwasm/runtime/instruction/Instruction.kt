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

import kwasm.ast.instruction.Instruction
import kwasm.ast.instruction.NumericConstantInstruction
import kwasm.ast.instruction.VariableInstruction
import kwasm.runtime.ExecutionContext

internal fun Instruction.execute(
    context: ExecutionContext
): ExecutionContext = when (this) {
    is NumericConstantInstruction<*> -> this.execute(context)
    is VariableInstruction -> this.execute(context)
    else -> TODO("Instruction: $this not supported yet.")
}
