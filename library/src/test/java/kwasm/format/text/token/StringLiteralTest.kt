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

package kwasm.format.text.token

import com.google.common.truth.Truth.assertThat
import kwasm.format.ParseContext
import kwasm.format.text.token.StringLiteral
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StringLiteralTest {
    @Test
    fun parsesEmptyString() {
        val actual = StringLiteral("\"\"")
        assertThat(actual.value).isEmpty()
    }

    @Test
    fun parsesSimpleString() {
        val actual =
            StringLiteral("\"This is a test of it's capabilities!\"")
        assertThat(actual.value).isEqualTo("This is a test of it's capabilities!")
    }

    @Test
    fun parsesComplexString() {
        val actual = StringLiteral(
            "\"Hello! \\u{1f44b}\\n\\u{4f60}\\u{597D} \\u{1F44B}\\n\\n\\\"wasm rocks\\\"\"",
            ParseContext("StringLiteralTest.kt", 0, 0)
        )
        assertThat(actual.value).isEqualTo("Hello! 👋\n你好 👋\n\n\"wasm rocks\"")
    }

    @Test
    fun parsesComplexString_withEmbeddedUnicode() {
        val actual =
            StringLiteral("\"Hello! 👋\\n你好 👋\\n\\n\\\"wasm rocks\\\"\"")
        assertThat(actual.value).isEqualTo("Hello! 👋\n你好 👋\n\n\"wasm rocks\"")
    }
}
