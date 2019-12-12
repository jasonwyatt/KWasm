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

package kwasm.format.text

import com.google.common.truth.Truth
import kwasm.format.ParseException
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@UseExperimental(ExperimentalUnsignedTypes::class)
class LimitsTest {

    @Test
    fun parseSingleNumber_setMinToCorrectValue() {
        val expectedMin = 123456.toUInt()
        val expectedMax = UInt.MAX_VALUE
        val limits = Type.Limits("123456")
        Truth.assertThat(limits.value.min).isEqualTo(expectedMin)
        Truth.assertThat(limits.value.max).isEqualTo(expectedMax)
    }

    @Test
    fun parseMaxVal_setMinToMaxVal() {
        val expectedMin = UInt.MAX_VALUE
        val expectedMax = UInt.MAX_VALUE
        val limits = Type.Limits(UInt.MAX_VALUE.toString())
        Truth.assertThat(limits.value.min).isEqualTo(expectedMin)
        Truth.assertThat(limits.value.max).isEqualTo(expectedMax)
    }

    @Test
    fun parseMinVal_setMinToMinVal() {
        val expectedMin = UInt.MIN_VALUE
        val expectedMax = UInt.MAX_VALUE
        val limits = Type.Limits(UInt.MIN_VALUE.toString())
        Truth.assertThat(limits.value.min).isEqualTo(expectedMin)
        Truth.assertThat(limits.value.max).isEqualTo(expectedMax)
    }

    @Test
    fun parseNegativeMin_throwsParseExceptionWithNegativeNumberMessage() {
        val limits = Type.Limits("-123456")
        Assertions.assertThatThrownBy { limits.value.min }
            .isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Illegal char")
    }

    @Test
    fun parseABitLargerThanMaxVal_throwsParseExceptionWithValueOverflowMessage() {
        val limits = Type.Limits("4294967296")
        Assertions.assertThatThrownBy { limits.value.min }
            .isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Illegal value")
    }

    @Test
    fun parseALotLargerThanMaxVal_throwsParseExceptionWithValueOverflowMessage() {
        val limits = Type.Limits("100000000000")
        Assertions.assertThatThrownBy { limits.value.min }
            .isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Illegal value")
    }

    @Test
    fun parseTwoNumbers_setMinAndMaxToCorrectValue() {
        val expectedMin = 123456.toUInt()
        val expectedMax = 234567.toUInt()
        val limits = Type.Limits("123456 234567")
        Truth.assertThat(limits.value.min).isEqualTo(expectedMin)
        Truth.assertThat(limits.value.max).isEqualTo(expectedMax)
    }

    @Test
    fun parseTwoNumbers_withMinGreaterThanMax_throwsParseExceptionWithInvalidRangeMessage() {
        val limits = Type.Limits("234567 123456")
        Assertions.assertThatThrownBy { limits.value }
            .isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Invalid Range specified, min > max.")
    }

    @Test
    fun parseTwoValuesWithNegativeMin_throwsParseExceptionWithNegativeNumberMessage() {
        val limits = Type.Limits("-123456 234567")
        Assertions.assertThatThrownBy { limits.value }
            .isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Illegal char")
    }

    @Test
    fun parseTwoValuesWithMinABitLargerThanMaxVal_throwsParseExceptionWithValueOverflowMessage() {
        val limits = Type.Limits("4294967296 234567")
        Assertions.assertThatThrownBy { limits.value }
            .isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Illegal value")
    }

    @Test
    fun parseTwoValuesWithMinALotLargerThanMaxVal_throwsParseExceptionWithValueOverflowMessage() {
        val limits = Type.Limits("100000000000 234567")
        Assertions.assertThatThrownBy { limits.value }
            .isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Illegal value")
    }

    @Test
    fun parseTwoValuesWithNegativeMax_throwsParseExceptionWithNegativeNumberMessage() {
        val limits = Type.Limits("123456 -234567")
        Assertions.assertThatThrownBy { limits.value }
            .isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Illegal char")
    }

    @Test
    fun parseTwoValuesWithMaxABitLargerThanMaxVal_throwsParseExceptionWithValueOverflowMessage() {
        val limits = Type.Limits("1234567 4294967296")
        Assertions.assertThatThrownBy { limits.value }
            .isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Illegal value")
    }

    @Test
    fun parseTwoValuesWithMaxALotLargerThanMaxVal_throwsParseExceptionWithValueOverflowMessage() {
        val limits = Type.Limits("1234567 100000000000")
        Assertions.assertThatThrownBy { limits.value }
            .isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Illegal value")
    }

    @Test
    fun parseNoValues_throwsParseExceptionWithIncorrectNumberOfArgumentsException() {
        val limits = Type.Limits("")
        Assertions.assertThatThrownBy { limits.value }
            .isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Invalid number of arguments.")
    }

    @Test
    fun parseThreeValues_throwsParseExceptionWithIncorrectNumberOfArgumentsException() {
        val limits = Type.Limits("1234567 2345678 3456789")
        Assertions.assertThatThrownBy { limits.value }
            .isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Invalid number of arguments.")
    }
}