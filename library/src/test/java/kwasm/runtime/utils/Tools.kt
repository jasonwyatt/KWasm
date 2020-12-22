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

package kwasm.runtime.utils

import com.google.common.truth.StandardSubjectBuilder
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kwasm.ParseRule
import kwasm.api.HostFunction
import kwasm.ast.AstNodeList
import kwasm.ast.Identifier
import kwasm.ast.instruction.Instruction
import kwasm.ast.module.Index
import kwasm.ast.module.WasmFunction
import kwasm.ast.util.toFunctionIndex
import kwasm.format.text.module.parseWasmFunction
import kwasm.runtime.Address
import kwasm.runtime.ExecutionContext
import kwasm.runtime.FunctionInstance
import kwasm.runtime.Table
import kwasm.runtime.Value
import kwasm.runtime.instruction.execute
import kwasm.runtime.stack.Activation
import kwasm.runtime.toFunctionInstance
import kwasm.runtime.toValue
import kwasm.runtime.util.AddressIndex
import kwasm.runtime.util.LocalIndex
import kwasm.runtime.util.TypeIndex
import org.junit.Assert.assertThrows
import kotlin.reflect.KClass

internal fun instructionCases(
    parser: ParseRule,
    instructionSource: String,
    block: InstructionTestBuilder.() -> Unit
) {
    InstructionTestBuilder(parser, instructionSource).apply(block)
}

internal fun functionCases(
    parser: ParseRule,
    functionSource: String,
    block: FunctionTestBuilder.() -> Unit
) {
    FunctionTestBuilder(parser, functionSource).apply(block)
}

internal class InstructionTestBuilder(
    val parser: ParseRule,
    private val instructionSource: String
) {
    lateinit var context: ExecutionContext

    fun errorCase(
        errorClass: KClass<out Throwable>,
        expectedMessage: String,
        vararg inputs: Number
    ) = apply {
        ErrorTestCase(
            parser,
            context,
            errorClass,
            expectedMessage,
            *inputs
        )
            .check(instructionSource)
    }

    fun validCase(expectedOutput: Number, vararg inputs: Number) = apply {
        context = TestCase(
            parser,
            context,
            listOf(expectedOutput),
            *inputs
        )
            .check(instructionSource)
    }

    fun validCase(expectedStack: List<Number>, vararg inputs: Number) = apply {
        context = TestCase(
            parser,
            context,
            expectedStack,
            *inputs
        ).check(instructionSource)
    }

    fun validVoidCase(vararg inputs: Number) = apply {
        context = TestCase(
            parser,
            context,
            emptyList(),
            *inputs
        ).check(instructionSource)
    }
}

internal class FunctionTestBuilder(
    val parser: ParseRule,
    private val functionSource: String
) {
    lateinit var context: ExecutionContext

    fun errorCase(
        errorClass: KClass<out Throwable>,
        expectedMessage: String,
        vararg inputs: Number
    ) = apply {
        ErrorTestCase(
            parser,
            context,
            errorClass,
            expectedMessage,
            *inputs
        ).checkFunction(functionSource)
    }

    fun validCase(expectedOutput: Number, vararg inputs: Number) = apply {
        context = TestCase(
            parser,
            context,
            listOf(expectedOutput),
            *inputs
        ).checkFunction(functionSource)
    }

    fun validCase(expectedStack: List<Number>, vararg inputs: Number) = apply {
        context = TestCase(
            parser,
            context,
            expectedStack,
            *inputs
        ).checkFunction(functionSource)
    }

    fun validVoidCase(vararg inputs: Number) = apply {
        context = TestCase(
            parser,
            context,
            emptyList(),
            *inputs
        ).checkFunction(functionSource)
    }
}
internal interface InstructionChecker {
    fun check(source: String): ExecutionContext
    fun checkFunction(source: String): ExecutionContext
}

internal class TestCase(
    val parser: ParseRule,
    val context: ExecutionContext,
    val expected: List<Number>,
    vararg val opStack: Number
) : InstructionChecker {
    override fun check(source: String): ExecutionContext {
        var instructions: AstNodeList<out Instruction>? = null
        parser.with { instructions = source.parseInstructions() }

        val resultContext = instructions!!.execute(
            context.withOpStack(opStack.map { it.toValue() })
        )

        checkOutput(source.trimIndent(), resultContext)

        return resultContext
    }

    override fun checkFunction(source: String): ExecutionContext {
        var wasmFunction: WasmFunction? = null
        parser.with { wasmFunction = source.tokenize().parseWasmFunction(0)!!.astNode }

        val resultContext = FunctionInstance.Module(context.moduleInstance, wasmFunction!!).execute(
            context.withOpStack(opStack.map { it.toValue() })
        )

        checkOutput(source.trimIndent(), resultContext)

        return resultContext
    }

    private fun checkOutput(source: String, resultContext: ExecutionContext) {
        val inputStr = opStack.joinToString(prefix = "[", postfix = "]")
        assertWithMessage("$source with input $inputStr should output $expected")
            .thatContext(resultContext)
            .also { subj ->
                subj.hasOpStackContaining(*(expected.map { it.toValue() }.toTypedArray()))
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
    override fun check(source: String): ExecutionContext {
        var instructions: List<Instruction>? = null
        parser.with { instructions = source.parseInstructions() }

        assertThrows(expectedThrowable.java) {
            instructions!!.execute(
                context.withOpStack(opStack.map { it.toValue() })
            )
        }.also { assertThat(it).hasMessageThat().contains(expectedMessage) }
        return context
    }

    override fun checkFunction(source: String): ExecutionContext {
        var wasmFunction: WasmFunction? = null
        parser.with { wasmFunction = source.tokenize().parseWasmFunction(0)!!.astNode }

        assertThrows(expectedThrowable.java) {
            FunctionInstance.Module(context.moduleInstance, wasmFunction!!).execute(
                context.withOpStack(opStack.map { it.toValue() })
            )
        }.also { assertThat(it).hasMessageThat().contains(expectedMessage) }
        return context
    }
}

internal fun ExecutionContext.withOpStack(values: List<Value<*>>): ExecutionContext {
    stacks.operands.clear()
    values.forEach { stackVal -> stacks.operands.push(stackVal) }
    return this
}

internal fun ExecutionContext.withHostFunction(
    hostFunctionId: String,
    hostFunction: HostFunction<*>
): ExecutionContext {
    val functionsSoFar = store.functions.toMutableList()
    moduleInstance.functionAddresses.add(
        Address.Function(functionsSoFar.size),
        Identifier.Function(hostFunctionId)
    )
    functionsSoFar.add(hostFunction.toFunctionInstance())

    return this.copy(store = store.copy(functions = functionsSoFar))
}

internal fun ExecutionContext.withEmptyFrame(): ExecutionContext {
    stacks.activations.push(
        Activation(
            "blah".toFunctionIndex(),
            LocalIndex(),
            moduleInstance
        )
    )
    return this
}

internal fun ExecutionContext.withFrameReturning(arity: Int): ExecutionContext {
    stacks.activations.push(
        Activation(
            "blah".toFunctionIndex(),
            LocalIndex(),
            moduleInstance,
            arity
        )
    )
    return this
}

internal fun ExecutionContext.withFrameContainingLocals(
    locals: Map<Index<Identifier.Local>, Value<*>>
): ExecutionContext {
    stacks.activations.push(
        Activation(
            "foo".toFunctionIndex(),
            LocalIndex().also {
                locals.forEach { (index, value) -> it[index] = value }
            },
            moduleInstance
        )
    )
    return this
}

internal fun ExecutionContext.withTable(vararg functions: FunctionInstance): ExecutionContext {
    val store = store.copy(
        functions = functions.toList(),
        tables = listOf(
            Table(functions.mapIndexed { i, _ -> i to Address.Function(i) }.toMap().toMutableMap())
        )
    )
    val moduleInstance = moduleInstance.copy(
        types = TypeIndex(functions.map { it.type }),
        functionAddresses = AddressIndex(functions.mapIndexed { i, _ -> Address.Function(i) }),
        tableAddresses = AddressIndex(listOf(Address.Table(0)))
    )
    return copy(store = store, moduleInstance = moduleInstance)
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
