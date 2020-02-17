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

/**
 * From [the docs](https://webassembly.github.io/spec/core/exec/runtime.html#store):
 *
 * The store represents all global state that can be manipulated by WebAssembly programs. It
 * consists of the runtime representation of all instances of functions, tables, memories, and
 * globals that have been allocated during the life time of the abstract machine.
 *
 * Syntactically, the store is defined as a record listing the existing instances of each category:
 * ```
 *   store  ::= {
 *                  funcs funcinst*,
 *                  tables tableinst*,
 *                  mems meminst*,
 *                  globals globalinst*
 *              }
 * ```
 */
data class Store internal constructor(
    val functions: List<FunctionInstance>,
    val tables: List<Table>,
    val memories: List<Memory>,
    val globals: List<Global<*>>
) {
    class Builder internal constructor(
        val functions: MutableList<FunctionInstance> = mutableListOf(),
        val tables: MutableList<Table> = mutableListOf(),
        val memories: MutableList<Memory> = mutableListOf(),
        val globals: MutableList<Global<*>> = mutableListOf()
    ) {
        fun build() = Store(functions, tables, memories, globals)
    }
}

/** Builder of a [Store]. */
@Suppress("FunctionName")
fun Store(builder: Store.Builder.() -> Unit): Store =
    Store.Builder().apply(builder).build()
