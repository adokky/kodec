package io.kodec

import io.kodec.buffers.asBuffer
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

open class StringToNumberTest {
    private val unusualNumbers = arrayOf(
        ".0" to 0.0,
        "0." to 0.0,
        "000001." to 1.0,
        "0000000.001" to 0.001,
    )

    @Test
    fun floats() {
        for ((input, expected) in unusualNumbers) {
            assertFloatEquals(expected.toFloat(), FloatingDecimalParsing.parseFloat(input))
        }

        for (num in NumbersDataSet.getFloat32()) {
            val decoded = FloatingDecimalParsing.parseFloat(num.toString())
            when {
                num.isNaN() -> assertTrue(decoded.isNaN())
                else -> assertFloatEquals(decoded, num)
            }
        }
    }

    @Test
    fun doubles() {
        for ((input, expected) in unusualNumbers) {
            assertFloatEquals(expected.toFloat(), FloatingDecimalParsing.parseFloat(input))
        }

        for (num in NumbersDataSet.getFloat64()) {
            val decoded = FloatingDecimalParsing.parseDouble(num.toString())
            when {
                num.isNaN() -> assertTrue(decoded.isNaN())
                else -> assertDoubleEquals(decoded, num)
            }
        }
    }

    private val randomStrings = arrayOf("", ".", "99")

    @Test
    fun floats_from_buffer_range() {
        for (num in NumbersDataSet.getFloat32()) {
            val expected = num.toString()
            for (prefix in randomStrings)
            for (suffix in randomStrings) {
                val decoded = FloatingDecimalParsing.readBuffer(
                    "$prefix${expected}$suffix".encodeToByteArray().asBuffer(),
                    start = prefix.length,
                    endExclusive = prefix.length + expected.length
                ).floatValue()
                if (num.isNaN() && decoded.isNaN()) continue
                assertFloatEquals(decoded, num)
            }
        }
    }

    @Test
    fun doubles_from_buffer_range() {
        for (num in NumbersDataSet.getFloat64()) {
            val expected = num.toString()
            for (prefix in randomStrings)
            for (suffix in randomStrings) {
                val decoded = FloatingDecimalParsing.readBuffer(
                    "$prefix${expected}$suffix".encodeToByteArray().asBuffer(),
                    start = prefix.length,
                    endExclusive = prefix.length + expected.length
                ).doubleValue()
                if (num.isNaN() && decoded.isNaN()) continue
                assertDoubleEquals(decoded, num)
            }
        }
    }

    @Test
    fun invalid_format_cases() {
        arrayOf("", " ", ".", "0..", "..0", "0..0", "127.0.0.1", "0e", "0.e", "0.1e", "e").forEach { input ->
            try {
                assertNull(assertFailsWith<NumberFormatException> { FloatingDecimalParsing.parseFloat(input) }.cause)
                assertNull(assertFailsWith<NumberFormatException> { FloatingDecimalParsing.parseDouble(input) }.cause)
                assertNull(assertFailsWith<NumberFormatException> { FloatingDecimalParsing.readString(input) }.cause)
                FloatingDecimalParsing.readString(input, onFormatError = DecodingErrorHandler.Ignore)
            } catch (e: Throwable) {
                fail("failed it '$input'", e)
            }
        }
    }

    private fun assertDoubleEquals(decoded: Double, num: Double, precision: Double = 0.0001) {
        if (decoded !in (num - precision .. num + precision))
            fail("expected=$num\ndecoded=$decoded")
    }

    private fun assertFloatEquals(decoded: Float, num: Float, precision: Float = 0.0001f) {
        if (decoded !in (num - precision .. num + precision))
            fail("expected=$num\ndecoded=$decoded")
    }
}