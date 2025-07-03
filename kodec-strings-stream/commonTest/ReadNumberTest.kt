package io.kodec.text

import io.kodec.NumbersDataSet
import karamel.utils.Bits32
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

class ReadNumberTest {
    private val reader = StringTextReader.Empty

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
    }

    private fun checkInteger(
        expected: Long,
        input: String = expected.toString(),
        terminationClasses: Bits32<DefaultCharClasses> = DefaultCharClasses.WORD_TERM
    ) {
        reader.startReadingFrom(input)
        var result: Long? = null
        reader.readNumberTemplate(
            acceptInt = { result = it },
            acceptFloat = { fail("expected acceptInt() but called acceptFloat() for '$input'") },
            charClasses = DefaultCharClasses.mapper,
            terminatorClass = terminationClasses
        )
        assertEquals(expected, result, input)
    }

    private fun checkFloat(
        expected: Double,
        input: String = expected.toString(),
        allowSpecial: Boolean = false,
        terminationClasses: Bits32<DefaultCharClasses> = DefaultCharClasses.WORD_TERM
    ) {
        reader.startReadingFrom(input)
        var result: Double? = null
        reader.readNumberTemplate(
            acceptInt = { fail("expected acceptFloat() but called acceptInt() for '$input'") },
            acceptFloat = { result = it },
            allowSpecialFp = allowSpecial,
            charClasses = DefaultCharClasses.mapper,
            terminatorClass = terminationClasses
        )
        assertNotNull(result)
        if (expected.isNaN()) {
            if (!result.isNaN()) {
                fail("input='$input'\nexpected NaN\ndecoded=$result")
            }
        } else {
            if (expected !in (result - 0.0001 .. result + 0.0001)) {
                fail("input='$input'\nexpected=$expected\ndecoded=$result")
            }
        }
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

        arrayOf(
            "-9223372036854775809L", //  Long.MIN_VALUE - 1
            (Long.MAX_VALUE.toULong() + 1uL).toString(),
            ULong.MAX_VALUE.toString(),
            "100000000000000000000",
        ).forEach { checkFails(it, NumberParsingError.IntegerOverflow) }

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
}