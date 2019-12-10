package kwasm.format.text

import com.google.common.truth.Truth.assertThat
import kwasm.format.ParseException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.absoluteValue

@RunWith(JUnit4::class)
@UseExperimental(ExperimentalUnsignedTypes::class)
class IntegerLiteralTest {
    @Test
    fun parsesUnsigned_base10() {
        val expected = 1234567890.toULong()
        val actual = IntegerLiteral.Unsigned("1234567890")
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun throwsOnUnsigned_whenBase10_containsHexChars() {
        val literal = IntegerLiteral.Unsigned("1234aa")
        assertThatThrownBy { literal.value }
            .isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Unexpected hex integer")
    }

    @Test
    fun parsesUnsigned_base16() {
        val expected = "1234567".toULong(16)
        val actual = IntegerLiteral.Unsigned("0x1234567")
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun throwsOnUnsigned_outsideMagnitude() {
        assertThatThrownBy {
            IntegerLiteral.Unsigned("256", 8).value
        }.isInstanceOf(ParseException::class.java).hasMessageContaining("Illegal value")
    }

    @Test
    fun parsesUnsigned_withinMagnitude() {
        var actual = IntegerLiteral.Unsigned("255", 8)
        assertThat(actual.value).isEqualTo(255.toULong())

        actual = IntegerLiteral.Unsigned("3", 2)
        assertThat(actual.value).isEqualTo(3.toULong())
    }

    @Test
    fun parsesSigned_base10_noExplicitSign() {
        val expected = "123456789".toLong()
        val actual = IntegerLiteral.Signed("$expected")
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun parsesSigned_base10_explicitPositive() {
        val expected = "123456789".toLong()
        val actual = IntegerLiteral.Signed("+$expected")
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun parsesSigned_base10_explicitNegative() {
        val expected = "-123456789".toLong()
        val actual = IntegerLiteral.Signed("$expected")
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun throwsOnSigned_whenBase10_containsHexChars() {
        val literal = IntegerLiteral.Signed("-1234aa")
        assertThatThrownBy { literal.value }
            .isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Unexpected hex integer")
    }

    @Test
    fun parsesSigned_base16_noExplicitSign() {
        val expected = "1234567".toLong(16)
        val actual = IntegerLiteral.Signed("0x${expected.toString(16)}")
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun parsesSigned_base16_explicitPositive() {
        val expected = "1234567".toLong(16)
        val actual = IntegerLiteral.Signed("+0x${expected.toString(16)}")
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun parsesSigned_base16_explicitNegative() {
        val expected = -1 * "1234567".toLong(16)
        val actual = IntegerLiteral.Signed("-0x${expected.absoluteValue.toString(16)}")
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun parsesSigned_withinMagnitude() {
        var actual = IntegerLiteral.Signed("+100", 8)
        assertThat(actual.value).isEqualTo(100L)

        actual = IntegerLiteral.Signed("+127", 8)
        assertThat(actual.value).isEqualTo(127L)

        actual = IntegerLiteral.Signed("-128", 8)
        assertThat(actual.value).isEqualTo(-128L)
    }

    @Test
    fun throwsOnSigned_whenOutsideMagnitude() {
        assertThatThrownBy {
            IntegerLiteral.Signed("-9", 4).value
        }.isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Illegal value")

        assertThatThrownBy {
            IntegerLiteral.Signed("8", 4).value
        }.isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Illegal value")
    }
}
