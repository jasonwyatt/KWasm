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

import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.shiftColumnBy

/**
 * This sealed class encapsulates all Types defined in
 * [the docs](https://webassembly.github.io/spec/core/text/types.html)
 *
 * @param T the Object representing the value related to the type and value parsed
 */
sealed class Type<T>(
    protected val sequence: CharSequence,
    protected val context: ParseContext? = null
) {
    val value: T by lazy { parseValue() }

    protected abstract fun parseValue(): T
    /**
     * Encapsulates kwasm.ast.ValueType for parsing. Parses the sequence in accordance
     * to the definition in [the docs](https://webassembly.github.io/spec/core/text/types.html#value-types)
     *
     * @constructor Parses the sequence passed in and populates the appropriate parsed value.
     * @throws ParseException when the sequence falls outside of the spec definition
     */
    class ValueType(
        sequence: CharSequence,
        context: ParseContext? = null
    ) : Type<kwasm.ast.ValueType>(sequence, context) {
        override fun parseValue(): kwasm.ast.ValueType = when (sequence){
                "i32" -> kwasm.ast.ValueType.I32
                "i64" -> kwasm.ast.ValueType.I64
                "f32" -> kwasm.ast.ValueType.F32
                "f64" -> kwasm.ast.ValueType.F64
                else -> throw ParseException("Invalid ValueType", context)
            }
        }

    class ResultType(
        sequence: CharSequence,
        context: ParseContext? = null
    ) : Type<kwasm.ast.ValueType?>(sequence, context) {
        override fun parseValue(): kwasm.ast.ValueType? {
            if (sequence.isEmpty()) return null
            val keywordAndParameters = getOperationAndParameters(sequence, context)
            if(keywordAndParameters.first != "result" || keywordAndParameters.second.size > 1){
                throw ParseException("Invalid ResultType syntax", context.shiftColumnBy(1))
            }
            // Context is shifted by 8 to shift past '(result ' to the start of the actual ValueType
            return ValueType(keywordAndParameters.second[0], context.shiftColumnBy(8)).value
        }
    }

    class FunctionType(
        sequence: CharSequence,
        context: ParseContext? = null
    ) : Type<Unit>(sequence, context) {
        override fun parseValue() {
            TODO("not implemented")
        }
    }

    class Param(
        sequence: CharSequence,
        context: ParseContext? = null
    ) : Type<Unit>(sequence, context) {
        override fun parseValue() {
            TODO("not implemented")
        }
    }

    class Result(
        sequence: CharSequence,
        context: ParseContext? = null
    ) : Type<Unit>(sequence, context) {
        override fun parseValue() {
            TODO("not implemented")
        }
    }

    class Limits(
        sequence: CharSequence,
        context: ParseContext? = null
    ) : Type<Unit>(sequence, context) {
        override fun parseValue() {
            TODO("not implemented")
        }
    }

    class MemoryType(
        sequence: CharSequence,
        context: ParseContext? = null
    ) : Type<Unit>(sequence, context) {
        override fun parseValue() {
            TODO("not implemented")
        }
    }

    class TableType(
        sequence: CharSequence,
        context: ParseContext? = null
    ) : Type<Unit>(sequence, context) {
        override fun parseValue() {
            TODO("not implemented")
        }
    }

    class ElementType(
        sequence: CharSequence,
        context: ParseContext? = null
    ) : Type<Unit>(sequence, context) {
        override fun parseValue() {
            TODO("not implemented")
        }
    }

    class GlobalType(
        sequence: CharSequence,
        context: ParseContext? = null
    ) : Type<Unit>(sequence, context) {
        override fun parseValue() {
            TODO("not implemented")
        }
    }
}