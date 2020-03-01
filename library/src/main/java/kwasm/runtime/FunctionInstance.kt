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

package kwasm.runtime

import kwasm.api.HostFunction
import kwasm.api.functionType
import kwasm.ast.module.WasmFunction
import kwasm.ast.type.FunctionType

/**
 * Represents either a [WasmFunction] from a [ModuleInstance], or a [HostFunction] exposed to the
 * [ModuleInstance] via imports.
 *
 * From [the docs](https://webassembly.github.io/spec/core/exec/runtime.html#function-instances):
 *
 * A function instance is the runtime representation of a function. It effectively is a closure of
 * the original function over the runtime module instance of its originating module. The module
 * instance is used to resolve references to other definitions during execution of the function.
 *
 * ```
 *   funcinst   ::= {type functype, module moduleinst, code func}
 *                  {type functype, hostcode hostfunc}
 *   hostfunc ::= ...
 * ```
 *
 * A host function is a function expressed outside WebAssembly but passed to a module as an import.
 */
sealed class FunctionInstance(open val type: FunctionType) {
    /** A function from within a [kwasm.ast.module.WasmModule]'s [ModuleInstance]. */
    data class Module(
        val moduleInstance: ModuleInstance,
        val code: WasmFunction
    ) : FunctionInstance(
        code.typeUse?.functionType ?: FunctionType(emptyList(), emptyList())
    )

    /** A function supplied by the host. */
    data class Host(
        override val type: FunctionType,
        val hostFunction: HostFunction<*>
    ) : FunctionInstance(type)

    companion object {
        /**
         * From [the docs](https://webassembly.github.io/spec/core/exec/modules.html#alloc-func):
         *
         * 1. Let `func` be the function to allocate and `moduleinst` its module instance.
         * 1. Let `a` be the first free function address in `S`.
         * 1. Let `functype` be the function type `moduleinst.types\[func.type]`.
         * 1. Let `funcinst` be the function instance
         *    `{type functype, module moduleinst, code func}`.
         * 1. Append `funcinst` to the `funcs` of `S`.
         * 1. Return `a`.
         */
        fun Store.allocate(
            moduleInstance: ModuleInstance,
            wasmFunction: WasmFunction
        ): Store.Allocation<Address.Function> =
            allocateFunction(Module(moduleInstance, wasmFunction))

        /**
         * From [the
         * docs](https://webassembly.github.io/spec/core/exec/modules.html#host-functions):
         *
         * 1. Let `hostfunc` be the host function to allocate and `functype` its function type.
         * 1. Let `a` be the first free function address in `S`.
         * 1. Let `funcinst` be the function instance `{type functype, hostcode hostfunc}`.
         * 1. Append `funcinst` to the `funcs` of `S`.
         * 1. Return `a`.
         */
        fun Store.allocate(hostFunction: HostFunction<*>): Store.Allocation<Address.Function> =
            allocateFunction(Host(hostFunction.functionType, hostFunction))
    }
}
