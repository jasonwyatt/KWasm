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

package kwasm.validation2.instruction.control

import kwasm.ast.instruction.ControlInstruction
import kwasm.validation.ValidationContext
import kwasm.validation.ValidationException
import kwasm.validation.validate
import kwasm.validation.validateNotNull
import kwasm.validation2.InstructionSequenceState
import kwasm.validation2.InstructionValidator
import kwasm.validation2.ValidationValueType
import kwasm.validation2.instruction.validate
import kwasm.validation2.toValidationValueType

object ControlInstructionValidator : InstructionValidator<ControlInstruction> {
    override fun validate(
        instruction: ControlInstruction,
        context: ValidationContext.FunctionBody,
        state: InstructionSequenceState
    ) {
        when (instruction) {
            is ControlInstruction.Block -> {
                val inputTypes = instruction.getInputTypes(context)
                val outputTypes = instruction.getOutputTypes(context)

                state.popOperands(inputTypes)
                state.pushControlFrame(instruction, inputTypes, outputTypes)

                instruction.instructions.validate(context, state)

                val frame = state.popControlFrame()
                validate(
                    condition = frame.opcode === instruction,
                    parseContext = null,
                    message = ""
                )
                state.pushOperands(frame.endTypes)
            }
            is ControlInstruction.Loop -> {
                val inputTypes = instruction.getInputTypes(context)
                val outputTypes = instruction.getOutputTypes(context)

                state.popOperands(inputTypes)
                state.pushControlFrame(instruction, inputTypes, outputTypes)

                instruction.instructions.validate(context, state)

                val frame = state.popControlFrame()
                validate(
                    condition = frame.opcode === instruction,
                    parseContext = null,
                    message = ""
                )
                state.pushOperands(frame.endTypes)
            }
            is ControlInstruction.If -> {
                val inputTypes = instruction.getInputTypes(context)
                val outputTypes = instruction.getOutputTypes(context)

                state.popOperands(inputTypes)
                // Positive
                if (instruction.positiveInstructions.isNotEmpty()) {
                    state.pushControlFrame(instruction, inputTypes, outputTypes)
                    instruction.positiveInstructions.validate(context, state)
                    val frame = state.popControlFrame()
                    validate(
                        condition = frame.opcode === instruction,
                        parseContext = null,
                        message = ""
                    )
                }
                // Negative
                if (instruction.negativeInstructions.isNotEmpty()) {
                    state.pushControlFrame(instruction, inputTypes, outputTypes)
                    instruction.negativeInstructions.validate(context, state)
                    val frame = state.popControlFrame()
                    validate(
                        condition = frame.opcode === instruction,
                        parseContext = null,
                        message = ""
                    )
                }

                // Finally, push the end types.
                state.pushOperands(outputTypes)
            }
            is ControlInstruction.StartBlock,
            is ControlInstruction.StartIf,
            is ControlInstruction.EndBlock ->
                throw ValidationException("Start/End markers not supported at validation time.")
            ControlInstruction.Unreachable -> state.markUnreachable()
            ControlInstruction.NoOp -> Unit
            is ControlInstruction.Break -> {
                val frame = state.findFrame(instruction.labelIndex)
                state.popOperands(frame.labelTypes)
                state.markUnreachable()
            }
            is ControlInstruction.BreakIf -> {
                val frame = state.findFrame(instruction.labelIndex)
                state.popOperand(ValidationValueType.I32)
                state.popOperands(frame.labelTypes)
                state.pushOperands(frame.labelTypes)
            }
            is ControlInstruction.BreakTable -> {
                val frame = state.findFrame(instruction.defaultTarget)
                instruction.targets.forEach {
                    val targetFrame = state.findFrame(it)
                    validate(
                        condition = frame.labelTypes == targetFrame.labelTypes,
                        parseContext = null,
                        message = "Mismatch output types for labels"
                    )
                }
                state.popOperand(ValidationValueType.I32)
                state.popOperands(frame.labelTypes)
                state.markUnreachable()
            }
            ControlInstruction.Return -> {
                val resultType = validateNotNull(
                    value = context.returnType,
                    parseContext = null,
                    message = "Result type must not be null"
                )
                // TODO: need multi-value return types.
                val expected =
                    resultType.result?.let {
                        listOf(it.valType.toValidationValueType())
                    } ?: emptyList()
                state.popOperands(expected)
                state.markUnreachable()
            }
            is ControlInstruction.Call -> {
                val function = validateNotNull(
                    value = context.functions[instruction.functionIndex],
                    parseContext = null,
                    message = "function not found for ${instruction.functionIndex}"
                )

                val expected =
                    function.functionType.parameters.map { it.valType.toValidationValueType() }
                val output =
                    function.functionType.returnValueEnums.map {
                        it.valType.toValidationValueType()
                    }

                state.popOperands(expected)
                state.pushOperands(output)
            }
            is ControlInstruction.CallIndirect -> {
                
            }
        }
    }
}
