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
import kwasm.runtime.Address

/**
 * Implementation of [MutableList] which allows for [Address] lookups using [Index.ByIdentifier]
 * values.
 */
class AddressIndex<T : Address>(
    contents: List<T> = emptyList()
) : MutableList<T> by contents.toMutableList() {
    private val addressIndexByIdentifier = mutableMapOf<Identifier, Int>()

    /** Adds the [address] to the [TypeIndex], with accompanying (optional) [identifier]. */
    fun add(address: T, identifier: Identifier?) {
        identifier?.let { addressIndexByIdentifier[it] = size }
        add(address)
    }

    /**
     * Gets the [Address] from the [AddressIndex] using the given [Index]. Returns `null` if it
     * can't be found.
     */
    operator fun get(index: Index<*>): T? = when (index) {
        is Index.ByInt -> getOrNull(index.indexVal)
        is Index.ByIdentifier<*> -> addressIndexByIdentifier[index.indexVal]?.let { getOrNull(it) }
    }
}
