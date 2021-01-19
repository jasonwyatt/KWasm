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

package kwasm.spectests.execution

import kwasm.KWasmProgram
import kwasm.api.ByteBufferMemoryProvider
import kwasm.ast.Identifier
import kwasm.ast.instruction.Expression
import kwasm.ast.module.WasmModule
import kwasm.ast.util.MutableAstNodeIndex
import kwasm.runtime.EmptyExecutionContext
import kwasm.runtime.Value
import kwasm.runtime.instruction.execute
import kwasm.runtime.toValue

class ScriptContext(
    val modules: MutableAstNodeIndex<WasmModule> = MutableAstNodeIndex(),
    var programBuilder: KWasmProgram.Builder =
        KWasmProgram.Builder(ByteBufferMemoryProvider(4000000))
) {
    private val namesByIdentifier = mutableMapOf<Identifier.Label, String>()
    lateinit var program: KWasmProgram

    fun build() {
        program = programBuilder.build()
    }

    fun invoke(
        moduleIdentifier: Identifier.Label?,
        fnName: String,
        args: List<Expression>
    ): List<Value<*>> {
        val argValues = args.map { expr ->
            val context = EmptyExecutionContext()
            expr.execute(context)
            context.stacks.operands.pop()
        }

        val moduleName = namesByIdentifier[moduleIdentifier] ?: ""
        val fn = program.getFunction(moduleName, fnName)
        val answer = fn.invoke(*argValues.map { it.value }.toTypedArray())
        return answer?.let { listOf(it.toValue()) } ?: emptyList()
    }

    fun getGlobal(moduleIdentifier: Identifier.Label?, globalName: String): Value<*> {
        val moduleName = namesByIdentifier[moduleIdentifier] ?: ""
        return program.getGlobal(moduleName, globalName).value.toValue()
    }
}
