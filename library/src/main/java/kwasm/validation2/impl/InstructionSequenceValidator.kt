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

package kwasm.validation2.impl

import kwasm.ast.AstNode
import kwasm.validation.ValidationContext
import kwasm.validation2.ControlFrame
import kwasm.validation2.InstructionSequenceState
import kwasm.validation2.ValidationValueType
import kwasm.validation2.ValidationValueType.UNKNOWN
import kwasm.validation2.util.validationCheck

class InstructionSequenceValidatorImpl(
    override val context: ValidationContext.FunctionBody
) : InstructionSequenceState {
    private val operandStack = mutableListOf<ValidationValueType>()
    private val controlStack = mutableListOf<ControlFrame>()

    override fun pushOperand(type: ValidationValueType) {
        operandStack.add(type)
    }

    override fun popOperand(): ValidationValueType {
        val frame = controlStack.last()
        if (operandStack.size == frame.height && frame.unreachable) return UNKNOWN
        validationCheck(
            predicate = operandStack.size != frame.height,
            errorMessage = "Cannot pop out of current frame"
        )
        return operandStack.removeLast()
    }

    override fun popOperand(expected: ValidationValueType): ValidationValueType {
        val actual = popOperand()
        if (actual == UNKNOWN) return expected
        if (expected == UNKNOWN) return actual
        validationCheck(
            predicate = actual == expected,
            errorMessage = "Unexpected value at top of stack: $actual, expected: $expected"
        )
        return actual
    }

    override fun pushOperands(types: List<ValidationValueType>) = types.forEach(this::pushOperand)

    override fun popOperands(expectedTypes: List<ValidationValueType>) =
        expectedTypes.asReversed().forEach { popOperand(it) }

    override fun pushControlFrame(
        opcode: AstNode,
        inOperands: List<ValidationValueType>,
        outOperands: List<ValidationValueType>
    ) {
        val frame = ControlFrame(
            opcode = opcode,
            startTypes = inOperands,
            endTypes = outOperands,
            height = operandStack.size,
            unreachable = false
        )
        controlStack.add(frame)
        pushOperands(inOperands)
    }

    override fun popControlFrame(): ControlFrame {
        validationCheck(
            predicate = controlStack.isNotEmpty(),
            errorMessage = "Not in a control frame!"
        )

        val frame = controlStack.last()
        popOperands(frame.endTypes)

        validationCheck(
            predicate = operandStack.size == frame.height,
            errorMessage = "Leftover values on the stack after exiting control frame"
        )
        return controlStack.removeLast()
    }

    override fun markUnreachable() {
        val currentFrame = controlStack.last()
        while (operandStack.size > currentFrame.height) {
            operandStack.removeLast()
        }
        currentFrame.unreachable = true
    }
}
