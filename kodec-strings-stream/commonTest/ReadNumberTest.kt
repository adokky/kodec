package io.kodec.text

import io.kodec.NumbersDataSet
import karamel.utils.Bits32
import karamel.utils.enrichMessageOf
import kotlin.math.sign
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

class ReadNumberTest {
    private val reader = StringTextReader.Empty
    private val result = ReadNumberResult()

    private fun checkFails(
        input: String,
        expected: NumberParsingError,
        terminationClasses: Bits32<DefaultCharClasses> = DefaultCharClasses.WORD_TERM
    ) {
        reader.startReadingFrom(input)
        var failure: NumberParsingError? = null
        reader.readNumberTemplate(
            acceptInt = { fail("expected failure but called acceptInt() for input '$input'") },
            acceptFloat = { fail("expected failure but called acceptFloat() for input '$input'") },
            onFail = { failure = it },
            charClasses = DefaultCharClasses.mapper,
            terminatorClass = terminationClasses
        )
        assertEquals(expected, failure, input)

        reader.startReadingFrom(input)
        val errorContainer = reader.errorContainer.prepare<NumberParsingError>()
        reader.readNumber(
            result.clear(),
            onFail = errorContainer,
            charClasses = DefaultCharClasses.mapper,
            terminatorClass = terminationClasses
        )
        assertEquals(expected, errorContainer.consumeError(), input)
        assertEquals(0.0, result.asDouble)
        assertEquals(0, result.asLong)
        assertFalse(result.isDouble)
    }

    private fun checkInteger(
        expected: Long,
        input: String = expected.toString(),
        terminationClasses: Bits32<DefaultCharClasses> = DefaultCharClasses.WORD_TERM
    ) {
        reader.startReadingFrom(input)
        var long: Long? = null
        reader.readNumberTemplate(
            acceptInt = { long = it },
            acceptFloat = { fail("expected acceptInt() but called acceptFloat() for '$input'") },
            charClasses = DefaultCharClasses.mapper,
            terminatorClass = terminationClasses
        )
        assertEquals(expected, long, input)

        reader.startReadingFrom(input)
        result.clear()
        reader.readNumber(result, DefaultCharClasses.mapper, terminationClasses)
        assertEquals(0.0, result.asDouble)
        assertEquals(expected, result.asLong)
        assertFalse(result.isDouble)
    }

    private fun checkFloat(
        expected: Double,
        input: String = expected.toString(),
        allowSpecial: Boolean = false,
        terminationClasses: Bits32<DefaultCharClasses> = DefaultCharClasses.WORD_TERM
    ) {
        reader.startReadingFrom(input)
        var decoded: Double? = null
        reader.readNumberTemplate(
            acceptInt = { fail("expected acceptFloat() but called acceptInt() for '$input'") },
            acceptFloat = { decoded = it.doubleValue() },
            allowSpecialFp = allowSpecial,
            charClasses = DefaultCharClasses.mapper,
            terminatorClass = terminationClasses
        )
        assertNotNull(decoded)

        when {
            expected.isNaN() -> {
                if (!decoded.isNaN()) {
                    fail("input='$input'\nexpected NaN\ndecoded=$decoded")
                }
            }
            !expected.isFinite() -> {
                assertTrue(!decoded.isFinite(), "expected infinity got $decoded")
                assertEquals(expected.sign, decoded.sign, "infinity has wrong sign")
            }
            else -> {
                if (expected !in (decoded - 0.0001 .. decoded + 0.0001)) {
                    fail("input='$input'\nexpected=$expected\ndecoded=$decoded")
                }
            }
        }

        reader.startReadingFrom(input)
        result.clear()
        reader.readNumber(result, DefaultCharClasses.mapper, terminationClasses, allowSpecialFp = allowSpecial)
        assertEquals(decoded, result.asDouble)
        assertEquals(0, result.asLong)
        assertTrue(result.isDouble)
    }

    @Test
    fun ints_happy_path() {
        for (int in NumbersDataSet.ints64) {
            checkInteger(int)
            checkInteger(int, "$int ")
            checkInteger(int, "$int:")
            checkInteger(int, "$int,")
        }
    }

    @Test
    fun float_happy_path() {
        for (float in NumbersDataSet.floats64) {
            if (!float.isFinite()) continue
            checkFloat(float)
            checkFloat(float, "$float ")
            checkFloat(float, "$float:")
            checkFloat(float, "$float,")
        }

        checkFloat(Double.NEGATIVE_INFINITY, allowSpecial = true)
        checkFloat(Double.POSITIVE_INFINITY, allowSpecial = true)
        checkFloat(Double.NaN, allowSpecial = true)
    }

    @Test
    fun bad_path() {
        arrayOf(
            "", " ", "-", "-.", ".", "1..", ".0.", "1z", "Z", "abc", "334-", "1.0z", "e", "0.e", "e.", "e.1", "1e3.e1", "1ez", "1e2z",
        ).forEach { checkFails(it, NumberParsingError.MalformedNumber) }

        val array = arrayOf(
            "-9223372036854775809", //  Long.MIN_VALUE - 1
            (Long.MAX_VALUE.toULong() + 1uL).toString(),
            ULong.MAX_VALUE.toString(),
            "100000000000000000000",
        )
        array.forEach { checkFails(it, NumberParsingError.IntegerOverflow) }

        checkFails("100000000000000000000.0", NumberParsingError.FloatOverflow)

        checkFails(Double.NEGATIVE_INFINITY.toString(), NumberParsingError.MalformedNumber)
        checkFails(Double.POSITIVE_INFINITY.toString(), NumberParsingError.MalformedNumber)
        checkFails(Double.NaN.toString(), NumberParsingError.MalformedNumber)
    }

    @Test
    fun custom_terminator() {
        checkFloat(123.4556, "123.4556/")
        checkFloat(123.4556, "123.4556 ", terminationClasses = DefaultCharClasses.WHITESPACE)
        checkFails("123.4556/", NumberParsingError.MalformedNumber, DefaultCharClasses.WHITESPACE)

        checkInteger(123, "123/")
        checkInteger(123, "123 ", terminationClasses = DefaultCharClasses.WHITESPACE)
        checkFails("123/", NumberParsingError.MalformedNumber, DefaultCharClasses.WHITESPACE)
    }

    private val floatStrings = arrayOf(
        "NaN",
        "Infinity",
        "-Infinity",
        "1.1e-23",
        ".1e-23",
        "1e-23",

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
        "2.",
        ".0909",
        "122112217090.0",
        "7090e-5",
        "2.E-20",
        ".0909e42",
        "122112217090.0E+100",

        "0.0E-10",

        // Culled from JCK test lex03691m1
        "0.",
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
    fun floats() {
        for (string in floatStrings) {
            enrichMessageOf<Throwable>({ string }) {
                checkFloat(expected = string.toDouble(), input = string, allowSpecial = true)
            }
        }
    }

    @Test
    fun overflows() {
        fun check(input: String, expected: NumberParsingError) {
            reader.startReadingFrom(input)
            val errorContainer = reader.errorContainer.prepare<NumberParsingError>()
            reader.readNumber(
                result.clear(),
                onFail = errorContainer,
                charClasses = DefaultCharClasses.mapper,
                terminatorClass = DefaultCharClasses.WORD_TERM
            )
            assertEquals(expected, errorContainer.consumeError())
            assertEquals(input.length, reader.position)
        }

        check("99999999999999999999999.9999999999999999999999", NumberParsingError.FloatOverflow)
        check("9223372036854775809.0", NumberParsingError.FloatOverflow)
        check("-9223372036854775810.0", NumberParsingError.FloatOverflow)
        check("9223372036854775809", NumberParsingError.IntegerOverflow)
        check("-9223372036854775810", NumberParsingError.IntegerOverflow)
        check("1e20", NumberParsingError.IntegerOverflow)
    }
}