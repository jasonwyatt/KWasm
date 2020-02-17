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
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")
@RunWith(JUnit4::class)
class GlobalTest {
    @Test
    fun int_toUnsigned() {
        var int = Global.Int(10, true)
        assertThat(int.unsigned).isEqualTo(10u)

        int = Global.Int(-1, true)
        assertThat(int.unsigned).isEqualTo(UInt.MAX_VALUE)
    }

    @Test
    fun int_setUnsigned() {
        val int = Global.Int(10, true)
        int.unsigned = UInt.MAX_VALUE
        assertThat(int.value).isEqualTo(-1)
    }

    @Test
    fun long_toUnsigned() {
        var long = Global.Long(10L, true)
        assertThat(long.unsigned).isEqualTo(10uL)

        long = Global.Long(-1L, true)
        assertThat(long.unsigned).isEqualTo(ULong.MAX_VALUE)
    }

    @Test
    fun long_setUnsigned() {
        val long = Global.Long(10L, true)
        long.unsigned = ULong.MAX_VALUE
        assertThat(long.value).isEqualTo(-1)
    }
}
