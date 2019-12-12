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

import kwasm.ast.FunctionType
import kwasm.ast.GlobalType
import kwasm.ast.Identifier
import kwasm.ast.Limit
import kwasm.ast.Memory
import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.shiftColumnBy
import kwasm.format.text.token.IntegerLiteral

/**
 * This sealed class encapsulates all Types defined in
 * [the docs](https://webassembly.github.io/spec/core/text/types.html)
 *
 * @param T the Object representing the value related to the type and value parsed
 */
@UseExperimental(ExperimentalUnsignedTypes::class)
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
    ) : Type<kwasm.ast.ValueTypeEnum>(sequence, context) {
        override fun parseValue(): kwasm.ast.ValueTypeEnum = when (sequence) {
            "i32" -> kwasm.ast.ValueTypeEnum.I32
            "i64" -> kwasm.ast.ValueTypeEnum.I64
            "f32" -> kwasm.ast.ValueTypeEnum.F32
            "f64" -> kwasm.ast.ValueTypeEnum.F64
            else -> throw ParseException("Invalid ValueType", context)
        }
    }

    class ResultType(
        sequence: CharSequence,
        context: ParseContext? = null
    ) : Type<Result?>(sequence, context) {
        override fun parseValue(): Result? {
            if (sequence.isEmpty()) return null
            return Result(sequence, context)
        }
    }

    /**
     * Encapsulates kwasm.ast.FunctionType for parsing. Parses the sequence in accordance
     * to the definition in [the docs](https://webassembly.github.io/spec/core/text/types.html#function-types)
     *
     * @constructor Parses the sequence passed in and populates the appropriate parsed value.
     * @throws ParseException when the sequence falls outside of the spec definition
     */
    class FunctionType(
        sequence: CharSequence,
        context: ParseContext? = null
    ) : Type<kwasm.ast.FunctionType>(sequence, context) {
        override fun parseValue(): kwasm.ast.FunctionType {
            val keywordAndParameters = getOperationAndParameters(sequence, context)
            if (keywordAndParameters.first != "func") {
                throw ParseException("Invalid FunctionType syntax", context.shiftColumnBy(1))
            } else {
                val params = mutableListOf<kwasm.ast.Param>()
                val returnValues = mutableListOf<kwasm.ast.ValueTypeEnum>()
                var inParenthesis = false
                var parsingParams = true
                var internalSequence = ""
                for (token in keywordAndParameters.second) {
                    when {
                        token.contains("(") -> {
                            inParenthesis = true
                            internalSequence += "$token "
                        }
                        token.contains(")") -> {
                            if (!inParenthesis) {
                                throw ParseException("Invalid FunctionType Syntax", context)
                            }
                            internalSequence += "$token"
                            inParenthesis = false

                            if (internalSequence.contains("(param") && parsingParams) {
                                params.add(Param(internalSequence, context).value)
                            } else if (internalSequence.contains("(result")) {
                                parsingParams = false
                                returnValues.add(Result(internalSequence, context).value.valTypeEnum)
                            } else {
                                throw ParseException("Invalid FunctionType Syntax", context)
                            }
                            internalSequence = ""
                        }
                        else -> internalSequence += "$token "
                    }
                }
                return FunctionType(params, returnValues)
            }
        }
    }

    /**
     * Encapsulates kwasm.ast.Param for parsing. Parses the sequence in accordance
     * to the definition in [the docs](https://webassembly.github.io/spec/core/text/types.html#value-types)
     *
     * @constructor Parses the sequence passed in and populates the appropriate parsed value.
     * @throws ParseException when the sequence falls outside of the spec definition
     */
    class Param(
        sequence: CharSequence,
        context: ParseContext? = null
    ) : Type<kwasm.ast.Param>(sequence, context) {
        override fun parseValue(): kwasm.ast.Param {
            val keywordAndParameters = getOperationAndParameters(sequence, context)
            if (keywordAndParameters.first != "param" || keywordAndParameters.second.size > 2) {
                throw ParseException("Invalid Param syntax", context.shiftColumnBy(1))
            }
            return if (keywordAndParameters.second.size == 1) {
                // Context is shifted by 7 to shift past '(param ' to the start of the actual ValueType
                kwasm.ast.Param(null, ValueType(keywordAndParameters.second[0], context.shiftColumnBy(7)).value)
            } else {
                // Context is shifted by 8 + id length to shift past '(param id ' to the start of the actual ValueType
                kwasm.ast.Param(
                    Identifier.Local(keywordAndParameters.second[0].toString()),
                    ValueType(
                        keywordAndParameters.second[1],
                        context.shiftColumnBy(8 + keywordAndParameters.second[0].length)
                    ).value
                )
            }
        }
    }

    class Result(
        sequence: CharSequence,
        context: ParseContext? = null
    ) : Type<kwasm.ast.Result>(sequence, context) {
        override fun parseValue(): kwasm.ast.Result {
            val keywordAndParameters = getOperationAndParameters(sequence, context)
            if (keywordAndParameters.first != "result" || keywordAndParameters.second.size > 1) {
                throw ParseException("Invalid Result syntax", context.shiftColumnBy(1))
            }
            // Context is shifted by 8 to shift past '(result ' to the start of the actual ValueType
            return kwasm.ast.Result(
                ValueType(
                    keywordAndParameters.second[0],
                    context.shiftColumnBy(8)
                ).value
            )
        }
    }

    /**
     * From [the spec](]https://webassembly.github.io/spec/core/text/types.html#limits):
     *
     * ```
     *   limits ::=  n:u32        => {min n, max ϵ}
     *           |   n:u32  m:u32 => {min n, max m}
     * ```
     */
    class Limits(
        sequence: CharSequence,
        context: ParseContext? = null
    ) : Type<Limit>(sequence, context) {

        override fun parseValue(): Limit {
            // If sequence doesn't contain a space, that means we are only dealing with 1 number
            return if (" " !in sequence) {
                if (sequence.isEmpty()) {
                    throw ParseException("Invalid number of arguments. Expected 1 or 2 but found 0", context)
                }
                val min = IntegerLiteral.Unsigned(sequence, 32, context).value.toUInt()
                Limit(min, IntegerLiteral.Unsigned(UInt.MAX_VALUE.toString(), 32, null).value.toUInt())
            } else {
                val numbers = sequence.split(" ")
                if (numbers.size != 2) {
                    throw ParseException(
                        "Invalid number of arguments. Expected 1 or 2 but found ${numbers.size}",
                        context
                    )
                }
                val min = IntegerLiteral.Unsigned(numbers[0], 32, context).value.toUInt()
                val max = IntegerLiteral.Unsigned(
                    numbers[1], 32,
                    context.shiftColumnBy(numbers[0].length + 1)
                ).value.toUInt()
                if (min > max) {
                    // We must undo the context shift if we encounter this error
                    throw ParseException("Invalid Range specified, min > max. Found min: $min, max: $max", context)
                }
                Limit(min, max)
            }
        }
    }

    /**
     * From [the spec](https://webassembly.github.io/spec/core/text/types.html#memory-types):
     * ```
     *   memtype ::= lim:limits => lim
     * ```
     */
    class MemoryType(
        sequence: CharSequence,
        context: ParseContext? = null
    ) : Type<Memory>(sequence, context) {
        override fun parseValue(): Memory = Memory(Limits(sequence, context).value)
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

    /**
     * Encapsulates kwasm.ast.GlobalType for parsing. Parses the sequence in accordance
     * to the definition in [the docs](https://webassembly.github.io/spec/core/text/types.html#global-types)
     *
     * @constructor Parses the sequence passed in and populates the appropriate parsed value.
     * @throws ParseException when the sequence falls outside of the spec definition
     */
    class GlobalType(
        sequence: CharSequence,
        context: ParseContext? = null
    ) : Type<kwasm.ast.GlobalType>(sequence, context) {
        override fun parseValue(): kwasm.ast.GlobalType {
            // The immutable case is just a ValueType
            return if (sequence.first() != '(') {
                GlobalType(ValueType(sequence, context).value, false)
            } else {
                val keywordAndParameters = getOperationAndParameters(sequence, context)
                if (keywordAndParameters.first != "mut" || keywordAndParameters.second.size > 1) {
                    throw ParseException("Invalid GlobalType syntax", context.shiftColumnBy(1))
                }
                // Context is shifted by 8 to shift past '(mut ' to the start of the actual ValueType
                GlobalType(
                    ValueType(
                        keywordAndParameters.second[0],
                        context.shiftColumnBy(5)
                    ).value, true
                )
            }
        }
    }
}
