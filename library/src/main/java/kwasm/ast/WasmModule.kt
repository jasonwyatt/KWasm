/*
 * Copyright 2019 Google LLC
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

package kwasm.ast

/**
 * Represents a WebAssembly module.
 *
 * From [the docs](https://webassembly.github.io/spec/core/syntax/modules.html#globals):
 *
 * WebAssembly programs are organized into modules, which are the unit of deployment, loading, and
 * compilation. A module collects definitions for types, functions, tables, memories, and globals.
 * In addition, it can declare imports and exports and provide initialization logic in the form of
 * data and element segments or a start function.
 */
data class WasmModule(
    val identifier: Identifier.Label?,
    val types: List<Type>,
    val imports: List<Import>,
    val functions: List<WasmFunction>,
    val tables: List<Table>,
    val memories: List<Memory>,
    val globals: List<Global>,
    val exports: List<Export>,
    val start: StartFunction?,
    val elements: List<ElementSegment>,
    val data: List<DataSegment>
) : AstNode {
    /** Composes this [WasmModule] with another into a new instance. */
    fun composeWith(other: WasmModule): WasmModule {
        check(start == null || other.start == null) {
            "Only one module may have a defined start function"
        }
        if (other.imports.isNotEmpty()) {
            check(
                functions.isEmpty() && tables.isEmpty() && globals.isEmpty() && memories.isEmpty()
            ) {
                "If the second module contains imports, the first module must not define " +
                    "functions, tables, globals, or memories"
            }
        }
        return WasmModule(
            identifier,
            types + other.types,
            imports + other.imports,
            functions + other.functions,
            tables + other.tables,
            memories + other.memories,
            globals + other.globals,
            exports + other.exports,
            start ?: other.start,
            elements + other.elements,
            data + other.data
        )
    }
}