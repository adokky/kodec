package io.kodec

import io.kodec.buffers.asArrayBuffer
import kotlin.test.*

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

        for (num in NumbersDataSet.floats32) {
            val decoded = FloatingDecimalParsing.parseFloat(num.toString())
            when {
                num.isNaN() -> assertTrue(decoded.isNaN())
                else -> assertFloatEquals(num, decoded)
            }
        }
    }

    @Test
    fun doubles() {
        for ((input, expected) in unusualNumbers) {
            assertDoubleEquals(expected, FloatingDecimalParsing.parseDouble(input))
        }

        for (num in NumbersDataSet.floats64) {
            val decoded = FloatingDecimalParsing.parseDouble(num.toString())
            when {
                num.isNaN() -> assertTrue(decoded.isNaN())
                else -> assertDoubleEquals(num, decoded)
            }
        }
    }

    private val randomStrings = arrayOf("", ".", "99")

    @Test
    fun floats_from_buffer_range() {
        for (num in NumbersDataSet.floats32) {
            val expected = num.toString()
            for (prefix in randomStrings)
            for (suffix in randomStrings) {
                val decoded = FloatingDecimalParsing.readBuffer(
                    "$prefix${expected}$suffix".encodeToByteArray().asArrayBuffer(),
                    start = prefix.length,
                    endExclusive = prefix.length + expected.length
                ).floatValue()
                if (num.isNaN() && decoded.isNaN()) continue
                assertFloatEquals(num, decoded)
            }
        }
    }

    @Test
    fun doubles_from_buffer_range() {
        for (num in NumbersDataSet.floats64) {
            val expected = num.toString()
            for (prefix in randomStrings)
            for (suffix in randomStrings) {
                val decoded = FloatingDecimalParsing.readBuffer(
                    "$prefix${expected}$suffix".encodeToByteArray().asArrayBuffer(),
                    start = prefix.length,
                    endExclusive = prefix.length + expected.length
                ).doubleValue()
                if (num.isNaN() && decoded.isNaN()) continue
                assertDoubleEquals(num, decoded)
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

    private fun assertDoubleEquals(expected: Double, decoded: Double, tolerance: Double = 0.0001) {
        if (expected.isNaN() && decoded.isNaN()) return

        if (expected.isInfinite() && decoded.isInfinite()) {
            assertEquals(expected < 0, decoded < 0)
        }
        if (decoded !in (expected - tolerance .. expected + tolerance)) {
            fail("expected=$expected\ndecoded=$decoded")
        }
    }

    private fun assertFloatEquals(expected: Float, decoded: Float, tolerance: Float = 0.0001f) {
        if (decoded !in (expected - tolerance .. expected + tolerance))
            fail("expected=$expected\ndecoded=$decoded")
    }

    private val badStrings: Array<String> = arrayOf(
        "",
        "x",
        "*",
        "f",
        "d",
        "f100",

        "+",
        "-",
        "+e",
        "-e",
        "+e170",
        "-e170",  // Make sure intermediate white space is not deleted.

        "1234   e10",
        "-1234   e10",  // Control characters in the interior of a string are not legal

        "1\u0007e1",
        "1e\u00071",  // NaN and infinity can't have trailing type suffices or exponents

        "1.1E-10\u0066",  // Culled from JCK test lex03595m1

        "+NaN",
        "-NaN",
        "NaNe10",
        "-NaNe10",
        "+NaNe10",
        "Infinitye10",
        "-Infinitye10",
        "+Infinitye10",  // Non-ASCII digits are not recognized

        "\u0661e\u0661",  // 1e1 in Arabic-Indic digits
        "\u06F1e\u06F1",  // 1e1 in Extended Arabic-Indic digits
        "\u0967e\u0967",  // 1e1 in Devanagari digits

        // JCK
        ".",
        "e42",
        ".e42",
        "1234L.01",
        "12ee-2",
        "12e-2.2.2",
        "12.01e+",
        "12.01E"
    )

    private val goodStrings: Array<String> = arrayOf(
        "NaN",
        "Infinity",
        "+Infinity",
        "-Infinity",
        "1.1e-23",
        ".1e-23",
        "1e-23",
        "1",
        "0",
        "-0",
        "+0",
        "00",
        "00",
        "-00",
        "+00",
        "0000000000",
        "-0000000000",
        "+0000000000",
        "1",
        "2",
        "1234",
        "-1234",
        "+1234",
        "2147483647",  // Integer.MAX_VALUE
        "2147483648",
        "-2147483648",  // Integer.MIN_VALUE
        "-2147483649",

        "16777215",
        "16777216",  // 2^24
        "16777217",

        "-16777215",
        "-16777216",  // -2^24
        "-16777217",

        "9007199254740991",
        "9007199254740992",  // 2^53
        "9007199254740993",

        "-9007199254740991",
        "-9007199254740992",  // -2^53
        "-9007199254740993",

        "9223372036854775807",
        "9223372036854775808",  // Long.MAX_VALUE
        "9223372036854775809",

        "-9223372036854775808",
        "-9223372036854775809", // Long.MIN_VALUE
        "-9223372036854775810",

        // Culled from JCK test lex03591m1
        "54.07140",
        "7.01e-324",
        "2147483647.01",
        "1.2147483647",
        "000000000000000000000000001.",
        "1.00000000000000000000000000e-2",

        // Culled from JCK test lex03592m2
        "2.",
        ".0909",
        "122112217090.0",
        "7090e-5",
        "2.E-20",
        ".0909e42",
        "122112217090.0E+100",
        "7090",
        "2.",
        ".0909",
        "122112217090.0",
        "7090e-5",
        "2.E-20",
        ".0909e42",
        "122112217090.0E+100",

        "0.0E-10",
        "1E10",

        // Culled from JCK test lex03691m1
        "0.",
        "1",
        "0.12",
        "1e-0",
        "12.e+1",
        "0e-0",
        "12.e+01",
        "1e-01",

        "1.7976931348623157E308",  // Double.MAX_VALUE
        "4.9e-324",  // Double.MIN_VALUE
        "2.2250738585072014e-308",  // Double.MIN_NORMAL

        "2.2250738585072012e-308",  // near Double.MIN_NORMAL

        "1.7976931348623158e+308",  // near MAX_VALUE + ulp(MAX_VALUE)/2
        "1.7976931348623159e+308",  // near MAX_VALUE + ulp(MAX_VALUE)

        "2.4703282292062329e-324",  // above MIN_VALUE/2
        "2.4703282292062327e-324",  // MIN_VALUE/2
        "2.4703282292062325e-324",  // below MIN_VALUE/2

        // 1e308 with leading zeros
        "0.0000000000001e321",
        "00.000000000000000001e326",
        "00000.000000000000000001e326",
        "000.0000000000000000001e327",
        "0.00000000000000000001e328",
    )

    @Test
    fun bad_double_strings() {
        for (s in badStrings) {
            assertFailsWith<NumberFormatException>(s) {
                FloatingDecimalParsing.parseDouble(s)
            }
        }
    }

    @Test
    fun good_double_strings() {
        for (s in goodStrings) {
            assertDoubleEquals(s.toDouble(), FloatingDecimalParsing.parseDouble(s))
        }
    }
}