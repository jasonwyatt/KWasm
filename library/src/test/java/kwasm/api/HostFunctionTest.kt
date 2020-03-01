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

package kwasm.api

import com.google.common.truth.Truth.assertThat
import kwasm.ast.Identifier
import kwasm.ast.type.FunctionType
import kwasm.ast.type.Param
import kwasm.ast.type.Result
import kwasm.ast.type.ValueType
import kwasm.runtime.DoubleValue
import kwasm.runtime.EmptyValue
import kwasm.runtime.FloatValue
import kwasm.runtime.IntValue
import kwasm.runtime.Memory
import kwasm.runtime.Value
import kwasm.runtime.memory.ByteBufferMemory
import kwasm.runtime.toValue
import org.junit.Assert.assertThrows
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class HostFunctionTest {
    private lateinit var context: HostFunctionContext

    @Before
    fun setUp() {
        context = Context(ByteBufferMemory())
    }

    @Test
    fun unitHostFunction_noParams() {
        var called = false
        val function = UnitHostFunction {
            assertThat(it).isSameInstanceAs(this@HostFunctionTest.context)
            called = true
        }

        assertThat(function(emptyList(), context)).isEqualTo(EmptyValue)
        assertThat(called).isTrue()

        assertThat(function.functionType).isEqualTo(
            FunctionType(emptyList(), emptyList())
        )
    }

    @Test
    fun unitHostFunction_oneParam() {
        val argumentValues = mutableListOf<Value<*>>()
        val function = UnitHostFunction { a: IntValue, context ->
            assertThat(context).isSameInstanceAs(this@HostFunctionTest.context)
            argumentValues.add(a)
        }

        assertThat(function(listOf(42.toValue()), context)).isEqualTo(EmptyValue)
        assertThat(argumentValues).containsExactly(42.toValue())

        assertThat(function.functionType).isEqualTo(
            FunctionType(
                listOf(Param(Identifier.Local(null, 0), ValueType.I32)),
                emptyList()
            )
        )
    }

    @Test
    fun unitHostFunction_oneParam_throwsOnBadValue() {
        val function = UnitHostFunction { _: IntValue, _ ->
            fail("Should not be called")
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 1 is null or is not of expected type: ${IntValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42L.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 1 is null or is not of expected type: ${IntValue::class}"
            )
        }
    }

    @Test
    fun unitHostFunction_twoParams() {
        val argumentValues = mutableListOf<Value<*>>()
        val function = UnitHostFunction { a: IntValue, b: FloatValue, context ->
            assertThat(context).isSameInstanceAs(this@HostFunctionTest.context)
            argumentValues.add(a)
            argumentValues.add(b)
        }

        assertThat(
            function(listOf(42.toValue(), 1337.0f.toValue()), context)
        ).isEqualTo(EmptyValue)
        assertThat(argumentValues).containsExactly(42.toValue(), 1337.0f.toValue())

        assertThat(function.functionType).isEqualTo(
            FunctionType(
                listOf(
                    Param(Identifier.Local(null, 0), ValueType.I32),
                    Param(Identifier.Local(null, 1), ValueType.F32)
                ),
                emptyList()
            )
        )
    }

    @Test
    fun unitHostFunction_twoParams_throwsOnBadValue() {
        val function = UnitHostFunction { _: IntValue, _: FloatValue, _ ->
            fail("Should not be called")
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 1 is null or is not of expected type: ${IntValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42L.toValue(), 1337f.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 1 is null or is not of expected type: ${IntValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 2 is null or is not of expected type: ${FloatValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42.toValue(), 1337.0.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 2 is null or is not of expected type: ${FloatValue::class}"
            )
        }
    }

    @Test
    fun unitHostFunction_threeParams() {
        val argumentValues = mutableListOf<Value<*>>()
        val function = UnitHostFunction { a: IntValue, b: FloatValue, c: DoubleValue, context ->
            assertThat(context).isSameInstanceAs(this@HostFunctionTest.context)
            argumentValues.add(a)
            argumentValues.add(b)
            argumentValues.add(c)
        }

        assertThat(
            function(listOf(42.toValue(), 1337.0f.toValue(), 80081335.0.toValue()), context)
        ).isEqualTo(EmptyValue)
        assertThat(argumentValues)
            .containsExactly(42.toValue(), 1337.0f.toValue(), 80081335.0.toValue())

        assertThat(function.functionType).isEqualTo(
            FunctionType(
                listOf(
                    Param(Identifier.Local(null, 0), ValueType.I32),
                    Param(Identifier.Local(null, 1), ValueType.F32),
                    Param(Identifier.Local(null, 1), ValueType.F64)
                ),
                emptyList()
            )
        )
    }

    @Test
    fun unitHostFunction_threeParams_throwsOnBadValue() {
        val function = UnitHostFunction { _: IntValue, _: FloatValue, _: DoubleValue, _ ->
            fail("Should not be called")
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 1 is null or is not of expected type: ${IntValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42L.toValue(), 1337f.toValue(), 80081335.0.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 1 is null or is not of expected type: ${IntValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 2 is null or is not of expected type: ${FloatValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42.toValue(), 1337.0.toValue(), 80081335.0.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 2 is null or is not of expected type: ${FloatValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42.toValue(), 1337.0f.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 3 is null or is not of expected type: ${DoubleValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42.toValue(), 1337.0f.toValue(), 1.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 3 is null or is not of expected type: ${DoubleValue::class}"
            )
        }
    }

    @Test
    fun hostFunction_noParams() {
        val function = HostFunction { context: HostFunctionContext ->
            assertThat(context).isSameInstanceAs(this@HostFunctionTest.context)
            return@HostFunction 35.toValue()
        }

        assertThat(function(emptyList(), context)).isEqualTo(35.toValue())

        assertThat(function.functionType).isEqualTo(
            FunctionType(
                emptyList(),
                listOf(Result(ValueType.I32))
            )
        )
    }

    @Test
    fun hostFunction_oneParam() {
        val function = HostFunction { a: IntValue, context ->
            assertThat(context).isSameInstanceAs(this@HostFunctionTest.context)
            (a.value * 3).toValue()
        }

        assertThat(function(listOf(42.toValue()), context)).isEqualTo((42 * 3).toValue())

        assertThat(function.functionType).isEqualTo(
            FunctionType(
                listOf(Param(Identifier.Local(null, 0), ValueType.I32)),
                listOf(Result(ValueType.I32))
            )
        )
    }

    @Test
    fun hostFunction_oneParam_throwsOnBadValue() {
        val function = HostFunction { _: IntValue, _ ->
            fail("Should not be called")
            3.toValue()
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 1 is null or is not of expected type: ${IntValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42L.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 1 is null or is not of expected type: ${IntValue::class}"
            )
        }
    }

    @Test
    fun hostFunction_twoParams() {
        val function = HostFunction { a: IntValue, b: FloatValue, context ->
            assertThat(context).isSameInstanceAs(this@HostFunctionTest.context)
            (a.value * b.value).toValue()
        }

        assertThat(
            function(listOf(42.toValue(), 1337.0f.toValue()), context)
        ).isEqualTo((42 * 1337.0f).toValue())

        assertThat(function.functionType).isEqualTo(
            FunctionType(
                listOf(
                    Param(Identifier.Local(null, 0), ValueType.I32),
                    Param(Identifier.Local(null, 1), ValueType.F32)
                ),
                listOf(
                    Result(ValueType.F32)
                )
            )
        )
    }

    @Test
    fun hostFunction_twoParams_throwsOnBadValue() {
        val function = HostFunction { _: IntValue, _: FloatValue, _ ->
            fail("Should not be called")
            3L.toValue()
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 1 is null or is not of expected type: ${IntValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42L.toValue(), 1337f.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 1 is null or is not of expected type: ${IntValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 2 is null or is not of expected type: ${FloatValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42.toValue(), 1337.0.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 2 is null or is not of expected type: ${FloatValue::class}"
            )
        }
    }

    @Test
    fun hostFunction_threeParams() {
        val function = HostFunction { a: IntValue, b: FloatValue, c: DoubleValue, context ->
            assertThat(context).isSameInstanceAs(context)
            (a.value * b.value / c.value).toValue()
        }

        assertThat(
            function(listOf(42.toValue(), 1337.0f.toValue(), 80081335.0.toValue()), context)
        ).isEqualTo((42 * 1337.0f / 80081335.0).toValue())

        assertThat(function.functionType).isEqualTo(
            FunctionType(
                listOf(
                    Param(Identifier.Local(null, 0), ValueType.I32),
                    Param(Identifier.Local(null, 1), ValueType.F32),
                    Param(Identifier.Local(null, 1), ValueType.F64)
                ),
                listOf(
                    Result(ValueType.F64)
                )
            )
        )
    }

    @Test
    fun hostFunction_threeParams_throwsOnBadValue() {
        val function = HostFunction { _: IntValue, _: FloatValue, _: DoubleValue, _ ->
            fail("Should not be called")
            1L.toValue()
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 1 is null or is not of expected type: ${IntValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42L.toValue(), 1337f.toValue(), 80081335.0.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 1 is null or is not of expected type: ${IntValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 2 is null or is not of expected type: ${FloatValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42.toValue(), 1337.0.toValue(), 80081335.0.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 2 is null or is not of expected type: ${FloatValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42.toValue(), 1337.0f.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 3 is null or is not of expected type: ${DoubleValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42.toValue(), 1337.0f.toValue(), 1.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 3 is null or is not of expected type: ${DoubleValue::class}"
            )
        }
    }

    private class Context(override val memory: Memory?) : HostFunctionContext
}
