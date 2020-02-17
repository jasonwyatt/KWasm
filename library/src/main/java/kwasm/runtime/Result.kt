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

import kwasm.runtime.stack.Value

/**
 * Represents the result of a computation.
 *
 * From [the docs](https://webassembly.github.io/spec/core/exec/runtime.html#results):
 *
 * A result is the outcome of a computation. It is either a sequence of values or a trap.
 *
 * ```
 *   result ::= val*
 *              trap
 * ```
 *
 * **Note:**
 *
 * In the current version of WebAssembly, a result can consist of at most one value.
 */
sealed class Result {
    /** Successful result of computation. */
    data class Ok<T : Number>(val value: Value<T>) : Result()

    /** An error occurred during computation. */
    data class Trap(val exception: Throwable) : Result()
}
