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

import kwasm.ast.type.TableType

/**
 * Runtime representation of a WebAssembly Table.
 *
 * From [the docs](https://webassembly.github.io/spec/core/exec/runtime.html#table-instances):
 *
 * A table instance is the runtime representation of a table. It holds a vector of function elements
 * and an optional maximum size, if one was specified in the table type at the table’s definition
 * site.
 *
 * Each function element is either empty, representing an uninitialized table entry, or a function
 * address. Function elements can be mutated through the execution of an element segment or by
 * external means provided by the embedder.
 *
 * ```
 *   tableinst ::= {elem vec(funcelem), max u32?}
 *   funcelem ::= funcaddr?
 * ```
 *
 * It is an invariant of the semantics that the length of the element vector never exceeds the
 * maximum size, if present.
 */
@Suppress("EXPERIMENTAL_API_USAGE")
data class Table(
    val elements: List<Address.Function>,
    val maxSize: Int = UInt.MAX_VALUE.toInt()
) {
    init {
        require(maxSize.toUInt() >= elements.size.toUInt()) {
            "Elements in table cannot exceed the maximum size: ${maxSize.toUInt()}"
        }
    }
}

/**
 * Builder to create a [Table] given a [TableType] and a [builder] function used to add elements to
 * the [Table]'s elements.
 */
@Suppress("EXPERIMENTAL_API_USAGE", "FunctionName")
fun Table(tableType: TableType, builder: (MutableList<Address.Function>) -> Unit): Table {
    val elements = mutableListOf<Address.Function>().also(builder)
    return Table(elements, maxSize = tableType.limits.max?.toInt() ?: UInt.MAX_VALUE.toInt())
}
