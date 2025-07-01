package io.kodec

import kotlin.test.Test
import kotlin.test.assertEquals

class StringsUtf16Test: StringsTestBase() {
    override fun readFromBytes(
        getByte: (Int) -> Int,
        endExclusive: Int,
        appendChar: (Char) -> Unit
    ): Int {
        var i = 0
        StringsUTF16.readFromByteStream(
            readByte = { if (i < endExclusive) getByte(i++) else -1 },
            appendChar
        )
        return i
    }

    override fun readFromByteStream(readByte: () -> Int, appendChar: (Char) -> Unit) =
        StringsUTF16.readFromByteStream(readByte, appendChar)

    override fun write(
        str: CharSequence,
        offset: Int,
        endExclusive: Int,
        writeByte: (Byte) -> Unit
    ): Int = StringsUTF16.writeBytes(str, offset, endExclusive, writeByte = { writeByte(it.toByte()) })

    @Test
    fun constants() {
        assertEquals(65536, StringsUTF16.MIN_SUPPLEMENTARY_CODE_POINT)
    }

    @Test
    fun code_point_count() {
        assertEquals(0, StringsUTF16.countCodePoints(""))
        assertEquals(5, StringsUTF16.countCodePoints("Hello"))
        assertEquals(6, StringsUTF16.countCodePoints("Привет"))
        assertEquals(1, StringsUTF16.countCodePoints("\uD801\uDC37"))
        assertEquals(2, StringsUTF16.countCodePoints("\uD801\uDC37 "))
        assertEquals(3, StringsUTF16.countCodePoints("\uD801\uDC37 \uD801\uDC37"))
        assertEquals(4, StringsUTF16.countCodePoints("\uD801\uDC37 \uD801\uDC37 "))
        assertEquals(32, StringsUTF16.countCodePoints("﷽ WTF is that? \uD809\uDC2B\uD808\uDE19⸻ and finally ꧅"))
    }
}