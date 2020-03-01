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
import kwasm.api.UnitHostFunction
import kwasm.api.functionType
import kwasm.runtime.FunctionInstance.Companion.allocate
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FunctionInstanceTest {
    @Test
    fun allocateHostFunction() {
        val store = Store()
        val function = UnitHostFunction { println("Hello World!") }

        store.allocate(function)
            .also { (newStore, addr) ->
                assertThat(addr.value).isEqualTo(0)
                assertThat(newStore.functions).hasSize(1)
                assertThat(newStore.functions[0]).isEqualTo(
                    FunctionInstance.Host(
                        function.functionType,
                        function
                    )
                )
            }
    }
}
