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

package kwasm.validation2

import kwasm.ast.AstNode
import kwasm.ast.Identifier
import kwasm.ast.module.Index
import kwasm.validation.ValidationContext

interface InstructionSequenceState {
    val context: ValidationContext.FunctionBody

    fun pushOperand(type: ValidationValueType)

    fun popOperand(): ValidationValueType

    fun popOperand(expected: ValidationValueType): ValidationValueType

    fun pushOperands(types: List<ValidationValueType>)

    fun popOperands(expectedTypes: List<ValidationValueType>)

    fun pushControlFrame(
        opcode: AstNode,
        inOperands: List<ValidationValueType>,
        outOperands: List<ValidationValueType>
    )

    fun popControlFrame(): ControlFrame

    fun markUnreachable()

    fun findFrame(labelIndex: Index<Identifier.Label>): ControlFrame
}
