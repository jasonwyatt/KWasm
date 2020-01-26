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

package kwasm.runtime.memory

import kwasm.util.Impossible

internal inline fun <reified T> Int.assertValidByteWidth() =
    when (T::class) {
        Int::class, Float::class ->
            require(this == 1 || this == 2 || this == 4) {
                "Invalid byte width for Int/Float"
            }
        Long::class, Double::class ->
            require(this == 1 || this == 2 || this == 4 || this == 8) {
                "Invalid byte width for Long/Double"
            }
        else -> Impossible()
    }
