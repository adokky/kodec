package io.kodec.text

import io.kodec.StringsDataSet
import karamel.utils.enrichMessageOf
import karamel.utils.replaceCharAt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

abstract class AbstractSubStringTest<S: AbstractSubString> {
    protected abstract fun substring(source: String, start: Int = 0, end: Int = source.length): S
    protected abstract fun S.resetData(source: String, start: Int = 0, end: Int = source.length)

    @Test
    fun for_each() {
        fun AbstractSubString.string() = buildString {
            this@string.forEach { append(it) }
        }

        testSubstrings {
            assertEquals(expected, subString.string())
            subString.resetData(source, start, end)
            assertEquals(expected, subString.toString()) // init cachedString
            assertEquals(expected, subString.string())
        }
    }

    @Test
    fun numbers() {
        assertEquals(-123, substring("-123").toInt())
        assertEquals((-123).toShort(), substring("-123").toShort())
        assertEquals((-123).toByte(), substring("-123").toByte())
        assertEquals(-123456789012L, substring("-123456789012").toLong())
        assertEquals(0.12f, substring("0.12").toFloat(), absoluteTolerance = 0.001f)
        assertEquals(0.12, substring("0.12").toDouble(), absoluteTolerance = 0.00001)

        assertFailsWith<IllegalArgumentException> { substring("1NotANumber2").toByte() }
        assertFailsWith<IllegalArgumentException> { substring("1NotANumber2").toShort() }
        assertFailsWith<IllegalArgumentException> { substring("1NotANumber2").toInt() }
        assertFailsWith<IllegalArgumentException> { substring("1NotANumber2").toFloat() }
        assertFailsWith<IllegalArgumentException> { substring("1NotANumber2").toDouble() }
    }

    @Test
    fun bool() {
        assertTrue(substring("true").toBoolean())
        assertFalse(substring("false").toBoolean())
        assertTrue(substring("TRUE").toBoolean())
        assertFalse(substring("FALSE").toBoolean())
        assertFailsWith<IllegalArgumentException> { substring("false_").toBoolean() }
    }

    @Test
    fun copy_test() {
        testSubstrings {
            val copy = subString.copy()

            assertNotSame(copy, subString)
            assertEquals(copy, subString)
            assertEquals(expected, subString.toString())
            assertEquals(expected, copy.toString())
        }
    }

    @Test
    fun hash_code() {
        testSubstrings {
            assertEquals(expected.hashCode() + 1, subString.hashCode())
        }
    }

    @Test
    fun equality() {
        testSubstrings {
            assertEquals(subString, subString)
            assertEquals(subString.copy(), subString)
            assertEquals(subString, subString.copy())

            if (subString.sourceLength > 0) {
                val ss2 = substring(source.replaceCharAt(start, source[start] + 1), start, end)
                assertNotEquals(ss2, subString)
                assertNotEquals(subString, ss2)

                ss2.resetData(source, start, end)
                assertEquals(ss2, subString)
            }
        }
    }

    protected data class SubStringData<S: AbstractSubString>(
        val source: String,
        val expected: String,
        val subString: S,
        val start: Int,
        val end: Int
    )

    protected fun testSubstrings(body: SubStringData<S>.() -> Unit) {
        for (s in StringsDataSet.getUtfData())
        for (start in 0..2)
        for (end in 0..2) {
            var start = start.coerceAtMost(s.length)
            var end = (s.length - end).coerceIn(start, s.length)

            // prevent surrogate pair corruption
            if (s.getOrElse(start  ) { '-' }.isLowSurrogate()) {
                start = (start - 1).coerceAtLeast(0)
            }
            if (s.getOrElse(end - 1) { '-' }.isHighSurrogate()) {
                end = (end + 1).coerceAtMost(s.length)
            }

            val expected = s.substring(start, end)
            val ss = substring(s, start, end)

            val data = SubStringData(source = s, expected = expected, subString = ss, start = start, end = end)
            enrichMessageOf<Throwable>({ data.toString() }) { body(data) }
        }
    }
}

