package io.kodec.text

import io.kodec.StringsDataSet
import io.kodec.StringsUTF16
import io.kodec.buffers.asBuffer
import karamel.utils.enrichMessageOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReadCodePointAtPositionTest {
    private fun RandomAccessTextReader.ensurePositionNotChanged(body: RandomAccessTextReader.() -> Unit) {
        val p = position
        val np = nextPosition
        body()
        assertEquals(p, position)
        assertEquals(np, nextPosition)
    }

    private fun test(input: String, body: RandomAccessTextReader.() -> Unit) {
        enrichMessageOf<Throwable>("Utf8TextReader") {
            Utf8TextReader.startReadingFrom(input.encodeToByteArray().asBuffer())
                .ensurePositionNotChanged(body)
        }
        enrichMessageOf<Throwable>("StringTextReader") {
            StringTextReader.startReadingFrom(input)
                .ensurePositionNotChanged(body)
        }
    }

    @Test
    fun empty() = test("") {
        assertEquals(CodePointAndSize.EOF, readCodePoint(0))
        assertEquals(CodePointAndSize.EOF, readCodePoint(0))
        assertEquals(CodePointAndSize.EOF, readCodePoint(1))
    }

    @Test
    fun single() = test("A") {
        assertEquals(CodePointAndSize('A'.code, 1), readCodePoint(0))
        assertEquals(CodePointAndSize.EOF, readCodePoint(1))
    }

    @Test
    fun multi_byte_utf8() = Utf8TextReader.startReadingFrom("фaꙊ\uD801\uDC37".encodeToByteArray().asBuffer()).ensurePositionNotChanged {
        assertEquals(CodePointAndSize.EOF, readCodePoint(10))
        assertEquals(CodePointAndSize('Ꙋ'.code, 3), readCodePoint(3))
        assertEquals(CodePointAndSize(StringsUTF16.codePoint('\uD801'.code, '\uDC37'.code), 4), readCodePoint(6))
        assertEquals(CodePointAndSize('a'.code, 1), readCodePoint(2))
        assertEquals(CodePointAndSize('ф'.code, 2), readCodePoint(0))
        assertEquals(CodePointAndSize.EOF, readCodePoint(11))
    }

    @Test
    fun multi_char_string() = StringTextReader.startReadingFrom("фa\uD801\uDC37Ꙋ").ensurePositionNotChanged {
        assertEquals(CodePointAndSize.EOF, readCodePoint(5))
        assertEquals(CodePointAndSize('Ꙋ'.code, 1), readCodePoint(4))
        assertEquals(CodePointAndSize(StringsUTF16.codePoint('\uD801'.code, '\uDC37'.code), 2), readCodePoint(2))
        assertEquals(CodePointAndSize('a'.code, 1), readCodePoint(1))
        assertEquals(CodePointAndSize('ф'.code, 1), readCodePoint(0))
        assertEquals(CodePointAndSize.EOF, readCodePoint(6))
    }

    @Test
    fun code_point_and_size() {
        CodePointAndSize(-1, -2).also {
            assertEquals(-1, it.codepoint)
            assertEquals(-2, it.size)
        }
        CodePointAndSize(1, 2).also {
            assertEquals(1, it.codepoint)
            assertEquals(2, it.size)
        }
        CodePointAndSize(Int.MAX_VALUE, Int.MAX_VALUE).also {
            assertEquals(Int.MAX_VALUE, it.codepoint)
            assertEquals(Int.MAX_VALUE, it.size)
        }
    }

    @Test
    fun randomized() {
        for (s in StringsDataSet.getUtfData()) {
            test(s) {
                val string = StringBuilder()
                var pos = 0
                while (true) {
                    val r = readCodePoint(pos)
                    assertTrue(r.size in 0..4, r.size.toString())
                    if (r.codepoint < 0) break
                    pos += r.size
                    string.append(r.toString())
                }
                assertEquals(s, string.toString())
            }
        }
    }
}