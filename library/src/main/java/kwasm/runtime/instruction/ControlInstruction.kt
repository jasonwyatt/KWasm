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
import kwasm.ast.Identifier
import kwasm.ast.instruction.ControlInstruction
import kwasm.ast.module.Index
import kwasm.ast.type.ValueType
import kwasm.runtime.EmptyValue
import kwasm.runtime.ExecutionContext
import kwasm.runtime.IntValue
import kwasm.runtime.popUntil
import kwasm.runtime.stack.Label
import kwasm.runtime.stack.OperandStack
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
    is ControlInstruction.Break -> this.execute(context)
    is ControlInstruction.BreakIf -> this.execute(context)
    is ControlInstruction.BreakTable -> this.execute(context)
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
        emptyList(), // 'end of the block'
        if (expectedValType != null) 1 else 0,
        context.stacks.operands
    )

    context.stacks.labels.push(myLabel)

    // Enter the block and run the insides with an empty op stack.
    val postInnerContext = instructions.execute(
        context.copy(
            stacks = context.stacks.copy(operands = OperandStack())
        )
    )
    val postContextLabelTop = postInnerContext.stacks.labels.peek()

    // Check if we exited ourselves naturally (if so - our label will be on the top still).
    // Strict equality is best, but if not- then at least the string reprs should match.
    return if (
        postContextLabelTop === myLabel ||
        (myLabel.identifier?.stringRepr != null &&
            myLabel.identifier.stringRepr == postContextLabelTop?.identifier?.stringRepr)
    ) {
        // Check the result type, if we expected one
        expectedValType?.let {
            checkResultType(it, postInnerContext)
            // push that result onto our stack
            context.stacks.operands.push(postInnerContext.stacks.operands.pop())
        }
        // pop our label
        context.stacks.labels.pop()
        context
    } else postInnerContext // If we were jumped out-of, return the context from the internals.
}

/**
 * See [executeBreakTo].
 */
internal fun ControlInstruction.Break.execute(context: ExecutionContext): ExecutionContext =
    executeBreakTo(labelIndex, context)

/**
 * From [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#exec-br-if):
 *
 * ```
 *   br_if l
 * ```
 *
 * 1. Assert: due to validation, a value of value type `i32` is on the top of the stack.
 * 1. Pop the value `i32.const c` from the stack.
 * 1. If `c` is non-zero, then:
 *    * Execute the instruction `(br l)`. (see [executeBreakTo])
 * 1. Else:
 *    * Do nothing.
 */
internal fun ControlInstruction.BreakIf.execute(context: ExecutionContext): ExecutionContext {
    val param = context.stacks.operands.pop() as? IntValue
        ?: throw KWasmRuntimeException("br_if requires i32 at the top of the stack")
    if (param.value == 0) return context
    return executeBreakTo(labelIndex, context)
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#exec-br-table):
 *
 * ```
 *   br_table l* l_N
 * ```
 *
 * 1. Assert: due to validation, a value of value type `i32` is on the top of the stack.
 * 1. Pop the value `i32.const i` from the stack.
 * 1. If `i` is smaller than the length of `l*`, then:
 *    * Let `l_i` be the label `l*\[i]`.
 *    * Execute the instruction `(br l_i)`.
 * 1. Else:
 *    * Execute the instruction `(br l_N)`.
 */
internal fun ControlInstruction.BreakTable.execute(context: ExecutionContext): ExecutionContext {
    val param = context.stacks.operands.pop() as? IntValue
        ?: throw KWasmRuntimeException("br_table requires i32 at the top of the stack")

    val breakTarget = targets.getOrNull(param.value) ?: defaultTarget
    return executeBreakTo(breakTarget, context)
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#exec-br):
 *
 * ```
 *   br l
 * ```
 *
 * 1. Assert: due to validation, the stack contains at least `l+1` labels.
 * 1. Let `L` be the `l`-th label appearing on the stack, starting from the top and counting from
 *    zero.
 * 1. Let `n` be the arity of `L`.
 * 1. Assert: due to validation, there are at least `n` values on the top of the stack.
 * 1. Pop the values `val^n` from the stack.
 * 1. Repeat `l+1` times:
 *    1. While the top of the stack is a value, do:
 *       * Pop the value from the stack.
 *    1. Assert: due to validation, the top of the stack now is a label.
 *    1. Pop the label from the stack.
 * 1. Push the values `val^n` to the stack.
 * 1. Jump to the continuation of `L`.
 */
internal fun executeBreakTo(
    labelIndex: Index<Identifier.Label>,
    context: ExecutionContext
): ExecutionContext {
    // Find the label we're jumping to in the label stack.
    val label = if (labelIndex as? Index.ByInt != null) {
        val labelCountFromTop = labelIndex.indexVal
        var poppedSoFar = 0
        // Pop labels further up the stack until we reach the one we're looking for
        context.stacks.labels.popUntil {
            if (poppedSoFar == labelCountFromTop) return@popUntil true
            poppedSoFar++
            false
        }
    } else {
        val labelIdentifier = (labelIndex as Index.ByIdentifier<Identifier.Label>)
            .indexVal.stringRepr
        // Pop labels further up the stack until we reach the one we're looking for
        context.stacks.labels.popUntil {
            (it.identifier as Identifier.Label).stringRepr == labelIdentifier
        }
    } ?: throw KWasmRuntimeException("Could not jump to label identified by: $labelIndex")

    val results = (0 until label.arity).map { context.stacks.operands.pop() }.reversed()

    // Now clear our stack.
    context.stacks.operands.clear()

    // Push the label's op stack at enter stack.
    results.forEach(label.opStackAtEnter::push)

    // Jump to the continuation.
    return label.continuation.execute(
        context.copy(stacks = context.stacks.copy(operands = label.opStackAtEnter))
    )
}

private fun checkResultType(expectedValType: ValueType, context: ExecutionContext) {
    val stackTop = context.stacks.operands.peek()
        ?: throw KWasmRuntimeException("expected to exit with $expectedValType on the stack")

    if (stackTop == EmptyValue) throw KWasmRuntimeException(
        "expected to exit with $expectedValType on the stack"
    )

    val stackTopType = stackTop::class.toValueType()

    if (stackTopType != expectedValType) throw KWasmRuntimeException(
        "expected to exit with $expectedValType on the top of the stack, but found $stackTopType"
    )
}
