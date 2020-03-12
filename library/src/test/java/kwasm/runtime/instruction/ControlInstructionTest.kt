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
import kwasm.ast.util.toLocalIndex
import kwasm.runtime.ExecutionContext
import kwasm.runtime.ModuleInstance
import kwasm.runtime.Store
import kwasm.runtime.stack.RuntimeStacks
import kwasm.runtime.toValue
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
        validCase(1f, 1f)
        assertThat(context).isEqualTo(emptyContext)
    }

    @Test
    fun block_noResult_hasSameStackAsBefore() = instructionCases(
        parser,
        """
            i32.const 42
            (block
                i32.const 2
            )
        """
    ) {
        context = emptyContext
        validCase(42)
        validCase(listOf<Number>(1f, 42), 1f)
        assertThat(context).isEqualTo(emptyContext)
    }

    @Test
    fun block_producer() = instructionCases(
        parser,
        """
            (block (result i32)
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
            (block (result i32)
                local.get ${'$'}foo
                local.get ${'$'}bar
                i32.add
            )
        """
    ) {
        context = emptyContext.withFrameContainingLocals(
            mapOf(
                "\$foo".toLocalIndex() to 1.toValue(),
                "\$bar".toLocalIndex() to 2.toValue()
            )
        )
        validCase(3)
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
                i32.const 1
            )
        """
    ) {
        context = emptyContext
        errorCase(
            KWasmRuntimeException::class,
            "expected to exit with i64 on the top of the stack, but found i32"
        )
    }

    @Test
    fun loop_empty() = instructionCases(
        parser,
        """
            (loop)
        """
    ) {
        context = emptyContext
        validVoidCase()
        validCase(1, 1)
    }

    @Test
    fun loop_while() = instructionCases(
        parser,
        """
            (loop
                (; Increment local $0 ;)
                (local.set $0 
                    (i32.add (local.get $0) (i32.const 1))
                )
                (; Jump back to the start of the loop if local $0 is less than 10 ;)
                (br_if 0 (i32.lt_u (local.get $0) (i32.const 10)))
            )
            local.get $0
        """
    ) {
        context = emptyContext.withFrameContainingLocals(
            mapOf(
                "$0".toLocalIndex() to 0.toValue()
            )
        )
        validCase(10) // should've looped 10 times
    }

    @Test
    fun loop_while_usingIfElse() = instructionCases(
        parser,
        """
            (loop
                (if (i32.lt_u (local.get $0) (i32.const 10))
                    (then
                        (local.set $0
                            (i32.add (local.get $0) (i32.const 1))
                        )
                        br 1
                    )
                )
            )
            local.get $0
        """
    ) {
        context = emptyContext.withFrameContainingLocals(
            mapOf(
                "$0".toLocalIndex() to 0.toValue()
            )
        )
        validCase(10) // should've looped 10 times
    }

    @Test
    fun if_else() = instructionCases(
        parser,
        """
            (if (result i32) (local.get $0)
                (then
                    (i32.add (local.get $1) (local.get $2))
                )
                (else
                    (i32.mul (local.get $1) (local.get $2))
                )
            )
        """.trimIndent()
    ) {
        context = emptyContext.withFrameContainingLocals(
            mapOf(
                "$0".toLocalIndex() to 1.toValue(),
                "$1".toLocalIndex() to 3.toValue(),
                "$2".toLocalIndex() to 5.toValue()
            )
        )
        validCase(8) // 'then' branch should be taken

        context = emptyContext.withFrameContainingLocals(
            mapOf(
                "$0".toLocalIndex() to 0.toValue(),
                "$1".toLocalIndex() to 3.toValue(),
                "$2".toLocalIndex() to 5.toValue()
            )
        )
        validCase(15) // 'else' branch should be taken

        context = emptyContext.withFrameContainingLocals(
            mapOf(
                "$0".toLocalIndex() to (-1).toValue(),
                "$1".toLocalIndex() to 3.toValue(),
                "$2".toLocalIndex() to 5.toValue()
            )
        )
        validCase(8) // 'then' branch should be taken (condition is non-zero)
    }

    @Test
    fun if_throws_ifTopOfStack_invalid() = instructionCases(
        parser,
        """
            (if
                (then)
                (else)
            )
        """
    ) {
        context = emptyContext
        errorCase(IllegalStateException::class, "Stack: Op is empty")
        errorCase(KWasmRuntimeException::class, "if requires i32 at the top of the stack", 1L)
        errorCase(KWasmRuntimeException::class, "if requires i32 at the top of the stack", 1f)
        errorCase(KWasmRuntimeException::class, "if requires i32 at the top of the stack", 1.0)
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

    @Test
    fun br_0_noresult() = instructionCases(
        parser,
        """
            (block
                i32.const -1
                br 0
                i32.const 5
                i32.const 6
            )
        """
    ) {
        context = emptyContext
        validVoidCase() // no inputs should produce no outputs
        validCase(1, 1)
    }

    @Test
    fun br_0_result_middle() = instructionCases(
        parser,
        """
            (block (result i32)
                i32.const 5
                br 0
                i32.const 6
            )
        """
    ) {
        context = emptyContext
        validCase(5)
    }

    @Test
    fun br_0_result_laterMiddle() = instructionCases(
        parser,
        """
            (block (result i32)
                i32.const 5
                i32.const 6
                br 0
                i32.const 1
            )
        """
    ) {
        context = emptyContext
        validCase(6)
    }

    @Test
    fun br_0_result_end() = instructionCases(
        parser,
        """
            (block (result i32)
                i32.const 5
                i32.const 6
                i32.const 1
                br 0
            )
        """
    ) {
        context = emptyContext
        validCase(1)
    }

    @Test
    fun br_nested_jumpAllTheWayOut() = instructionCases(
        parser,
        """
            (block (result i32)
                (block 
                    (block 
                        i32.const 1
                        br 2
                    )
                    (; should get skipped ;)
                    unreachable
                )
                (; should also get skipped ;)
                unreachable
            )
        """.trimIndent()
    ) {
        context = emptyContext
        validCase(1)
    }

    @Test
    fun br_nested_jumpAllTheWayOut_byIdentifier() = instructionCases(
        parser,
        """
            (block ${'$'}foo (result i32)
                (block 
                    (block 
                        i32.const 1
                        br ${'$'}foo
                    )
                    (; should get skipped ;)
                    unreachable
                )
                (; should also get skipped ;)
                unreachable
            )
        """.trimIndent()
    ) {
        context = emptyContext
        validCase(1)
    }

    @Test
    fun br_nested_jumpPartlyOut() = instructionCases(
        parser,
        """
            (block (result i32)
                (block 
                    (block 
                        i32.const 1
                        br 1
                    )
                    (; should get skipped ;)
                    unreachable
                )
                (; should get jumped-to ;)
                i32.const 3
            )
        """.trimIndent()
    ) {
        context = emptyContext
        validCase(3)
    }

    @Test
    fun br_nested_jumpPartlyOutByIdentifier() = instructionCases(
        parser,
        """
            (block (result i32)
                (block ${'$'}foo 
                    (block 
                        i32.const 1
                        br ${'$'}foo
                    )
                    (; should get skipped ;)
                    unreachable
                )
                (; should get jumped-to ;)
                i32.const 3
            )
        """.trimIndent()
    ) {
        context = emptyContext
        validCase(3)
    }

    @Test
    fun br_if() = instructionCases(
        parser,
        """
            (block 
                (block
                    local.get ${'$'}foo
                    br_if 1
                )
                unreachable
            )
            i32.add
        """
    ) {
        context = emptyContext.withFrameContainingLocals(
            mapOf(
                "\$foo".toLocalIndex() to 1.toValue()
            )
        )
        validCase(1, 2, -1)

        context = emptyContext.withFrameContainingLocals(
            mapOf(
                "\$foo".toLocalIndex() to 0.toValue()
            )
        )
        errorCase(KWasmRuntimeException::class, "unreachable instruction reached", 1, 0)
    }

    @Test
    fun br_table() = instructionCases(
        parser,
        """
            (block (result i32)
                (block (result i32)
                    (block (result i32)
                        (block (result i32)
                            i32.const 99
                            local.get $0
                            br_table 1 2 3 0
                        )
                        i32.const 1 ;; jumping to 0 goes here
                        br 2 ;; exit
                    )
                    i32.const 2 ;; jumping to 1 goes here
                    br 1 ;; exit
                )
                i32.const 3 ;; jumping to 2 goes here
            )
            ;; jumping to 3 goes here (same as exiting)
        """.trimIndent()
    ) {
        context = emptyContext.withFrameContainingLocals(
            mapOf(
                "$0".toLocalIndex() to 3.toValue()
            )
        )
        validCase(1) // should use the default, since 3 == length of l*

        context = emptyContext.withFrameContainingLocals(
            mapOf(
                "$0".toLocalIndex() to 4.toValue()
            )
        )
        validCase(1) // should use the default, since 4 > length of l*
    }

    @Test
    fun br_table_inners() = instructionCases(
        parser,
        """
            (block (result i32)
                (block (result i32)
                    (block (result i32)
                        (block (result i32)
                            i32.const 99
                            local.get $0
                            br_table 1 2 3 0
                        )
                        i32.const 1 ;; jumping to 0 goes here
                        br 2 ;; exit
                    )
                    i32.const 2 ;; jumping to 1 goes here
                    br 1 ;; exit
                )
                i32.const 3 ;; jumping to 2 goes here
            )
            ;; jumping to 3 goes here (same as exiting)
        """.trimIndent()
    ) {
        context = emptyContext.withFrameContainingLocals(
            mapOf(
                "$0".toLocalIndex() to 0.toValue()
            )
        )
        validCase(2) // should use the first target: 1

        context = emptyContext.withFrameContainingLocals(
            mapOf(
                "$0".toLocalIndex() to 1.toValue()
            )
        )
        validCase(3) // should use the second target: 2

        context = emptyContext.withFrameContainingLocals(
            mapOf(
                "$0".toLocalIndex() to 2.toValue()
            )
        )
        validCase(99) // should use the third target: 3
    }
}
