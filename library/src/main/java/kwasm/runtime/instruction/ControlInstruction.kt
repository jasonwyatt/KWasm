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

import kwasm.KWasmRuntimeException
import kwasm.ast.instruction.ControlInstruction
import kwasm.ast.type.ValueType
import kwasm.runtime.ExecutionContext
import kwasm.runtime.stack.Label
import kwasm.runtime.toValueType

/**
 * Executes the receiving [ControlInstruction].
 */
internal fun ControlInstruction.execute(
    context: ExecutionContext
): ExecutionContext = when (this) {
    is ControlInstruction.Block -> this.execute(context)
    is ControlInstruction.Loop -> TODO()
    is ControlInstruction.If -> TODO()
    // unreachable throws
    ControlInstruction.Unreachable -> throw KWasmRuntimeException("unreachable instruction reached")
    // nop does nothing.
    ControlInstruction.NoOp -> context
    is ControlInstruction.Break -> TODO()
    is ControlInstruction.BreakIf -> TODO()
    is ControlInstruction.BreakTable -> TODO()
    is ControlInstruction.Return -> TODO()
    is ControlInstruction.Call -> TODO()
    is ControlInstruction.CallIndirect -> TODO()
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#exec-block):
 *
 * ```
 *   block [t?] instr* end
 * ```
 *
 * 1. Let `n` be the arity `|t?|` of the result type `t?`.
 * 1. Let `L` be the label whose arity is `n` and whose continuation is the end of the block.
 * 1. Enter the block `instr*` with label `L`.
 */
internal fun ControlInstruction.Block.execute(context: ExecutionContext): ExecutionContext {
    val expectedValType = result.result?.valType

    val myLabel = Label(
        label,
        instructions,
        if (expectedValType != null) 1 else 0
    )
    context.stacks.labels.push(myLabel)

    // Enter the block and run the insides.
    val postInnerContext = instructions.execute(context)

    // Check the result type, if we expected one
    expectedValType?.let { checkResultType(it, postInnerContext) }

    // Verify that the top of the label stack is myLabel.
    val postContextLabelTop = postInnerContext.stacks.labels.peek()
    if (postContextLabelTop != myLabel) throw KWasmRuntimeException(
        "Label stack expected to be topped by $myLabel, but found $postContextLabelTop"
    )
    postInnerContext.stacks.labels.pop()

    return context
}

private fun checkResultType(expectedValType: ValueType, context: ExecutionContext) {
    val stackTop = context.stacks.operands.peek()
        ?: throw KWasmRuntimeException("expected to exit with $expectedValType on the stack")
    val stackTopType = stackTop::class.toValueType()

    if (stackTopType != expectedValType) throw KWasmRuntimeException(
        "expected to exit with $expectedValType on the top of the stack, but found $stackTopType"
    )
}
