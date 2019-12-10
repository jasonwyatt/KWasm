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

sealed class Type(
    protected val sequence: CharSequence,
    protected val context: ParseContext? = null
) {

    protected abstract fun parseValue(): Unit

    class ValueType(
        sequence: CharSequence,
        context: ParseContext? = null
    ) : Type(sequence, context) {
        override fun parseValue() {
            TODO("not implemented")
        }
    }

    class ResultType(
        sequence: CharSequence,
        context: ParseContext? = null
    ) : Type(sequence, context) {
        override fun parseValue() {
            TODO("not implemented")
        }
    }

    class FunctionType(
        sequence: CharSequence,
        context: ParseContext? = null
    ) : Type(sequence, context) {
        override fun parseValue() {
            TODO("not implemented")
        }
    }

    class Param(
        sequence: CharSequence,
        context: ParseContext? = null
    ) : Type(sequence, context) {
        override fun parseValue() {
            TODO("not implemented")
        }
    }

    class Result(
        sequence: CharSequence,
        context: ParseContext? = null
    ) : Type(sequence, context) {
        override fun parseValue() {
            TODO("not implemented")
        }
    }

    class Limits(
        sequence: CharSequence,
        context: ParseContext? = null
    ) : Type(sequence, context) {
        override fun parseValue() {
            TODO("not implemented")
        }
    }

    class MemoryType(
        sequence: CharSequence,
        context: ParseContext? = null
    ) : Type(sequence, context) {
        override fun parseValue() {
            TODO("not implemented")
        }
    }

    class TableType(
        sequence: CharSequence,
        context: ParseContext? = null
    ) : Type(sequence, context) {
        override fun parseValue() {
            TODO("not implemented")
        }
    }

    class ElementType(
        sequence: CharSequence,
        context: ParseContext? = null
    ) : Type(sequence, context) {
        override fun parseValue() {
            TODO("not implemented")
        }
    }

    class GlobalType(
        sequence: CharSequence,
        context: ParseContext? = null
    ) : Type(sequence, context) {
        override fun parseValue() {
            TODO("not implemented")
        }
    }
}