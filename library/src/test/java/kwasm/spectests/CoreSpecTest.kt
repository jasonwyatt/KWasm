/*
 * Copyright 2021 Google LLC
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

package kwasm.spectests

import kwasm.format.ParseContext
import org.junit.runner.RunWith
import java.io.InputStream
import java.io.InputStreamReader

@RunWith(SpecTestRunner::class)
class CoreSpecTest {
    @SpecTest(
        subdir = "core",
        files = ["binary.wast", "traps.wast"],
    )
    fun scriptTest(input: InputStream, file: String) {
        runScript(InputStreamReader(input), ParseContext(file))
    }
}