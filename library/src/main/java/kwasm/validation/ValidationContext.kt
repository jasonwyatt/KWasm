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

package kwasm.validation

import kwasm.ast.GlobalType
import kwasm.ast.ImportDescriptor
import kwasm.ast.MemoryType
import kwasm.ast.ResultType
import kwasm.ast.TableType
import kwasm.ast.Type
import kwasm.ast.TypeUse
import kwasm.ast.ValueType
import kwasm.ast.WasmFunction
import kwasm.ast.WasmModule
import kwasm.ast.astNodeListOf
import kwasm.ast.util.AstNodeIndex
import kwasm.ast.util.MutableAstNodeIndex

/**
 * Represents the context accrued throughout the validation process.
 *
 * [ValidationContext] objects contain the current [AstNode] being validated, as well as additional
 * context as specified in the WebAssembly Spec.
 *
 * From [the docs](https://webassembly.github.io/spec/core/valid/conventions.html#contexts):
 *
 * Validity of an individual definition is specified relative to a context, which collects relevant
 * information about the surrounding module and the definitions in scope:
 *
 * * Types: the list of types defined in the current module.
 * * Functions: the list of functions declared in the current module, represented by their function
 *   type.
 * * Tables: the list of tables declared in the current module, represented by their table type.
 * * Memories: the list of memories declared in the current module, represented by their memory
 *   type.
 * * Globals: the list of globals declared in the current module, represented by their global type.
 * * Locals: the list of locals declared in the current function (including parameters), represented
 *   by their value type.
 * * Labels: the stack of labels accessible from the current position, represented by their result
 *   type.
 * * Return: (the return type of the current function, represented as an optional result type that is
 *   absent when no return is allowed, as in free-standing expressions.
 *
 * In other words, a context contains a sequence of suitable types for each index space, describing
 * each defined entry in that space. Locals, labels and return type are only used for validating
 * instructions in function bodies, and are left empty elsewhere. The label stack is the only part
 * of the context that changes as validation of an instruction sequence proceeds.
 */
sealed class ValidationContext(
    open val types: AstNodeIndex<Type>,
    open val functions: AstNodeIndex<TypeUse>,
    open val tables: AstNodeIndex<TableType>,
    open val memories: AstNodeIndex<MemoryType>,
    open val globals: AstNodeIndex<GlobalType>
) {
    /** See [ValidationContext]. */
    data class Module(
        override val types: AstNodeIndex<Type>,
        override val functions: AstNodeIndex<TypeUse>,
        override val tables: AstNodeIndex<TableType>,
        override val memories: AstNodeIndex<MemoryType>,
        override val globals: AstNodeIndex<GlobalType>
    ) : ValidationContext(types, functions, tables, memories, globals)

    /**
     * For validating [Instruction]s within a [WasmFunction] body or [Expression].
     *
     * See [ValidationContext].
     */
    data class FunctionBody(
        override val types: AstNodeIndex<Type>,
        override val functions: AstNodeIndex<TypeUse>,
        override val tables: AstNodeIndex<TableType>,
        override val memories: AstNodeIndex<MemoryType>,
        override val globals: AstNodeIndex<GlobalType>,
        val locals: AstNodeIndex<ValueType>,
        val labels: AstNodeIndex<ResultType>,
        val returnType: ValueType?
    ) : ValidationContext(types, functions, tables, memories, globals)
}

/** Given a [WasmModule], creates a new [ValidationContext]. */
fun ValidationContext(module: WasmModule): ValidationContext.Module {
    val types = MutableAstNodeIndex<Type>()
    val functions = MutableAstNodeIndex<TypeUse>()
    val tables = MutableAstNodeIndex<TableType>()
    val memories = MutableAstNodeIndex<MemoryType>()
    val globals = MutableAstNodeIndex<GlobalType>()

    // TODO: remove upcastThrown calls when ParseContext is available on AstNodes.
    module.types.forEach {
        upcastThrown { types[it.id] = it }
    }
    module.functions.forEach {
        val typeUse = it.typeUse ?: TypeUse(null, astNodeListOf(), astNodeListOf())

        // If the typeUse has an index, validate that there is a defined type for that index
        // TODO: pass the context when it's available on the AstNode
        validate(typeUse.index == null || types[typeUse.index] != null) {
            "Function uses type ${typeUse.index}, but it's not declared by the module"
        }

        upcastThrown { functions[it.id] = typeUse }
    }
    module.tables.forEach {
        upcastThrown { tables[it.id] = it.tableType }
    }
    module.memories.forEach {
        upcastThrown { memories[it.id] = it.memoryType }
    }
    module.globals.forEach {
        upcastThrown { globals[it.id] = it.globalType }
    }
    module.imports.forEach { import ->
        when (val descriptor = import.descriptor) {
            is ImportDescriptor.Function -> {
                val typeUse = descriptor.typeUse

                // If the typeUse has an index, validate that there is a defined type for that index
                // TODO: pass the context when it's available on the AstNode
                validate(typeUse.index == null || types[typeUse.index] != null) {
                    "Function uses type ${typeUse.index}, but it's not declared by the module"
                }

                upcastThrown { functions[descriptor.id] = typeUse }
            }
            is ImportDescriptor.Table -> {
                validate(
                    descriptor.id.stringRepr != null
                        || descriptor.id.unique != null
                        || tables[0] == null
                ) {
                    "Cannot import a table when one is already defined by the module"
                }
                upcastThrown { tables[descriptor.id] = descriptor.tableType }
            }
            is ImportDescriptor.Memory -> {
                validate(
                    descriptor.id.stringRepr != null
                        || descriptor.id.unique != null
                        || memories[0] == null
                ) {
                    "Cannot import a memory when one is already defined by the module"
                }
                upcastThrown { memories[descriptor.id] = descriptor.memoryType }
            }
            is ImportDescriptor.Global -> upcastThrown {
                globals[descriptor.id] = descriptor.globalType
            }
        }
    }

    return ValidationContext.Module(types, functions, tables, memories, globals)
}
