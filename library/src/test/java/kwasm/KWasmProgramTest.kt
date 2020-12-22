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

package kwasm

import com.google.common.truth.Truth.assertThat
import kwasm.api.ByteBufferMemoryProvider
import kwasm.api.HostFunction
import kwasm.format.ParseException
import kwasm.runtime.EmptyValue
import kwasm.runtime.IntValue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File

@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
@RunWith(JUnit4::class)
class KWasmProgramTest {
    @Test
    fun build_singleModule_importNotFound() {
        val memoryProvider = ByteBufferMemoryProvider(1024 * 1024)
        val programBuilder = KWasmProgram.Builder(memoryProvider).apply {
            withModule(
                name = "testModule",
                source = """(module (import "other" "module" (memory 1 1)))"""
            )
        }

        try {
            programBuilder.build()
            fail()
        } catch (e: KWasmProgram.ImportNotFoundException) {
            assertThat(e).hasMessageThat().contains("other")
            assertThat(e).hasMessageThat().contains("module")
        }
    }

    @Test
    fun build_importMismatch() {
        val memoryProvider = ByteBufferMemoryProvider(1024 * 1024)
        val programBuilder = KWasmProgram.Builder(memoryProvider).apply {
            withModule(
                name = "other",
                source = """
                    (module
                        (func $0)
                        (export "memory" (func $0)))
                """.trimIndent()
            )

            withModule(
                name = "testModule",
                source = """(module (import "other" "memory" (memory 1 1)))"""
            )
        }

        try {
            programBuilder.build()
            fail()
        } catch (e: KWasmProgram.ImportMismatchException) {
            assertThat(e).hasMessageThat()
                .contains("Import for testModule found, but was wrong type: Function(value=0)")
        }
    }

    @Test
    fun build_importFunction() {
        val memoryProvider = ByteBufferMemoryProvider(1024 * 1024)
        var hostFunctionCalled = false
        KWasmProgram.Builder(memoryProvider).apply {
            withModule(
                name = "testModule",
                source = """
                    (module
                        (import "host" "fn" (func ${"$"}fn))
                        (func ${"$"}starter
                            (call ${"$"}fn))
                        (start ${"$"}starter))
                """.trimIndent()
            )

            withHostFunction(
                "host",
                "fn",
                HostFunction { _ ->
                    hostFunctionCalled = true
                    EmptyValue
                }
            )
        }.build()

        assertThat(hostFunctionCalled).isTrue()
    }

    @Test
    fun build_importTable() {
        val memoryProvider = ByteBufferMemoryProvider(1024 * 1024)
        var hostFunctionCalled = false
        KWasmProgram.Builder(memoryProvider).apply {
            withModule(
                name = "provider",
                source = """
                    |(module
                    |    (import "host" "fn" (func ${"$"}fn))
                    |    (type (func))
                    |    (func $1
                    |        (call ${"$"}fn))
                    |    (table 1 5 funcref)
                    |    (elem 0 (offset (i32.const 0)) $1)
                    |    (export "table" (table 0)))
                """.trimMargin("|")
            )

            withModule(
                name = "testModule",
                source = """
                    (module
                        (import "provider" "table" (table $0 1 1 funcref))
                        (type (func))
                        (func ${"$"}starter
                            (call_indirect (i32.const 0)))
                        (start ${"$"}starter))
                """.trimIndent()
            )

            withHostFunction(
                namespace = "host",
                name = "fn",
                hostFunction = HostFunction { _ ->
                    hostFunctionCalled = true
                    EmptyValue
                }
            )
        }.build()

        assertThat(hostFunctionCalled).isTrue()
    }

    @Test
    fun build_importMemory() {
        val memoryProvider = ByteBufferMemoryProvider(1024 * 1024)
        var hostFunctionResult: Int? = null

        val program = KWasmProgram.builder(memoryProvider).apply {
            withModule(
                name = "memProvider",
                source = """(module (memory (export "mem") 1 1))"""
            )

            withModule(
                name = "testModule",
                source = """
                    (module
                        (import "memProvider" "mem" (memory 1 1))
                        (import "host" "fn" (func $1 (param i32)))
                        (data (offset i32.const 0) "hello world!")
                        (func $2
                            (i32.store (i32.const 12) (i32.const 42))
                            (call $1 (i32.load (i32.const 12)))
                        )
                        (start $2)  
                    )
                """.trimIndent()
            )

            withHostFunction(
                namespace = "host",
                name = "fn",
                hostFunction = HostFunction { p1: IntValue, _ ->
                    hostFunctionResult = p1.value
                    EmptyValue
                }
            )
        }.build()

        assertThat(hostFunctionResult).isEqualTo(42)
        val bytes = ByteArray(12)
        program.memory.readBytes(bytes, 0)
        assertThat(bytes.toString(Charsets.UTF_8)).isEqualTo("hello world!")
    }

    @Test
    fun build_importGlobal() {
        val memoryProvider = ByteBufferMemoryProvider(1024 * 1024)
        var hostFunctionResult: Int? = null

        KWasmProgram.builder(memoryProvider).apply {
            withModule(
                name = "globalProvider",
                source = """
                    (module 
                        (global (mut i32) (i32.const 0))
                        (export "flag" (global 0)))
                """.trimIndent()
            )

            withModule(
                name = "testModule",
                source = """
                    (module
                        (import "globalProvider" "flag" (global $1 (mut i32)))
                        (import "host" "fn" (func $1 (param i32)))
                        (func $2
                            (global.set $1 (i32.const 42))
                            (call $1 (global.get $1))
                        )
                        (start $2)  
                    )
                """.trimIndent()
            )

            withHostFunction(
                namespace = "host",
                name = "fn",
                hostFunction = HostFunction { p1: IntValue, _ ->
                    hostFunctionResult = p1.value
                    EmptyValue
                }
            )
        }.build()

        assertThat(hostFunctionResult).isEqualTo(42)
    }

    @Test
    fun withModule_failsIfModuleCantParse() {
        val memoryProvider = ByteBufferMemoryProvider(1024 * 1024)
        val builder = KWasmProgram.Builder(memoryProvider)
        try {
            builder.withModule("myModule", "(not a module)")
        } catch (e: ParseException) {
            assertThat(e).hasMessageThat().contains("No module found in source for myModule")
        }
    }

    @Test
    fun withTextFormatModule_fromFile() {
        val testFileUrl = javaClass.classLoader.getResource("kwasm/helloworld.wat")

        val memoryProvider = ByteBufferMemoryProvider(1024 * 1024)
        var receivedString: String? = null

        val builder = KWasmProgram.Builder(memoryProvider)
        builder.withTextFormatModule("blah", File(testFileUrl.file))
        builder.withHostFunction(
            namespace = "System",
            name = "println",
            hostFunction = HostFunction { offset: IntValue, length: IntValue, context ->
                val bytes = ByteArray(length.value)
                context.memory?.readBytes(bytes, offset.value)
                receivedString = bytes.toString(Charsets.UTF_8)
                EmptyValue
            }
        )
        builder.build()

        assertThat(receivedString).isEqualTo("Hello world")
    }

    @Test
    fun withTextFormatModule_fromStream() {
        val testFileUrl = javaClass.classLoader.getResource("kwasm/helloworld.wat")

        val memoryProvider = ByteBufferMemoryProvider(1024 * 1024)
        var receivedString: String? = null

        val builder = KWasmProgram.Builder(memoryProvider)
        builder.withTextFormatModule("blah", testFileUrl.openStream())
        builder.withHostFunction(
            namespace = "System",
            name = "println",
            hostFunction = HostFunction { offset: IntValue, length: IntValue, context ->
                val bytes = ByteArray(length.value)
                context.memory?.readBytes(bytes, offset.value)
                receivedString = bytes.toString(Charsets.UTF_8)
                EmptyValue
            }
        )
        builder.build()

        assertThat(receivedString).isEqualTo("Hello world")
    }
}
