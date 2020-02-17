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

/**
 * From [the docs]():
 *
 * A global instance is the runtime representation of a global variable. It holds an individual
 * value and a flag indicating whether it is mutable.
 *
 * ```
 *   globalinst ::= {value val, mut mut}
 * ```
 *
 * The value of mutable globals can be mutated through variable instructions or by external means
 * provided by the embedder.
 */
@Suppress("EXPERIMENTAL_API_USAGE")
sealed class Global<T> {
    abstract var value: T
    abstract val mutable: Boolean

    data class Int(
        override var value: kotlin.Int,
        override val mutable: Boolean
    ) : Global<kotlin.Int>() {
        var unsigned: UInt
            get() = value.toUInt()
            set(value) {
                this.value = value.toInt()
            }
    }

    data class Long(
        override var value: kotlin.Long,
        override val mutable: Boolean
    ) : Global<kotlin.Long>() {
        var unsigned: ULong
            get() = value.toULong()
            set(value) {
                this.value = value.toLong()
            }
    }

    data class Float(
        override var value: kotlin.Float,
        override val mutable: Boolean
    ) : Global<kotlin.Float>()

    data class Double(
        override var value: kotlin.Double,
        override val mutable: Boolean
    ) : Global<kotlin.Double>()
}
