package io.kodec.text

import io.kodec.StringsDataSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SimpleSubStringTest {
    private val ss = SimpleSubString()

    @Test
    fun simple() {
        for (s in StringsDataSet.getUtfData()) {
            assertEquals(s, s.substringWrapper().toString())
            ss.set(s)
            assertEquals(s, ss.toString())
        }
    }

    @Test
    fun wrong_start() {
        assertFailsWith<IllegalArgumentException> {
            "012".substringWrapper(start = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            "012".substringWrapper(start = 4)
        }
        assertFailsWith<IllegalArgumentException> {
            ss.set("012", start = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            ss.set("012", start = 4)
        }
    }

    @Test
    fun wrong_end() {
        assertFailsWith<IllegalArgumentException> {
            "012".substringWrapper(end = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            "012".substringWrapper(end = 4)
        }
        assertFailsWith<IllegalArgumentException> {
            "012".substringWrapper(start = 2, end = 1)
        }
        assertFailsWith<IllegalArgumentException> {
            ss.set("012", end = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            ss.set("012", end = 4)
        }
        assertFailsWith<IllegalArgumentException> {
            ss.set("012", start = 2, end = 1)
        }
    }

    @Test
    fun numbers() {
        assertEquals(-123, "-123".substringWrapper().toInt())
        assertEquals(-123456789012L, "-123456789012".substringWrapper().toLong())
        assertEquals(0.12f, "0.12".substringWrapper().toFloat(), absoluteTolerance = 0.001f)
        assertEquals(0.12, "0.12".substringWrapper().toDouble(), absoluteTolerance = 0.00001)
    }
}