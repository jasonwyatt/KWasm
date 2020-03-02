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

package kwasm.runtime.util

import kwasm.ast.Identifier
import kwasm.ast.module.Index
import kwasm.ast.module.Type
import kwasm.ast.type.FunctionType

/**
 * Implementation of [MutableList] which allows for lookups using [Index.ByIdentifier] values.
 */
class TypeIndex(
    types: List<FunctionType> = emptyList()
) : MutableList<FunctionType> by types.toMutableList() {
    private val addressIndexByIdentifier = mutableMapOf<Identifier, Int>()

    /** Adds the [type] to the [TypeIndex], with accompanying (optional) [identifier]. */
    fun add(type: FunctionType, identifier: Identifier?) {
        identifier?.let { addressIndexByIdentifier[it] = size }
        add(type)
    }

    /**
     * Gets the [FunctionType] from the [TypeIndex] using the given [Index]. Returns `null` if it
     * can't be found.
     */
    operator fun get(index: Index<*>): FunctionType? = when (index) {
        is Index.ByInt -> getOrNull(index.indexVal)
        is Index.ByIdentifier<*> -> addressIndexByIdentifier[index.indexVal]?.let { getOrNull(it) }
    }
}

/** Creates a [TypeIndex] from a list of [Type]s. */
@Suppress("FunctionName")
fun TypeIndex(types: List<Type>): TypeIndex =
    TypeIndex().apply { types.forEach { add(it.functionType, it.id) } }
