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

package kwasm.runtime.instruction

import com.google.common.truth.StandardSubjectBuilder
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kwasm.ParseRule
import kwasm.ast.instruction.Instruction
import kwasm.runtime.ExecutionContext
import kwasm.runtime.Value
import kwasm.runtime.toValue
import org.junit.Assert.assertThrows
import kotlin.reflect.KClass

internal fun instructionCases(
    parser: ParseRule,
    instructionSource: String,
    block: InstructionTestBuilder.() -> Unit
) {
    InstructionTestBuilder(parser, instructionSource).apply(block)
}

internal class InstructionTestBuilder(
    val parser: ParseRule,
    private val instructionSource: String
) {
    var context: ExecutionContext? = null

    fun errorCase(
        errorClass: KClass<out Throwable>,
        expectedMessage: String,
        vararg inputs: Number
    ) = apply {
        ErrorTestCase(parser, context!!, errorClass, expectedMessage, *inputs)
            .check(instructionSource)
    }

    fun validCase(
        expectedOutput: Number,
        vararg inputs: Number
    ) = apply {
        TestCase(parser, context!!, expectedOutput, *inputs).check(instructionSource)
    }

    fun validVoidCase(vararg inputs: Number) = apply {
        TestCase(parser, context!!, null, *inputs).check(instructionSource)
    }
}

interface InstructionChecker {
    fun check(source: String)
}

internal class TestCase(
    val parser: ParseRule,
    val context: ExecutionContext,
    val expected: Number?,
    vararg val opStack: Number
) : InstructionChecker {
    override fun check(source: String) {
        var instruction: Instruction? = null
        parser.with { instruction = source.parseInstruction() }

        val resultContext = instruction!!.execute(
            context.withOpStack(opStack.map { it.toValue() })
        )

        val inputStr = opStack.joinToString(prefix = "[", postfix = "]")
        assertWithMessage("$source with input $inputStr should output $expected")
            .thatContext(resultContext)
            .also {
                expected?.let { exp -> it.hasOpStackContaining(exp.toValue()) }
                    ?: run { it.hasOpStackContaining() }
            }
    }
}

internal class ErrorTestCase(
    val parser: ParseRule,
    val context: ExecutionContext,
    val expectedThrowable: KClass<out Throwable>,
    val expectedMessage: String,
    vararg val opStack: Number
) : InstructionChecker {
    override fun check(source: String) {
        var instruction: Instruction? = null
        parser.with { instruction = source.parseInstruction() }

        assertThrows(expectedThrowable.java) {
            instruction!!.execute(
                context.withOpStack(opStack.map { it.toValue() })
            )
        }.also { assertThat(it).hasMessageThat().contains(expectedMessage) }
    }
}

internal fun ExecutionContext.withOpStack(values: List<Value<*>>): ExecutionContext {
    stacks.operands.clear()
    values.forEach { stackVal -> stacks.operands.push(stackVal) }
    return this
}

internal class ExecutionContextSubject(
    private val builder: StandardSubjectBuilder,
    private val context: ExecutionContext
) {
    fun hasOpStackContaining(vararg vals: Value<*>) {
        val stack = mutableListOf<Value<*>>()
        while (context.stacks.operands.peek() != null) {
            stack += context.stacks.operands.pop()
        }
        stack.reverse()
        builder.that(stack).containsExactly(*vals).inOrder()
    }
}

internal fun StandardSubjectBuilder.thatContext(context: ExecutionContext) =
    ExecutionContextSubject(this, context)
