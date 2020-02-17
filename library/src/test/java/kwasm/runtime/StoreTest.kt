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

import com.google.common.truth.Truth.assertThat
import kwasm.api.HostFunction
import kwasm.ast.type.FunctionType
import kwasm.runtime.memory.ByteBufferMemory
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StoreTest {
    @Test
    fun pseudoConstructor() {
        val expectedFunctions = mutableListOf<FunctionInstance>()
        val expectedTables = mutableListOf<Table>()
        val expectedMemories = mutableListOf<Memory>()
        val expectedGlobals = mutableListOf<Global<*>>()
        val store = Store {
            functions += FunctionInstance.Host(
                FunctionType(emptyList(), emptyList()),
                HostFunction()
            ).also { expectedFunctions += it }
            tables += Table(emptyList()).also { expectedTables += it }
            memories += ByteBufferMemory().also { expectedMemories += it }
            globals += Global.Int(10, true).also { expectedGlobals += it }
            globals += Global.Float(10f, false).also { expectedGlobals += it }
        }

        assertThat(store.functions).containsExactlyElementsIn(expectedFunctions)
        assertThat(store.tables).containsExactlyElementsIn(expectedTables)
        assertThat(store.memories).containsExactlyElementsIn(expectedMemories)
        assertThat(store.globals).containsExactlyElementsIn(expectedGlobals)
    }
}
