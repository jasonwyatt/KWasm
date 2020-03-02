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
import kwasm.ast.instruction.VariableInstruction
import kwasm.runtime.DoubleValue
import kwasm.runtime.ExecutionContext
import kwasm.runtime.FloatValue
import kwasm.runtime.Global
import kwasm.runtime.IntValue
import kwasm.runtime.LongValue
import kwasm.runtime.toValue

/**
 * Executes the receiving [VariableInstruction].
 */
internal fun VariableInstruction.execute(
    context: ExecutionContext
): ExecutionContext = when (this) {
    is VariableInstruction.GlobalGet -> this.execute(context)
    is VariableInstruction.GlobalSet -> this.execute(context)
    else -> TODO("$this not supported yet")
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#exec-global-get):
 *
 * 1. Let `F` be the current frame.
 * 1. Assert: due to validation, `F.module.globaladdrs\[x]` exists.
 * 1. Let `a` be the global address `F.module.globaladdrs\[x]`.
 * 1. Assert: due to validation, `S.globals\[a]` exists.
 * 1. Let `glob` be the global instance `S.globals\[a]`.
 * 1. Let `val` be the value `glob.value`.
 * 1. Push the value `val` to the stack.
 */
internal fun VariableInstruction.GlobalGet.execute(context: ExecutionContext): ExecutionContext {
    val globalAddress = context.moduleInstance.globalAddresses[valueAstNode]
        ?: throw KWasmRuntimeException("Global with index $valueAstNode not found")

    when (val globalValue = context.store.globals[globalAddress.value]) {
        is Global.Int -> context.stacks.operands.push(globalValue.value.toValue())
        is Global.Long -> context.stacks.operands.push(globalValue.value.toValue())
        is Global.Float -> context.stacks.operands.push(globalValue.value.toValue())
        is Global.Double -> context.stacks.operands.push(globalValue.value.toValue())
    }

    return context
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#exec-global-set):
 *
 * 1. Let `F` be the current frame.
 * 1. Assert: due to validation, `F.module.globaladdrs\[x]` exists.
 * 1. Let `a` be the global address `F.module.globaladdrs\[x]`.
 * 1. Assert: due to validation, `S.globals\[a]` exists.
 * 1. Let `glob` be the global instance `S.globals\[a]`.
 * 1. Assert: due to validation, a value is on the top of the stack.
 * 1. Pop the value `val` from the stack.
 * 1. Replace `glob.value` with the value `val`.
 */
internal fun VariableInstruction.GlobalSet.execute(context: ExecutionContext): ExecutionContext {
    val globalAddress = context.moduleInstance.globalAddresses[valueAstNode]
        ?: throw KWasmRuntimeException("Global with index $valueAstNode not found")

    val globalValue = context.store.globals[globalAddress.value]
    if (!globalValue.mutable) {
        throw KWasmRuntimeException("Global with index $valueAstNode is not mutable")
    }

    val topOfStack = context.stacks.operands.pop()

    when (globalValue) {
        is Global.Int -> {
            topOfStack as? IntValue
                ?: throw KWasmRuntimeException("Top of stack is not expected type: IntValue")
            globalValue.value = topOfStack.value
        }
        is Global.Long -> {
            topOfStack as? LongValue
                ?: throw KWasmRuntimeException("Top of stack is not expected type: LongValue")
            globalValue.value = topOfStack.value
        }
        is Global.Float -> {
            topOfStack as? FloatValue
                ?: throw KWasmRuntimeException("Top of stack is not expected type: FloatValue")
            globalValue.value = topOfStack.value
        }
        is Global.Double -> {
            topOfStack as? DoubleValue
                ?: throw KWasmRuntimeException("Top of stack is not expected type: DoubleValue")
            globalValue.value = topOfStack.value
        }
    }
    return context
}
