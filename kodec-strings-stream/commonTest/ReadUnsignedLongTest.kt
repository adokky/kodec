package io.kodec.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReadUnsignedLongTest {
    private val reader = StringTextReader.Empty

    @Test
    fun happy_path() {
        fun check(l: ULong) {
            reader.startReadingFrom(l.toString())
            assertEquals(l, reader.readUnsignedLong())

            reader.startReadingFrom("$l:")
            assertEquals(l, reader.readUnsignedLong())

            reader.startReadingFrom("${l}z")
            assertFailsWith<TextDecodingException> { reader.readUnsignedLong() }

            reader.startReadingFrom(" $l")
            assertFailsWith<TextDecodingException> { reader.readUnsignedLong() }
        }

        check(0uL)
        check(Long.MAX_VALUE.toULong() + 1uL)
        check(ULong.MAX_VALUE - 1000uL)
        check(ULong.MAX_VALUE - 1uL)
        check(ULong.MAX_VALUE)

        reader.startReadingFrom("+123")
        assertEquals(123uL, reader.readUnsignedLong())
    }

    @Test
    fun bad_path() {
        fun check(input: String, failure: IntegerParsingError) {
            reader.startReadingFrom(input)
            assertFailsWith<TextDecodingException>(input) { reader.readUnsignedLong() }

            reader.startReadingFrom(input)
            var caught: IntegerParsingError? = null
            reader.readUnsignedLong(onFail = { err -> caught = err })
            assertEquals(failure, caught)
        }

        arrayOf(
            " ",
            "0e",
            "1e1",
            "e",
            "e1",
            "ABC",
            "1.0",
            "-1",
            "-0",
            "1z",
            "18446744073_709551615"
        ).forEach { input ->
            check(input, IntegerParsingError.MalformedNumber)
        }

        arrayOf(
            "18446744073709551616", // ULong.MAX_VALUE + 1
            "100000000000000000000"
        ).forEach { input ->
            check(input, IntegerParsingError.Overflow)
        }
    }
}