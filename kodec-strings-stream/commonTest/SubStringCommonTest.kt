package io.kodec.text

import dev.adokky.eqtester.EqualsTesterConfigBuilder
import dev.adokky.eqtester.testEquality
import io.kodec.buffers.asDataBuffer
import karamel.utils.enrichMessageOf
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SubStringCommonTest {
    @Test
    fun simple() {
        arrayOf("", " ", "abc", "hello world").forEach { string ->
            repeat((string.length * string.length + 1).coerceAtMost(100)) {
                testRandomIndices(string)
            }
        }
    }

    private fun testRandomIndices(string: String) {
        val start = string.indices.randomOrNull() ?: 0
        val end = Random.nextInt(start, string.length + 1)

        enrichMessageOf<Throwable>({ "on string: $string. range: ${start..end}" }) {
            val ss = string.substringWrapper(start, end)
            val expectedSubString = string.substring(start, end)

            assertEquals(expectedSubString, ss.toString())

            assertEquals(ss.copy(), ss)
            assertEquals(ss, ss.copy())

            val changed = string.replace(" ", "_")
            assertEquals(
                changed.substring(start, end) == expectedSubString,
                ss == changed.substringWrapper(start, end),
                message = changed
            )
        }
    }

    private val original = "\uD801\uDC37Привет, Мир! ࠀ"
    private val prefix = "_" // WARN: must be all ASCII!
    private val s = prefix + original
    private val modifiedEnd = 8

    private val ss1: AbstractSubString = s.substringWrapper(start = prefix.length)
    private val ss2: AbstractSubString = run {
        val bytes = s.encodeToByteArray().asDataBuffer()
        Utf8TextReader.startReadingFrom(bytes, position = prefix.length).readSubStringInline { true }
    }
    private val ss3: AbstractSubString = run {
        StringTextReader.startReadingFrom(s, position = prefix.length).readSubStringInline { true }
    }

    private val modifiedSs1: AbstractSubString = s.substringWrapper(start = prefix.length, end = modifiedEnd)
    private val modifiedSs2: AbstractSubString = run {
        val bytes = s.encodeToByteArray().asDataBuffer()
        val byteLength = modifiedSs1.toString().encodeToByteArray().size
        Utf8TextReader.startReadingFrom(bytes, position = prefix.length).substring(prefix.length, prefix.length + byteLength)
    }

    private fun String.asBufferSubString(start: Int, end: Int): TextReaderSubString {
        val bytes = encodeToByteArray().asDataBuffer()
        var pos = 0
        return Utf8TextReader.startReadingFrom(bytes, position = start)
            .readSubStringInline { pos++ < end }
    }

    @Test
    fun to_string() {
        assertEquals(original, ss1.toString())
        assertEquals(original, ss2.toString())
        assertEquals(original, ss3.toString())

        assertEquals(modifiedSs1.toString(), modifiedSs2.toString())

        s.substring(1, modifiedEnd).let {
            assertEquals(it, modifiedSs1.toString())
            assertEquals(it, modifiedSs2.toString())
        }
    }

    @Test
    fun hash_code() {
        fun check(ss: AbstractSubString) {
            assertNotEquals(0, ss.hashCode())

            val expectedHashCode = ss.toString().hashCode() + 1
            assertEquals(expectedHashCode, ss.hashCode())

            ss.resetCache()
            assertNotEquals(0, ss.hashCode())

            ss.resetCache()
            assertEquals(expectedHashCode, ss.hashCode())
        }

        check(ss1)
        check(ss2)
        check(ss3)
        check(modifiedSs1)
        check(modifiedSs2)
    }

    @Test
    fun text_reader_substring_hash_code_set() {
        val ss = TextReaderSubString()

        assertEquals(0, ss.cachedHashCode)

        ss.resetCache()
        assertEquals(0, ss.cachedHashCode)

        ss.resetCache(111)
        assertEquals(112, ss.cachedHashCode)

        ss.set(StringTextReader.Empty, 0, 0, 0, hashCode = -123)
        assertEquals(-122, ss.cachedHashCode)

        ss.setUnchecked(StringTextReader.Empty, 0, 0, 0, hashCode = 456)
        assertEquals(457, ss.cachedHashCode)

        val encoded = "abc".encodeToByteArray().asDataBuffer()
        ss.set(Utf8TextReader.startReadingFrom(encoded), 0, encoded.endExclusive, codePoints = 3)
        assertEquals("abc".hashCode() + 1, ss.hashCode())

        ss.set(StringTextReader.startReadingFrom("xyz"), 0, encoded.endExclusive, codePoints = 3)
        assertEquals("xyz".hashCode() + 1, ss.hashCode())
    }

    @Test
    fun simple_substring_hash_code_set() {
        val ss = SimpleSubString()

        assertEquals(0, ss.cachedHashCode)

        ss.resetCache()
        assertEquals(0, ss.cachedHashCode)

        ss.resetCache(111)
        assertEquals(112, ss.cachedHashCode)

        ss.set("", 0, 0, hashCode = -123)
        assertEquals(-122, ss.cachedHashCode)

        ss.setUnchecked("", 0, 0, hashCode = 456)
        assertEquals(457, ss.cachedHashCode)

        ss.set("abc")
        assertEquals("abc".hashCode() + 1, ss.hashCode())
    }

    fun RandomAccessTextReader.charSubString(startCharIndex: Int, endCharIndex: Int): AbstractSubString {
        var charIndex = 0
        readCharsWhile { charIndex++ != startCharIndex }
        val ssStart = position
        charIndex--

        val codepoints = readCharsHeavyInline { charIndex++ != endCharIndex }
        val ssEnd = position

        return TextReaderSubString().also {
            it.set(this, ssStart, ssEnd, codepoints)
        }
    }

    @Test
    fun equality_auto() {
        val text = "АБ_В\uD801\uDC37GD Eё-ЖzࠀzZ3^&457`!"

        fun EqualsTesterConfigBuilder.group(range: IntRange) {
            val end = range.endInclusive + 1
            val ss1 = text.substringWrapper(range.start, end)
            val ss2 = StringTextReader.startReadingFrom(text).charSubString(range.start, end)
            val ss3 = run {
                val bytes = text.encodeToByteArray().asDataBuffer()
                Utf8TextReader.startReadingFrom(bytes).charSubString(range.start, end)
            }
            group(ss1, ss2, ss3)
        }

        testEquality {
            group("".substringWrapper(), TextReaderSubString(StringTextReader.Empty, 0, 0, 0), TextReaderSubString(Utf8TextReader.Empty, 0, 0, 0))
            group(1..1)
            group(1..text.length-1)
            group(2..text.length-1)
            group(0..text.length-4)
            group(0..text.length-5)
            group(6..text.length-9)
            group(4..text.length-10)
        }
    }

    @Test
    fun equality() {
        assertEquals(ss1, ss1)
        assertEquals(ss1, ss2)
        assertEquals(ss1, ss3)

        assertEquals(ss2, ss1)
        assertEquals(ss2, ss2)
        assertEquals(ss2, ss3)

        assertEquals(ss3, ss1)
        assertEquals(ss3, ss2)
        assertEquals(ss3, ss3)

        assertEquals(modifiedSs1, modifiedSs2)
        assertEquals(modifiedSs2, modifiedSs1)

        assertNotEquals(ss1, modifiedSs1)
        assertNotEquals(ss2, modifiedSs1)
        assertNotEquals(ss1, modifiedSs2)
        assertNotEquals(ss2, modifiedSs2)

        assertNotEquals(modifiedSs1, ss1)
        assertNotEquals(modifiedSs1, ss2)
        assertNotEquals(modifiedSs2, ss1)
        assertNotEquals(modifiedSs2, ss2)

        assertNotEquals(
            "12345".asBufferSubString(1, 3),
            "12345".asBufferSubString(2, 4)
        )

        assertEquals(
            "123454321".asBufferSubString(0, 1),
            "123454321".asBufferSubString(8, 9)
        )
    }
}