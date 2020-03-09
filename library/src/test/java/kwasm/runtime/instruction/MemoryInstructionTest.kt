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

import kwasm.KWasmRuntimeException
import kwasm.ParseRule
import kwasm.api.ByteBufferMemoryProvider
import kwasm.ast.Identifier
import kwasm.ast.module.Index
import kwasm.ast.module.WasmModule
import kwasm.runtime.ExecutionContext
import kwasm.runtime.Memory
import kwasm.runtime.allocate
import kwasm.runtime.stack.Activation
import kwasm.runtime.stack.ActivationStack
import kwasm.runtime.stack.RuntimeStacks
import kwasm.validation.module.validate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("UNCHECKED_CAST")
@RunWith(JUnit4::class)
class MemoryInstructionTest {
    @get:Rule
    val parser = ParseRule()

    @Test
    fun i32Store() = instructionCases(parser, "i32.store") {
        context = buildMemoryContext(minPages = 0, maxPages = 1)
        errorCase(KWasmRuntimeException::class, "Cannot store at position 0", 0, 1)

        context = buildMemoryContext(minPages = 1, maxPages = 1)
        errorCase(
            KWasmRuntimeException::class,
            "Cannot store at position ${Memory.PAGE_SIZE}",
            Memory.PAGE_SIZE, 1
        )

        context = buildMemoryContext(minPages = 1, maxPages = 1)
        validVoidCase(0, 1)
        println("test")
    }

    @Test
    fun size_returnsSize() = instructionCases(parser, "memory.size") {
        context = buildMemoryContext(minPages = 0)
        validCase(0)
        context = buildMemoryContext(minPages = 1)
        validCase(1)
        context = buildMemoryContext(minPages = 20, maxPages = 20)
        validCase(20)
    }

    @Test
    fun grow_returnsError_ifCouldntGrow() = instructionCases(parser, "memory.grow") {
        context = buildMemoryContext(maxPages = 1)
        validCase(-1, 2)
        context = buildMemoryContext(maxPages = 1)
        validCase(-1, 20)
    }

    @Test
    fun grow_returnsOldSize_onSuccess() = instructionCases(parser, "memory.grow") {
        context = buildMemoryContext()
        validCase(0, 1)
        validCase(1, 1)
        validCase(2, 1)
        validCase(3, 3)
        validCase(6, 0)
    }

    private fun buildMemoryContext(maxPages: Int = 10, minPages: Int = 0): ExecutionContext {
        var module: WasmModule? = null

        parser.with {
            // TODO: make memory command work without an id.
            module = """
                (module
                    (memory ${'$'}foo $minPages $maxPages)
                )
            """.parseModule()
        }

        val validationContext = module!!.validate()
        val moduleAlloc = module!!.allocate(
            validationContext,
            ByteBufferMemoryProvider(4 * 1024 * 1024)
        )

        return ExecutionContext(
            moduleAlloc.store,
            moduleAlloc.moduleInstance,
            RuntimeStacks(
                activations = ActivationStack().apply {
                    push(
                        Activation(
                            Index.ByInt(0) as Index<Identifier.Function>,
                            emptyMap(),
                            moduleAlloc.moduleInstance
                        )
                    )
                }
            )
        )
    }

}
