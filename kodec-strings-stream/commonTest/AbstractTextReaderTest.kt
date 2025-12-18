package io.kodec.text

import io.kodec.*
import karamel.utils.enrichMessageOf
import karamel.utils.nearlyEquals
import kotlin.random.Random
import kotlin.test.*

abstract class AbstractTextReaderTest {
    abstract val reader: RandomAccessTextReader

    abstract fun setText(text: String)

    @Test
    fun initial() {
        assertEquals(0, reader.position)
        reader.position = 0
        assertEquals(0, reader.position)

        setText("")
        assertEquals(0, reader.position)

        setText("ё")
        assertEquals(0, reader.position)
    }

    @Test
    fun empty() {
        setText("")
        assertEquals(0, reader.position)
        assertEquals(-1, reader.readCodePoint())
        assertEquals(0, reader.position)
        assertEquals(-1, reader.readCodePoint())
        assertEquals(0, reader.position)
    }

    @Test
    fun skip_whitespace() {
        setText("")
        reader.skipWhitespace()
        assertEquals(-1, reader.readCodePoint())

        setText("X")
        reader.skipWhitespace()
        assertEquals('X'.code, reader.readCodePoint())

        setText("   a b")
        reader.skipWhitespace()
        assertEquals('a'.code, reader.readCodePoint())
        reader.skipWhitespace()
        assertEquals('b'.code, reader.readCodePoint())
        reader.skipWhitespace()
        assertEquals(-1, reader.readCodePoint())
    }

    @Test
    fun integers() {
        fun test(s: String, expected: Long) {
            setText(s)
            assertEquals(expected, reader.readLong())
            setText(s)
            checkNumber(expected)
        }

        test("00000000000000000000000000000003e1", 30)
        test("1e1", 10)
        test("78e3", 78000)

        for (n in NumbersDataSet.ints64) {
            test(n.toString(), n)
            test((-n).toString(), -n)
        }
    }

    @Test
    fun integers_bad_path() {
        fun check(s: String) {
            setText(s)
            assertFails(s) { reader.readInt() }
            reader.position = 0
            assertFails(s) { reader.readLong() }
        }

        check("")
        check("A")
        check(" ")
        check("AAA")
        check("1000000000000000000999")
    }

    @Test
    fun small_ints() {
        fun Throwable.checkErrorMessage() =
            assertContains(message!!, "too big", ignoreCase = true)

        setText((Byte.MAX_VALUE.toLong() + 1L).toString())
        assertFailsWith<TextDecodingException> { reader.readByte() }.checkErrorMessage()

        setText((Byte.MIN_VALUE.toLong() - 1L).toString())
        assertFailsWith<TextDecodingException> { reader.readByte() }.checkErrorMessage()

        setText((Short.MAX_VALUE.toLong() + 1L).toString())
        assertFailsWith<TextDecodingException> { reader.readShort() }.checkErrorMessage()

        setText((Short.MIN_VALUE.toLong() - 1L).toString())
        assertFailsWith<TextDecodingException> { reader.readShort() }.checkErrorMessage()

        setText((Int.MAX_VALUE.toLong() + 1L).toString())
        assertFailsWith<TextDecodingException> { reader.readInt() }.checkErrorMessage()

        setText((Int.MIN_VALUE.toLong() - 1L).toString())
        assertFailsWith<TextDecodingException> { reader.readInt() }.checkErrorMessage()
    }

    @Test
    fun floats() {
        for (n in NumbersDataSet.floats32.filter { it.isFinite() }) {
            for (nStr in arrayOf(n.toString(), "$n ,", "${n})")) {
                enrichMessageOf<Throwable>({ "failed on: '$nStr'" }) {
                    setText(nStr)
                    assertNearlyEquals(n, reader.readFloat())
                }
            }
        }
    }

    @Test
    fun special_floats() {
        for (allowSpV in arrayOf(false, true)) {
            fun check(input: String, test: (Float) -> Boolean) {
                if (allowSpV) {
                    setText(input)
                    assertTrue(test(reader.readFloat(allowSpecialValues = true)))
                    return
                }

                checkFails { errHandler ->
                    reader.readFloat(allowSpecialValues = false, onFormatError = errHandler)
                }

                setText(input + "ё")
                checkFails { errHandler ->
                    reader.readFloat(allowSpecialValues = false, onFormatError = errHandler)
                }

                assertFailsWith<TextDecodingException> { reader.readFloat() }
            }

            check("-Infinity") { it.isInfinite() && it < 0 }
            check("Infinity") { it.isInfinite() && it > 0 }
            check("NaN") { it.isNaN() }
            check("-0") { it == -0f }
            check("+0") { it == 0f }
        }
    }

    private fun checkFails(handler: (DecodingErrorHandler<Any>) -> Unit) {
        var fired = false
        val errHandler = DecodingErrorHandler<Any> { fired = true }
        handler(errHandler)
        assertTrue(fired, "'onFormatError' is not called")
    }

    @Test
    fun special_doubles() {
        for (allowSpV in arrayOf(false, true)) {
            fun check(input: String, test: (Double) -> Boolean) {
                if (allowSpV) {
                    setText(input)
                    assertTrue(test(reader.readDouble(allowSpecialValues = true)))
                    return
                }

                checkFails { errHandler ->
                    reader.readDouble(allowSpecialValues = false, onFormatError = errHandler)
                }

                setText(input + "ё")
                checkFails { errHandler ->
                    reader.readDouble(allowSpecialValues = false, onFormatError = errHandler)
                }

                assertFailsWith<TextDecodingException> { reader.readDouble() }
            }

            check("-Infinity") { it.isInfinite() && it < 0 }
            check("Infinity") { it.isInfinite() && it > 0 }
            check("NaN") { it.isNaN() }
            check("-0") { it == -0.0 }
            check("+0") { it == 0.0 }
        }
    }

    @Test
    fun seeking() {
        val surrogatePairs = StringsDataSet.getSurrogatePairs(20).toList()

        repeat(20) { iteration ->
            val c1 = CharacterDataSet.ascii().drop(Random.nextInt(100) + 1).first()
            val c2 = CharacterDataSet.utf8_2bytes().drop(Random.nextInt(1000)).first()
            val c3 = CharacterDataSet.utf8_3bytes().toList().random()
            val c4 = surrogatePairs[iteration]
            var s = listOf(c1, c2, c3, c4).shuffled().joinToString("")
            setText(s)

            val got = reader.readStringUntil('\u0000')
            assertEquals(s, got)

            s = listOf(c1, c2, c3, c4).joinToString("")
            setText(s)

            reader.expect(c1)
            val p1 = reader.position
            reader.expect(c2)
            val p2 = reader.position
            reader.expect(c3)
            val p3 = reader.position
            assertEquals(c4, reader.readStringUntil('\u0000'))
            val pEnd = reader.position

            reader.position = p3
            assertEquals(StringsUTF16.codePoint(c4[0].code, c4[1].code), reader.nextCodePoint)
            assertEquals(c4, reader.readStringUntil('\u0000'))

            reader.position = p2
            assertEquals(c3.code, reader.nextCodePoint)
            reader.expect(c3)

            reader.position = p1
            assertEquals(c2.code, reader.nextCodePoint)
            reader.expect(c2)

            reader.position = 0
            assertEquals(c1.code, reader.nextCodePoint)
            reader.expect(c1)

            reader.position = pEnd
            assertEquals(-1, reader.readCodePoint())
            assertEquals(-1, reader.nextCodePoint)
        }
    }

    @Test
    fun floats_bad_path() {
        fun check(s: String) {
            setText(s)
            assertFailsWith<TextDecodingException> { reader.readFloat() }
            reader.position = 0
            assertFailsWith<TextDecodingException> { reader.readDouble() }
        }

        check("e2")
        check("0.e")
        check("0.0e")

        check("1.1.")
        check(".001.")
        check("..1")
        check("0..0")
    }

    @Test
    fun dgdfg() {
        val n = 1.7976931348623157E308
        setText(n.toString())
        assertNearlyEquals(n, reader.readDouble())
    }

    @Test
    fun doubles() {
        for (n in NumbersDataSet.floats64.filter { it.isFinite() }) {
            enrichMessageOf<Throwable>({ "failed on: $n" }) {
                setText(n.toString())
                assertNearlyEquals(n, reader.readDouble())

                setText((-n).toString())
                assertNearlyEquals(-n, reader.readDouble())

                setText(n.toString())
                checkNumber(n)

                setText((-n).toString())
                checkNumber(-n)
            }
        }
    }

    private fun checkNumber(n: Long) {
        var ok = false
        reader.readNumberTemplate(
            acceptInt = { assertEquals(n, it); ok = true },
            acceptDouble = { fail() }
        )
        assertTrue(ok, "acceptInt not called")
    }

    private fun checkNumber(n: Double) {
        var ok = false
        reader.readNumberTemplate(
            acceptInt = { fail() },
            acceptDouble = { assertNearlyEquals(n, it.doubleValue()); ok = true }
        )
        assertTrue(ok, "acceptFloat not called")
    }

    private fun assertNearlyEquals(expected: Double, actual: Double) {
        if (!expected.nearlyEquals(actual)) {
            fail("expected: $expected, actual: $actual")
        }
    }

    private fun assertNearlyEquals(expected: Float, actual: Float) {
        if (!expected.nearlyEquals(actual)) {
            fail("expected: $expected, actual: $actual")
        }
    }

    @Test
    fun read_string_until_ending() {
        for (s in StringsDataSet.getUtfData(excludeChar = '|')) {
            setText("$s|")
            assertEquals(s, reader.readStringUntil(ending = '|'))
        }
    }

    @Test
    fun read_string_sized1() {
        setText("Привет, Мир!")
        assertEquals("Привет", reader.readStringSized(length = "Привет".length))
        assertEquals(',', reader.readCodePoint().toChar())
        assertEquals(' ', reader.readCodePoint().toChar())
        assertEquals("Мир!", reader.readStringSized(length = "Мир!".length))
    }

    @Test
    fun utf_codepoints() {
        setText("a1ф2Ꙁ3")
        assertEquals('a', reader.readCodePoint().toChar())
        reader.expect('1')
        assertEquals('ф', reader.readCodePoint().toChar())
        reader.expect('2')
        assertEquals('Ꙁ', reader.readCodePoint().toChar())
        reader.expect('3')
        reader.expectEof()

        val text = "\u0A12\u1A13\uAA34\ufA56\uAA34\u1A13\u0A12"
        setText(text)
        for (c in text) {
            assertEquals(c, reader.readCodePoint().toChar())
        }
        reader.expectEof()

        for (c in text) {
            setText(c.toString())
            assertEquals(c, reader.readCodePoint().toChar())
            reader.expectEof()
        }
    }

    @Test
    fun utf_surrogates() {
        StringsDataSet.getSurrogatePairs(100_000).forEach { chars ->
            val expected = StringsUTF16.codePoint(chars[0].code, chars[1].code)
            setText(chars + "Ё")
            assertEquals(expected, reader.readCodePoint())
            reader.expect('Ё')
        }
    }

    @Test
    fun read_string_sized2() {
        for (s in StringsDataSet.getUtfData()) {
            setText(s)
            assertEquals(s, reader.readStringSized(length = s.length))
        }
    }

    @Test
    fun sub_string() {
        setText("Привет, Мир!")
        val ss = reader.readSubStringInline { it != ',' }
        assertEquals("Привет".hashCode() + 1, ss.hashCode())
        assertEquals("Привет", ss.toString())
        reader.expect(',')
        reader.skipWhitespaceStrict()
        assertEquals("Мир!", reader.readSubStringInline { true }.toString())
    }

    @Test
    fun failure_handler() {
        assertFailsWith<TextDecodingException> { reader.fail("zzz") }
    }
}