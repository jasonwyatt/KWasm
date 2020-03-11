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

import com.google.common.truth.Truth.assertThat
import kwasm.KWasmRuntimeException
import kwasm.ParseRule
import kwasm.runtime.ExecutionContext
import kwasm.runtime.ModuleInstance
import kwasm.runtime.Store
import kwasm.runtime.stack.RuntimeStacks
import kwasm.runtime.util.AddressIndex
import kwasm.runtime.util.TypeIndex
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ControlInstructionTest {
    @get:Rule
    val parser = ParseRule()

    private val emptyContext: ExecutionContext
        get() = ExecutionContext(
            Store(),
            ModuleInstance(
                TypeIndex(),
                AddressIndex(),
                AddressIndex(),
                AddressIndex(),
                AddressIndex(),
                emptyList()
            ),
            RuntimeStacks()
        )

    @Test
    fun block_empty() = instructionCases(
        parser,
        """
           (block)
        """
    ) {
        context = emptyContext
        validCase(1f,  1f)
        assertThat(context).isEqualTo(emptyContext)
    }

    @Test
    fun block_producer() = instructionCases(
        parser,
        """
            (block
                (i32.add (i32.const 1) (i32.const 2))
                i32.const 3
                i32.add
            )
        """
    ) {
        context = emptyContext
        validCase(6)
        assertThat(context.stacks.labels.height).isEqualTo(0)
    }

    @Test
    fun block_consumer() = instructionCases(
        parser,
        """
            (block i32.add i32.add)
        """
    ) {
        context = emptyContext
        validCase(6, 1, 2, 3)
        assertThat(context.stacks.labels.height).isEqualTo(0)
    }

    @Test
    fun block_withResultType_butEmptyStack() = instructionCases(
        parser,
        """
            (block (result i64)
                i64.const 1
                drop
            )
        """
    ) {
        context = emptyContext
        errorCase(
            KWasmRuntimeException::class,
            "expected to exit with i64 on the stack"
        )
    }

    @Test
    fun block_withResultType_mismatch() = instructionCases(
        parser,
        """
            (block (result i64)
                i32.add
            )
        """
    ) {
        context = emptyContext
        errorCase(
            KWasmRuntimeException::class,
            "expected to exit with i64 on the top of the stack, but found i32",
            2,
            3
        )
    }

    @Test
    fun unreachable() = instructionCases(parser, "unreachable") {
        context = emptyContext
        errorCase(KWasmRuntimeException::class, "unreachable instruction reached")
    }

    @Test
    fun nop() = instructionCases(parser, "nop") {
        context = emptyContext
        validCase(1f, 1f)
        assertThat(context).isEqualTo(emptyContext)
    }
}
