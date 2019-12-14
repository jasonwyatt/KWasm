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

package kwasm.format.text

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.text.ParseException

@RunWith(JUnit4::class)
@UseExperimental(ExperimentalUnsignedTypes::class)
class LiteralTest {
    private val tokenizer = Tokenizer()

    @Test
    fun throws_ifLiteral_isNotUInt_whenDesired() {
        val exception = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("0x5p1").parseLiteral<UInt>(0)
        }
        assertThat(exception).hasMessageThat().contains("Expected i32")
    }

    @Test
    fun throws_ifLiteral_isNotInt_whenDesired() {
        val exception = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("0x5p1").parseLiteral<Int>(0)
        }
        assertThat(exception).hasMessageThat().contains("Expected i32")
    }

    @Test
    fun throws_ifLiteral_isNotULong_whenDesired() {
        val exception = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("0x5p1").parseLiteral<ULong>(0)
        }
        assertThat(exception).hasMessageThat().contains("Expected i64")
    }

    @Test
    fun throws_ifLiteral_isNotLong_whenDesired() {
        val exception = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("0x5p1").parseLiteral<Long>(0)
        }
        assertThat(exception).hasMessageThat().contains("Expected i64")
    }
}
