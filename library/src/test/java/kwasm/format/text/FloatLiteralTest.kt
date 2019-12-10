package kwasm.format.text

import com.google.common.truth.Truth.assertThat
import kwasm.format.ParseException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.pow

@RunWith(JUnit4::class)
class FloatLiteralTest {
    @Test
    fun parsesInf() {
        val actual = FloatLiteral("inf")
        assertThat(actual.isInfinite()).isTrue()
    }

    @Test
    fun parsesNaN() {
        val actual = FloatLiteral("nan")
        assertThat(actual.isNaN()).isTrue()
    }

    @Test
    fun parsesHexNaN() {
        val actual = FloatLiteral("nan:0xF")
        assertThat(actual.isNaN()).isTrue()
    }

    @Test
    fun parsesDecimalInts() {
        var actual = FloatLiteral("10")
        assertThat(actual.value).isWithin(TOLERANCE).of(10.0)

        actual = FloatLiteral("0")
        assertThat(actual.value).isWithin(TOLERANCE).of(0.0)

        actual = FloatLiteral("1000000000")
        assertThat(actual.value).isWithin(TOLERANCE).of(1000000000.0)
    }

    @Test
    fun parsesHexInts() {
        var actual = FloatLiteral("0x10")
        assertThat(actual.value).isWithin(TOLERANCE).of(16.0)

        actual = FloatLiteral("0x0")
        assertThat(actual.value).isWithin(TOLERANCE).of(0.0)

        actual = FloatLiteral("0xFFFFFFFF")
        assertThat(actual.value).isWithin(TOLERANCE).of(0xFFFFFFFFL.toDouble())
    }

    @Test
    fun parsesDecimal_withFraction() {
        var actual = FloatLiteral("1.")
        assertThat(actual.value).isWithin(TOLERANCE).of(1.0)

        actual = FloatLiteral("1.5")
        assertThat(actual.value).isWithin(TOLERANCE).of(1.5)

        actual = FloatLiteral("0.123456789")
        assertThat(actual.value).isWithin(TOLERANCE).of(0.123456789)

        actual = FloatLiteral("123456789.123456789")
        assertThat(actual.value).isWithin(TOLERANCE).of(123456789.123456789)
    }

    @Test
    fun parsesHex_withFraction() {
        var actual = FloatLiteral("0xF.")
        assertThat(actual.value).isWithin(TOLERANCE).of(15.0)

        actual = FloatLiteral("0x10.a")
        assertThat(actual.value).isWithin(TOLERANCE).of(16.0 + 10/16.0)
    }

    @Test
    fun parsesDecimal_withExponent() {
        var actual = FloatLiteral("1e10")
        assertThat(actual.value).isWithin(TOLERANCE).of(1e10)

        actual = FloatLiteral("3e-5")
        assertThat(actual.value).isWithin(TOLERANCE).of(3e-5)
    }

    @Test
    fun parsesHex_withExponent() {
        var actual = FloatLiteral("0xFp2")
        assertThat(actual.value).isWithin(TOLERANCE).of(15 * 2.0.pow(2))

        actual = FloatLiteral("0xABp-2")
        assertThat(actual.value).isWithin(TOLERANCE).of(0xAB * 2.0.pow(-2))
    }

    @Test
    fun parsesDecimal_withFraction_andExponent() {
        var actual = FloatLiteral("1.e10")
        assertThat(actual.value).isWithin(TOLERANCE).of(1e10)

        actual = FloatLiteral("1.5e10")
        assertThat(actual.value).isWithin(TOLERANCE).of(1.5e10)

        actual = FloatLiteral("3.12345e-4")
        assertThat(actual.value).isWithin(TOLERANCE).of(3.12345e-4)
    }

    @Test
    fun parsesHex_withFraction_andExponent() {
        var actual = FloatLiteral("0xF.p2")
        assertThat(actual.value).isWithin(TOLERANCE).of(15 * 2.0.pow(2))

        actual = FloatLiteral("0xF.ap2")
        assertThat(actual.value).isWithin(TOLERANCE)
            .of((15 + 10/16.0) * 2.0.pow(2))

        actual = FloatLiteral("0xAB.1p-2")
        assertThat(actual.value).isWithin(TOLERANCE)
            .of((0xAB + 1/16.0) * 2.0.pow(-2))
    }

    @Test
    fun decimalThrows_whenHexEncountered() {
        // Case 1.
        var actual = FloatLiteral("a")
        assertThatThrownBy { actual.value }
            .isInstanceOf(ParseException::class.java)

        // Case 2.
        actual = FloatLiteral("a.1")
        assertThatThrownBy { actual.value }
            .isInstanceOf(ParseException::class.java)

        // Case 2.
        actual = FloatLiteral("1.a")
        assertThatThrownBy { actual.value }
            .isInstanceOf(ParseException::class.java)

        // Case 3.
        actual = FloatLiteral("aE1")
        assertThatThrownBy { actual.value }
            .isInstanceOf(ParseException::class.java)

        // Case 3.
        actual = FloatLiteral("1ea")
        assertThatThrownBy { actual.value }
            .isInstanceOf(ParseException::class.java)

        // Case 4.
        actual = FloatLiteral("5.1eA")
        assertThatThrownBy { actual.value }
            .isInstanceOf(ParseException::class.java)

        // Case 4.
        actual = FloatLiteral("5.Ae1")
        assertThatThrownBy { actual.value }
            .isInstanceOf(ParseException::class.java)

        // Case 4.
        actual = FloatLiteral("A.5e1")
        assertThatThrownBy { actual.value }
            .isInstanceOf(ParseException::class.java)
    }

    @Test
    fun decimalThrows_whenDotIsFirstElement() {
        val actual = FloatLiteral(".5")
        assertThatThrownBy { actual.value }
            .isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Invalid placement for decimal")
    }

    @Test
    fun hexThrows_whenDotIsFirstElement() {
        val actual = FloatLiteral("0x.5")
        assertThatThrownBy { actual.value }
            .isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Invalid placement for decimal")
    }

    @Test
    fun decimalThrows_whenExponentIsEmpty() {
        val actual = FloatLiteral("0.5e")
        assertThatThrownBy { actual.value }
            .isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Invalid exponent")
    }

    @Test
    fun hexThrows_whenExponentIsEmpty() {
        val actual = FloatLiteral("0x0.Ap")
        assertThatThrownBy { actual.value }
            .isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Invalid exponent")
    }

    companion object {
        private const val TOLERANCE = 1e-10
    }
}
