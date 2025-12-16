package io.kodec.text

import io.kodec.StringsASCII
import kotlin.test.assertEquals

class StringTextReaderTest: AbstractTextReaderTest() {
    override val reader: StringTextReader = StringTextReader.startReadingFrom("")

    override fun setText(text: String) {
        reader.startReadingFrom(text)
    }

    fun testReadSurrogatePair() {
        // Create surrogate pair using constants
        val surrogatePair = charArrayOf(
            Char.MIN_HIGH_SURROGATE,
            Char.MIN_LOW_SURROGATE
        ).concatToString()
        reader.startReadingFrom(surrogatePair)

        val codePoint = reader.readCodePoint()
        assertEquals(0x10000, codePoint)

        assertEquals(2, reader.position)
    }

    fun testIncompleteSurrogatePair() {
        // Test with high surrogate at boundary
        val highSurrogate = Char.MAX_HIGH_SURROGATE
        reader.startReadingFrom(highSurrogate.toString())

        val codePoint = reader.readCodePoint()
        assertEquals(StringsASCII.INVALID_BYTE_PLACEHOLDER.code, codePoint)

        assertEquals(1, reader.position)
    }
}
