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
import kwasm.ast.instruction.Instruction
import kwasm.ast.module.Index
import kwasm.ast.type.ValueType
import kwasm.runtime.EmptyValue
import kwasm.runtime.ExecutionContext
import kwasm.runtime.FunctionInstance
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
    is ControlInstruction.Block -> executeBlockOrLoop(
        label,
        result.result?.valType,
        false,
        instructions,
        context
    )
    is ControlInstruction.Loop -> executeBlockOrLoop(
        label,
        result.result?.valType,
        true,
        instructions,
        context
    )
    is ControlInstruction.If -> this.execute(context)
    // unreachable throws
    ControlInstruction.Unreachable -> throw KWasmRuntimeException("unreachable instruction reached")
    // nop does nothing.
    ControlInstruction.NoOp -> context
    is ControlInstruction.Break -> this.execute(context)
    is ControlInstruction.BreakIf -> this.execute(context)
    is ControlInstruction.BreakTable -> this.execute(context)
    is ControlInstruction.Return -> this.execute(context)
    is ControlInstruction.Call -> this.execute(context)
    is ControlInstruction.CallIndirect -> this.execute(context)
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
 *
 * ```
 *   loop [t?] instr* end
 * ```
 *
 * 1. Let `L` be the label whose arity is `0` and whose continuation is the start of the loop.
 * 1. Enter the block `instr*` with label `L`.
 */
internal fun executeBlockOrLoop(
    label: Identifier.Label,
    expectedValType: ValueType?,
    isLoop: Boolean,
    blockInstructions: List<Instruction>,
    context: ExecutionContext
): ExecutionContext {
    val myLabel = Label(
        label,
        if (isLoop) blockInstructions else emptyList(), // 'end of the block'
        if (expectedValType != null) 1 else 0,
        context.stacks.operands
    )

    context.stacks.labels.push(myLabel)

    // Enter the block and run the insides with an empty op stack.
    val postInnerContext = blockInstructions.execute(
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
 * From [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#exec-if):
 *
 * ```
 *   if [t?] instr*1 else instr*2 end
 * ```
 *
 * 1. Assert: due to validation, a value of value type `i32` is on the top of the stack.
 * 1. Pop the value `i32.const c` from the stack.
 * 1. Let `n` be the arity `|t?|` of the result type `t?`.
 * 1. Let `L` be the label whose arity is `n` and whose continuation is the end of the `if`
 *    instruction.
 * 1. If `c` is non-zero, then:
 *    * Enter the block `instr*1` with label `L`. (see [executeBlockOrLoop])
 * 1. Else:
 *    * Enter the block `instr*2` with label `L`. (see [executeBlockOrLoop])
 */
internal fun ControlInstruction.If.execute(context: ExecutionContext): ExecutionContext {
    val condition = context.stacks.operands.pop() as? IntValue
        ?: throw KWasmRuntimeException("if requires i32 at the top of the stack")

    return if (condition.value != 0) {
        executeBlockOrLoop(label, result.result?.valType, false, positiveInstructions, context)
    } else {
        executeBlockOrLoop(label, result.result?.valType, false, negativeInstructions, context)
    }
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
 * From [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#exec-return):
 *
 * ```
 *   return
 * ```
 *
 * 1. Let `F` be the current frame.
 * 1. Let `n` be the arity of `F`.
 * 1. Assert: due to validation, there are at least `n` values on the top of the stack.
 * 1. Pop the results `val^n` from the stack.
 * 1. Assert: due to validation, the stack contains at least one frame.
 * 1. While the top of the stack is not a frame, do:
 * 1. Pop the top element from the stack.
 * 1. Assert: the top of the stack is the frame `F`.
 * 1. Pop the frame from the stack.
 * 1. Push `val^n` to the stack.
 * 1. Jump to the instruction after the original call that pushed the frame.
 */
internal fun ControlInstruction.Return.execute(context: ExecutionContext): ExecutionContext {
    val currentActivation = context.stacks.activations.pop()
    if (context.stacks.operands.height < currentActivation.arity)
        throw KWasmRuntimeException("Can't return, insufficient data available for function type")
    return context
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#exec-call):
 *
 * ```
 *   call x
 * ```
 *
 * 1. Let `F` be the current frame.
 * 1. Assert: due to validation, `F.module.funcaddrs\[x]` exists.
 * 1. Let `a` be the function address `F.module.funcaddrs\[x]`.
 * 1. Invoke the function instance at address `a`.
 */
internal fun ControlInstruction.Call.execute(context: ExecutionContext): ExecutionContext {
    val currentActivation = context.stacks.activations.peek()
        ?: throw KWasmRuntimeException("Cannot call a function outside of an activation frame")
    val funcAddr = currentActivation.module.functionAddresses[functionIndex]
        ?: throw KWasmRuntimeException("Can't find function address at index $functionIndex")
    val function = context.store.functions[funcAddr.value]

    return function.execute(context)
}

/**
 * From
 * [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#exec-call-indirect):
 *
 * ```
 *   call_indirect x
 * ```
 *
 * 1. Let `F` be the current frame.
 * 1. Assert: due to validation, `F.module.tableaddrs[0]` exists.
 * 1. Let `ta` be the table address `F.module.tableaddrs\[0]`.
 * 1. Assert: due to validation, `S.tables\[ta]` exists.
 * 1. Let `tab` be the table instance `S.tables\[ta]`.
 * 1. Assert: due to validation, `F.module.types\[x]` exists.
 * 1. Let `ft_expect` be the function type `F.module.types\[x]`.
 * 1. Assert: due to validation, a value with value type `i32` is on the top of the stack.
 * 1. Pop the value `i32.const i` from the stack.
 * 1. If `i` is not smaller than the length of `tab.elem`, then:
 *    * Trap.
 * 1. If `tab.elem\[i]` is uninitialized, then:
 *    * Trap.
 * 1. Let `a` be the function address `tab.elem\[i]`.
 * 1. Assert: due to validation, `S.funcs\[a]` exists.
 * 1. Let `f` be the function instance `S.funcs\[a]`.
 * 1. Let `ft_actual` be the function type `f.type`.
 * 1. If `ft_actual` and `ft_expect` differ, then:
 *    * Trap.
 * 1. Invoke the function instance at address `a`.
 */
internal fun ControlInstruction.CallIndirect.execute(context: ExecutionContext): ExecutionContext {
    val activationFrame = context.stacks.activations.peek()
        ?: throw KWasmRuntimeException("Cannot call_indirect from outside a function")
    val tableAddress = activationFrame.module.tableAddresses.getOrNull(0)
        ?: throw KWasmRuntimeException("No table allocated for module")
    val table = context.store.tables.getOrNull(tableAddress.value)
        ?: throw KWasmRuntimeException(
            "No table found in the store at address ${tableAddress.value}"
        )

    val expectedFunctionType = typeUse.index?.let { activationFrame.module.types[it] }
        ?: activationFrame.module.types.find { it == typeUse.functionType }
        ?: throw KWasmRuntimeException("Expected type not found in module: $typeUse")
    val argument = context.stacks.operands.pop() as? IntValue
        ?: throw KWasmRuntimeException("Expected an i32 on the top of the stack")

    if (argument.value >= table.elements.size)
        throw KWasmRuntimeException("Table for module has no element at position ${argument.value}")
    val functionAddress = table.elements[argument.value]
        ?: throw KWasmRuntimeException(
            "No function found in the table with location ${argument.value}"
        )
    val function = context.store.functions.getOrNull(functionAddress.value)
        ?: throw KWasmRuntimeException(
            "No function found in the store at address ${functionAddress.value}"
        )

    val actualFunctionType = function.type
    if (actualFunctionType != expectedFunctionType)
        throw KWasmRuntimeException(
            "Function at address ${functionAddress.value} has unexpected type. " +
                "Expected $expectedFunctionType."
        )

    if (function is FunctionInstance.Module) {
        return function.execute(
            ExecutionContext(context.store, function.moduleInstance, context.stacks)
        )
    }
    return function.execute(context)
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

    // If we have a continuation for the label that's non-empty, push the label back onto the stack
    if (label.continuation.isNotEmpty()) context.stacks.labels.push(label)

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
