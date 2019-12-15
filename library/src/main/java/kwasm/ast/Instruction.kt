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

package kwasm.ast

/** Base for all instruction [AstNode] implementations. */
interface Instruction : AstNode

/** Base for all [Instruction]s which return some value. */
interface Operation : Instruction {
    val returnType: ValueTypeEnum
}

/** Base for all [Instruction]s which return a constant value. */
interface Constant<T> : Instruction {
    /** [Literal] node which contains the constant's value. */
    val valueAstNode: Literal<T>
}

/** Defines an [AstNode] which represents an argument to an [Instruction]. */
interface Argument {
    /** Node which calculates the value of the argument. */
    val valueAstNode: AstNode
}

/**
 * Base for all [Instruction] implementations which require [Argument]s.
 *
 * **Note** Don't extend this directly. Instead, use one of:
 *
 * * [UnaryOperation],
 * * [BinaryOperation],
 * * [TestInstruction],
 * * [Comparison], or
 * * [Conversion].
 */
interface ArgumentedInstruction : Instruction {
    val arguments: List<Argument>
}

/**
 * Base for all [Instruction] implementations which require a single [Argument].
 *
 * **Note** Don't extend this directly. Instead, use one of:
 *
 * * [UnaryOperation]
 * * [TestInstruction]
 * * [Conversion]
 */
interface UnaryInstruction<X : Argument> : ArgumentedInstruction {
    var arg: X

    override val arguments: List<Argument>
        get() = listOf(arg)
}

/**
 * Base for all [Instruction] implementations which require two [Argument]s.
 *
 * **Note** Don't extend this directly. Instead, use one of:
 *
 * * [BinaryOperation]
 * * [Comparison]
 */
interface BinaryInstruction<X : Argument, Y : Argument> : ArgumentedInstruction {
    var argX: X
    var argY: Y

    override val arguments: List<Argument>
        get() = listOf(argX, argY)
}

/**
 * Base for all [Operation]s which require a single [Argument] and return a value of type
 * [returnType].
 */
interface UnaryOperation<X : Argument> : UnaryInstruction<X>, Operation

/**
 * Base for all [Operation]s which require a two [Argument]s and return a value of type
 * [returnType].
 */
interface BinaryOperation<X : Argument, Y : Argument> : BinaryInstruction<X, Y>, Operation

/**
 * Base for all [Instruction]s which require a single [Argument] and return a [Boolean] value.
 */
interface TestInstruction<X : Argument> : UnaryInstruction<X>

/**
 * Base for all [Instruction]s which compare two [Argument]s and return a [Boolean] value.
 */
interface Comparison<X : Argument, Y : Argument> : BinaryInstruction<X, Y>

/** Base for all [Operation]s which take one [Argument] and convert it to its [returnType]. */
interface Conversion<From : Argument> : UnaryOperation<From>
