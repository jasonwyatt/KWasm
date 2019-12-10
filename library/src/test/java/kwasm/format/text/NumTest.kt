package kwasm.format.text

import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import kwasm.format.ParseException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.Executors.callable

@RunWith(JUnit4::class)
@UseExperimental(ExperimentalUnsignedTypes::class)
class NumTest {
    @Test
    fun parsesSingleDigits_base10() {
        repeat(10) { digit ->
            val num = Num("$digit")
            assertThat(num.value).isEqualTo(digit.toULong())
            assertThat(num.foundHexChars).isFalse()
        }
    }

    @Test
    fun parsesSingleDigits_base16_lowercase() {
        val chars = "abcdef"
        val values = listOf(10, 11, 12, 13, 14, 15).map { it.toULong() }
        chars.forEachIndexed { index, value ->
            val num = Num("$value")
            assertThat(num.value).isEqualTo(values[index])
            assertThat(num.foundHexChars).isTrue()
        }
    }

    @Test
    fun parsesSingleDigits_base16_uppercase() {
        val chars = "ABCDEF"
        val values = listOf(10, 11, 12, 13, 14, 15).map { it.toULong() }
        chars.forEachIndexed { index, value ->
            val num = Num("$value")
            assertThat(num.value).isEqualTo(values[index])
            assertThat(num.foundHexChars).isTrue()
        }
    }

    @Test
    fun parsesBase10() {
        val expected = 1234567890.toULong()
        val num = Num("$expected")
        assertThat(num.value).isEqualTo(expected)
    }

    @Test
    fun parsesBase10_withUnderscores() {
        val expected = 1234567890.toULong()
        var num = Num("${expected}_")
        assertThat(num.value).isEqualTo(expected)

        num = Num("_$expected")
        assertThat(num.value).isEqualTo(expected)

        num = Num("_1_2_3_4_5_6_7_8_9_0_")
        assertThat(num.value).isEqualTo(expected)
    }

    @Test
    fun parsesBase16() {
        val sequence = "1a2b3c4d"
        val expected = sequence.toULong(16)
        val num = Num(sequence)
        assertThat(num.value).isEqualTo(expected)
    }

    @Test
    fun parsesBase16_withUnderScores() {
        val sequence = "1a2b3c4d"
        val expected = sequence.toULong(16)

        var num = Num(sequence)
        assertThat(num.value).isEqualTo(expected)

        num = Num("${sequence}_")
        assertThat(num.value).isEqualTo(expected)

        num = Num("_$sequence")
        assertThat(num.value).isEqualTo(expected)

        num = Num("_1_a_2_b_3_c_4_d_")
        assertThat(num.value).isEqualTo(expected)
    }

    @Test
    fun parsesBase16_whenForcingHex() {
        val sequence = "123456789"
        val expected = sequence.toULong(16)
        val num = Num(sequence)
        assertThat(num.foundHexChars).isFalse()
        num.forceHex = true
        assertThat(num.value).isEqualTo(expected)
    }

    @Test
    fun throwsOnEmptySequence() {
        val num = Num("")
        assertThatThrownBy { num.value }
            .isInstanceOf(ParseException::class.java)
    }

    @Test
    fun throwsOnIllegalChar() {
        val sequence = "10z00"
        val num = Num(sequence)
        assertThatThrownBy { num.value }
            .isInstanceOf(ParseException::class.java)
    }
}
