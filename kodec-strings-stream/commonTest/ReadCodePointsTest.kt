package io.kodec.text

import io.kodec.buffers.asArrayBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReadCodePointsTest {
    @Test
    fun testReadCodePointsWithDifferentReaders() {
        val text = "Hello🌍World🚀"
        val readers = listOf(
            StringTextReader(text),
            text.encodeToByteArray().asArrayBuffer().let { buffer ->
                Utf8TextReader.startReadingFrom(buffer)
            }
        )

        readers.forEach { reader ->
            val codePoints = buildList {
                reader.readCodePoints(0, 100, ::add)
            }

            assertEquals(12, codePoints.size)
            assertEquals('H'.code, codePoints[0])
            assertEquals('e'.code, codePoints[1])
            assertEquals(0x1F30D, codePoints[5]) // 🌍 symbol
            assertEquals(0x1F680, codePoints[11]) // 🚀 symbol
        }
    }

    @Test
    fun testReadCodePointsRangeWithDifferentReaders() {
        val text = "Hello🌍World"
        val readers = listOf(
            StringTextReader(text),
            text.encodeToByteArray().asArrayBuffer().let { buffer ->
                Utf8TextReader.startReadingFrom(buffer)
            }
        )

        readers.forEach { reader ->
            val codePoints = mutableListOf<Int>()

            // Read only "Hello"
            reader.readCodePoints(0, 5) { cp ->
                codePoints.add(cp)
            }

            assertEquals(5, codePoints.size)
            assertEquals("Hello".toList().map { it.code }, codePoints)
        }
    }

    @Test
    fun testReadCodePointsEmptyRangeWithDifferentReaders() {
        val text = "Hello"
        val readers = listOf(
            StringTextReader(text),
            text.encodeToByteArray().asArrayBuffer().let { buffer ->
                Utf8TextReader.startReadingFrom(buffer)
            }
        )

        readers.forEach { reader ->
            val codePoints = mutableListOf<Int>()

            reader.readCodePoints(2, 2) { cp ->
                codePoints.add(cp)
            }

            assertTrue(codePoints.isEmpty())
        }
    }

    @Test
    fun testReadCodePointsWithSurrogates() {
        // surrogate pair in the middle
        val text = "A\ud83d\ude00B" // A + 😀 + B
        val readers = listOf(
            StringTextReader(text),
            text.encodeToByteArray().asArrayBuffer().let { buffer ->
                Utf8TextReader.startReadingFrom(buffer)
            }
        )

        readers.forEach { reader ->
            val codePoints = mutableListOf<Int>()
            reader.readCodePoints(0, 100) { cp ->
                codePoints.add(cp)
            }

            assertEquals(3, codePoints.size)
            assertEquals('A'.code, codePoints[0])
            assertEquals(0x1F600, codePoints[1]) // 😀 symbol
            assertEquals('B'.code, codePoints[2])
        }
    }

    @Test
    fun testReadCodePointsWithHighSurrogateAtEnd() {
        // Incomplete surrogate pair at end
        val text = "A\ud83d" // A + incomplete surrogate pair
        val reader = StringTextReader(text)

        val codePoints = mutableListOf<Int>()
        reader.readCodePoints(0, text.length) { cp ->
            codePoints.add(cp)
        }

        // Expect replacement of incomplete surrogate pair
        assertEquals(2, codePoints.size)
        assertEquals('A'.code, codePoints[0])
    }

    @Test
    fun testReadCodePointsWithLowSurrogateAtStart() {
        val text = "\ude00A" // incomplete surrogate pair + A
        val reader = StringTextReader(text)

        val codePoints = mutableListOf<Int>()
        reader.readCodePoints(0, text.length) { cp ->
            codePoints.add(cp)
        }

        // UTF-8 reader should handle this correctly
        assertEquals(2, codePoints.size)
        assertEquals('A'.code, codePoints[1])
    }
}
