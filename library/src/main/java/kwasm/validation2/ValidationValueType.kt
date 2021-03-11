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

package kwasm.validation2

import kwasm.ast.type.ValueType

enum class ValidationValueType {
    I32, I64, F32, F64, UNKNOWN;
}

fun ValueType.toValidationValueType(): ValidationValueType {
    return when (this) {
        ValueType.I32 -> ValidationValueType.I32
        ValueType.I64 -> ValidationValueType.I64
        ValueType.F32 -> ValidationValueType.F32
        ValueType.F64 -> ValidationValueType.F64
    }
}
