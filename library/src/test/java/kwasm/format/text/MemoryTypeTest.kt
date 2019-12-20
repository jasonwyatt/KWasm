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

import com.google.common.truth.Truth.assertThat
import kwasm.format.ParseContext
import kwasm.format.ParseException
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@UseExperimental(ExperimentalUnsignedTypes::class)
class MemoryTypeTest {

    private val context = ParseContext("TokenizerTest.wasm", 1, 1)
    private val tokenizer = Tokenizer()

    @Test
    fun parseSingleNumber_setMinToCorrectValue() {
        val expectedMin = 123456.toUInt()
        val expectedMax = UInt.MAX_VALUE
        val tokens = tokenizer.tokenize("$expectedMin", context)
        val parseResult = tokens.parseMemoryType(0)
        assertThat(parseResult.astNode.limits.min).isEqualTo(expectedMin)
        assertThat(parseResult.astNode.limits.max).isEqualTo(expectedMax)
        assertThat(parseResult.parseLength).isEqualTo(1)
    }

    @Test
    fun parseMaxVal_setMinToMaxVal() {
        val expectedMin = UInt.MAX_VALUE
        val expectedMax = UInt.MAX_VALUE
        val tokens = tokenizer.tokenize("$expectedMin", context)
        val parseResult = tokens.parseMemoryType(0)
        assertThat(parseResult.astNode.limits.min).isEqualTo(expectedMin)
        assertThat(parseResult.astNode.limits.max).isEqualTo(expectedMax)
        assertThat(parseResult.parseLength).isEqualTo(1)
    }

    @Test
    fun parseMinVal_setMinToMinVal() {
        val expectedMin = UInt.MIN_VALUE
        val expectedMax = UInt.MAX_VALUE
        val tokens = tokenizer.tokenize("$expectedMin", context)
        val parseResult = tokens.parseMemoryType(0)
        assertThat(parseResult.astNode.limits.min).isEqualTo(expectedMin)
        assertThat(parseResult.astNode.limits.max).isEqualTo(expectedMax)
        assertThat(parseResult.parseLength).isEqualTo(1)
    }

    @Test
    fun parseNegativeMin_throwsParseExceptionWithNegativeNumberMessage() {
        val tokens = tokenizer.tokenize("-123456", context)
        val exception = assertThrows(ParseException::class.java) { tokens.parseMemoryType(0) }
        assertThat(exception).hasMessageThat().contains("Expected integer literal")
    }

    @Test
    fun parseABitLargerThanMaxVal_throwsParseExceptionWithValueOverflowMessage() {
        val tokens = tokenizer.tokenize("4294967296", context)
        val exception = assertThrows(ParseException::class.java) { tokens.parseMemoryType(0) }
        assertThat(exception).hasMessageThat().contains("Illegal value")
    }

    @Test
    fun parseALotLargerThanMaxVal_throwsParseExceptionWithValueOverflowMessage() {
        val tokens = tokenizer.tokenize("100000000000", context)
        val exception = assertThrows(ParseException::class.java) { tokens.parseMemoryType(0) }
        assertThat(exception).hasMessageThat().contains("Illegal value")
    }

    @Test
    fun parseTwoNumbers_setMinAndMaxToCorrectValue() {
        val expectedMin = 123456.toUInt()
        val expectedMax = 234567.toUInt()
        val tokens = tokenizer.tokenize("$expectedMin $expectedMax", context)
        val parseResult = tokens.parseMemoryType(0)
        assertThat(parseResult.astNode.limits.min).isEqualTo(expectedMin)
        assertThat(parseResult.astNode.limits.max).isEqualTo(expectedMax)
        assertThat(parseResult.parseLength).isEqualTo(2)
    }

    @Test
    fun parseTwoNumbers_withMinGreaterThanMax_throwsParseExceptionWithInvalidRangeMessage() {
        val tokens = tokenizer.tokenize("234567 123456", context)
        val exception = assertThrows(ParseException::class.java) { tokens.parseMemoryType(0) }
        assertThat(exception).hasMessageThat().contains("Arguments out of order, min > max.")
    }

    @Test
    fun parseTwoValuesWithNegativeMin_throwsParseExceptionWithNegativeNumberMessage() {
        val tokens = tokenizer.tokenize("-123456 234567", context)
        val exception = assertThrows(ParseException::class.java) { tokens.parseMemoryType(0) }
        assertThat(exception).hasMessageThat().contains("Expected integer literal")
    }

    @Test
    fun parseTwoValuesWithMinABitLargerThanMaxVal_throwsParseExceptionWithValueOverflowMessage() {
        val tokens = tokenizer.tokenize("4294967296 234567", context)
        val exception = assertThrows(ParseException::class.java) { tokens.parseMemoryType(0) }
        assertThat(exception).hasMessageThat().contains("Illegal value")
    }

    @Test
    fun parseTwoValuesWithMinALotLargerThanMaxVal_throwsParseExceptionWithValueOverflowMessage() {
        val tokens = tokenizer.tokenize("100000000000 234567", context)
        val exception = assertThrows(ParseException::class.java) { tokens.parseMemoryType(0) }
        assertThat(exception).hasMessageThat().contains("Illegal value")
    }

    @Test
    fun parseTwoValuesWithMaxABitLargerThanMaxVal_throwsParseExceptionWithValueOverflowMessage() {
        val tokens = tokenizer.tokenize("1234567 4294967296", context)
        val exception = assertThrows(ParseException::class.java) { tokens.parseMemoryType(0) }
        assertThat(exception).hasMessageThat().contains("Illegal value")
    }

    @Test
    fun parseTwoValuesWithMaxALotLargerThanMaxVal_throwsParseExceptionWithValueOverflowMessage() {
        val tokens = tokenizer.tokenize("1234567 100000000000", context)
        val exception = assertThrows(ParseException::class.java) { tokens.parseMemoryType(0) }
        assertThat(exception).hasMessageThat().contains("Illegal value")
    }

    @Test
    fun parseNoValues_throwsParseExceptionWithIncorrectNumberOfArgumentsException() {
        val tokens = tokenizer.tokenize("memory", context)
        val exception = assertThrows(ParseException::class.java) { tokens.parseMemoryType(0) }
        assertThat(exception).hasMessageThat().contains("Expected integer literal")
    }
}