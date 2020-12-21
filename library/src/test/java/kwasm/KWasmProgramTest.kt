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
import kwasm.runtime.EmptyValue
import kwasm.runtime.IntValue
import kwasm.runtime.toValue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class KWasmProgramTest {
    @Test
    fun build_singleModule() {
        val memoryProvider = ByteBufferMemoryProvider(1024 * 1024)
        val program = KWasmProgram.builder(memoryProvider).apply {
            withModule(
                name = "MyModule",
                source = """
                    (module
                        (memory $1 1 1)
                        (export "mem" (memory $1))
                        (import "io" "println" (func ${'$'}println (param i32) (param i32)))
                        (import "util" "currentTimeMillis" 
                            (func ${'$'}currentTimeMillis (result i64))
                        )
                        (data (offset (i32.const 0)) "test")
                        (data (offset (i32.const 8)) "Hello world!")
                        (func ${"\$fib"} (param ${"\$n"} i32) (result i32) (local $0 i32)
                            (if (i32.lt_s
                                    (local.get  ${"\$n"})
                                    (i32.const 2)
                                )
                                (then (local.set $0 (i32.const 1)))
                                (else 
                                    (local.set $0
                                        (i32.add
                                            (call ${"\$fib"}
                                                (i32.sub
                                                    (local.get ${"\$n"})
                                                    (i32.const 2)
                                                )
                                            )
                                            (call ${"\$fib"}
                                                (i32.sub
                                                    (local.get  ${"\$n"})
                                                    (i32.const 1)
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                            (local.get $0)
                        )
                        (func ${"$"}starter 
                                    (local ${"\$startTime"} i64) 
                                    (local ${"\$fibResult"} i32)
                            (i32.store16 (i32.const 4) (i32.const 1337))
                            (call ${"$"}println (i32.const 0) (i32.const 4))
                            (call ${"$"}println (i32.const 8) (i32.const 12))
                            (local.set ${"\$startTime"} (call ${"\$currentTimeMillis"}))
                            (local.set ${"\$fibResult"} (call ${"\$fib"} (i32.const 20)))
                            (i64.store 
                                (i32.const 20) 
                                (i64.sub
                                    (call ${"\$currentTimeMillis"})
                                    (local.get ${"\$startTime"})
                                )
                            )
                            (i32.store (i32.const 28) (local.get ${"\$fibResult"}))
                        )
                        (start ${"$"}starter)
                    )
                """.trimIndent()
            )

            withHostFunction(
                namespace = "io",
                name = "println",
                hostFunction = HostFunction { offset: IntValue, length: IntValue, context ->
                    val strBytes = ByteArray(length.value)
                    requireNotNull(context.memory).readBytes(strBytes, offset.value)
                    println(strBytes.toString(Charsets.UTF_8))
                    EmptyValue
                }
            )

            withHostFunction(
                namespace = "util",
                name = "currentTimeMillis",
                hostFunction = HostFunction { context -> System.currentTimeMillis().toValue() }
            )
        }.build()

        val stringBytes = ByteArray(4)
        assertThat(program.memory.readBytes(stringBytes, memoryOffset = 0))
            .isEqualTo(stringBytes.size)
        assertThat(stringBytes.toString(Charsets.UTF_8)).isEqualTo("test")
        assertThat(program.memory.readInt(4)).isEqualTo(1337)
    }
}
