package io.kodec.text

import kotlin.test.Test
import kotlin.test.assertEquals

class ReadAtPositionTest {
    private fun testWithReader(input: String, testBlock: StringTextReader.() -> Unit) {
        StringTextReader.startReadingFrom(input).use {
            testBlock(it)
        }
    }

    @Test
    fun `test readBoolean at position`() = testWithReader("true false") {
        assertEquals(true, readBoolean(0))
        assertEquals(false, readBoolean(5))
    }

    @Test
    fun `test readByte at position`() = testWithReader("123 -45") {
        assertEquals(123.toByte(), readByte(0))
        assertEquals((-45).toByte(), readByte(4))
    }

    @Test
    fun `test readShort at position`() = testWithReader("12345 -6789") {
        assertEquals(12345.toShort(), readShort(0))
        assertEquals((-6789).toShort(), readShort(6))
    }

    @Test
    fun `test readInt at position`() = testWithReader("1234567890 -987654321") {
        assertEquals(1234567890, readInt(0))
        assertEquals(-987654321, readInt(11))
    }

    @Test
    fun `test readLong at position`() = testWithReader("1234567890123456789 -987654321098765432") {
        assertEquals(1234567890123456789L, readLong(0))
        assertEquals(-987654321098765432L, readLong(20))
    }

    @Test
    fun `test readFloat at position`() = testWithReader("123.45 -67.89 Infinity -Infinity NaN") {
        assertEquals(123.45f, readFloat(0))
        assertEquals(-67.89f, readFloat(7))
        assertEquals(Float.POSITIVE_INFINITY, readFloat(14, allowSpecialValues = true))
        assertEquals(Float.NEGATIVE_INFINITY, readFloat(23, allowSpecialValues = true))
        assertEquals(Float.NaN, readFloat(33, allowSpecialValues = true))
    }

    @Test
    fun `test readDouble at position`() = testWithReader("123.456789012345 -67.890123456789 Infinity -Infinity NaN") {
        assertEquals(123.456789012345, readDouble(0))
        assertEquals(-67.890123456789, readDouble(17))
        assertEquals(Double.POSITIVE_INFINITY, readDouble(34, allowSpecialValues = true))
        assertEquals(Double.NEGATIVE_INFINITY, readDouble(43, allowSpecialValues = true))
        assertEquals(Double.NaN, readDouble(53, allowSpecialValues = true))
    }

    @Test
    fun `test readStringSized at position`() = testWithReader("Hello, World!") {
        assertEquals("Hello", readStringSized(0, 5))
        assertEquals("World", readStringSized(7, 5))
    }

    @Test
    fun `test readStringCodePointSized at position`() = testWithReader("Hello, 世界!") {
        assertEquals("Hello", readStringCodePointSized(0, 5))
        assertEquals("世界", readStringCodePointSized(7, 2))
    }

    @Test
    fun `test substring at position`() = testWithReader("Hello, World!") {
        assertEquals("Hello", substring(0, 5).toString())
        assertEquals("World", substring(7, 12).toString())
        assertEquals("World!", substring(7).toString())
    }

    @Test
    fun `test comprehensive data set`() = testWithReader(
        StringBuilder()
            .append("123 -456 78.9 true false ")
            .append("Infinity -Infinity NaN ")
            .append("hello world")
            .toString()
    ) {
        var pos = 0
        
        // Test integers
        assertEquals(123, readInt(pos))
        pos += 4 // "123 "
        assertEquals(-456, readInt(pos))
        pos += 5 // "-456 "
        
        // Test float
        assertEquals(78.9, readDouble(pos))
        pos += 5 // "78.9 "
        
        // Test booleans
        assertEquals(true, readBoolean(pos))
        pos += 5 // "true "
        assertEquals(false, readBoolean(pos))
        pos += 6 // "false "
        
        // Test special floats
        assertEquals(Double.POSITIVE_INFINITY, readDouble(pos, allowSpecialValues = true))
        pos += 9 // "Infinity "
        assertEquals(Double.NEGATIVE_INFINITY, readDouble(pos, allowSpecialValues = true))
        pos += 10 // "-Infinity "
        assertEquals(Double.NaN, readDouble(pos, allowSpecialValues = true))
        pos += 4 // "NaN "
        
        // Test strings
        assertEquals("hello", readStringSized(pos, 5))
        pos += 6 // "hello "
        assertEquals("world", readStringSized(pos, 5))
    }
}
