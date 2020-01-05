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

package kwasm.ast.type

import kotlin.reflect.KClass
import kwasm.ast.AstNode
import kwasm.util.Impossible

/**
 * From [the docs](https://webassembly.github.io/spec/core/text/types.html#value-types):
 *
 * ```
 *   valtype ::=  { 'i32' -> I32
 *                  'i64' -> I64
 *                  'f32' -> F32
 *                  'f64' -> F64 }
 * ```
 */
sealed class ValueType : AstNode {
    object I32 : ValueType()
    object I64 : ValueType()
    object F32 : ValueType()
    object F64 : ValueType()

    companion object {
        fun forClass(klass: KClass<out ValueType>): ValueType = when (klass) {
            I32::class -> I32
            I64::class -> I64
            F32::class -> F32
            F64::class -> F64
            else -> Impossible()
        }
    }
}
