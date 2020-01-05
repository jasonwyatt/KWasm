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

package kwasm.ast.instruction

import kwasm.ast.AstNode
import kwasm.ast.Literal
import kwasm.ast.type.ValueType

/** Base for all instruction [AstNode] implementations. */
interface Instruction : AstNode {
    val isPlain: Boolean
        get() = true
}

/** Base for all instruction [AstNode] implementations which are "block" instructions. */
interface BlockInstruction : Instruction {
    override val isPlain: Boolean
        get() = false
}

/** Base for all [Instruction]s which return some value. */
interface Operation<ReturnType : ValueType> :
    Instruction

/** Gets the return type for the [Operation]. */
inline val <reified ReturnType : ValueType> Operation<ReturnType>.output
    get() = ValueType.forClass(ReturnType::class)

/** Base for all [Instruction]s which return a constant value. */
interface Constant<T, ReturnType : ValueType> :
    Instruction {
    /** [Literal] node which contains the constant's value. */
    val value: Literal<T>
}

/** Gets the output [ValueType] for a [Constant] instruction. */
inline val <reified ReturnType : ValueType> Constant<Any, ReturnType>.output: ValueType
    get() = ValueType.forClass(ReturnType::class)

/** Defines an [AstNode] which represents an argument to an [Instruction]. */
interface Argument {
    /** Node which calculates the value of the argument. */
    val valueAstNode: AstNode
}

/**
 * Base for all [Instruction] implementations which require a single [Argument].
 *
 * **Note** You typically won't extend this directly. Instead, use one of:
 *
 * * [UnaryOperation]
 * * [TestInstruction]
 * * [Conversion]
 *
 * Exceptions include [MemoryInstruction] implementations.
 */
interface UnaryInstruction<X : ValueType> :
    Instruction

/** Gets the [ValueType] needed by the [UnaryInstruction]. */
inline val <reified X : ValueType> UnaryInstruction<X>.input: ValueType
    get() = ValueType.forClass(X::class)

/**
 * Base for all [Instruction] implementations which require two [Argument]s.
 *
 * **Note** Don't extend this directly. Instead, use one of:
 *
 * * [BinaryOperation]
 * * [Comparison]
 */
interface BinaryInstruction<X : ValueType, Y : ValueType> :
    Instruction

/**
 * Gets the [ValueType]s needed by the [BinaryInstruction].
 */
inline val <reified X, reified Y> BinaryInstruction<X, Y>.inputs: Pair<ValueType, ValueType>
    where X : ValueType, Y : ValueType
    get() {
        val first = ValueType.forClass(X::class)
        val second = ValueType.forClass(Y::class)
        return Pair(first, second)
    }

/**
 * Base for all [Operation]s which require a single [ValueType] ([Input]) and return a value of type
 * [ReturnType].
 */
interface UnaryOperation<Input : ValueType, ReturnType : ValueType> :
    UnaryInstruction<Input>,
    Operation<ReturnType>

/**
 * Base for all [Operation]s which require a two [Argument]s and return a value of type
 * [ReturnType].
 */
interface BinaryOperation<X : ValueType, Y : ValueType, ReturnType : ValueType> :
    BinaryInstruction<X, Y>,
    Operation<ReturnType>

/**
 * Base for all [Instruction]s which require a single [Argument] and return a [Boolean] value.
 */
interface TestInstruction<X : ValueType> :
    UnaryOperation<X, ValueType.I32>

/**
 * Base for all [Instruction]s which compare two inputs (of types [X] and [Y]) and return a
 * [Boolean] value.
 */
interface Comparison<X : ValueType, Y : ValueType> :
    BinaryOperation<X, Y, ValueType.I32>

/**
 * Base for all [Operation]s which take one [ValueType] ([From]) and convert it to its [To] type.
 */
interface Conversion<From : ValueType, To : ValueType> :
    UnaryOperation<From, To>
