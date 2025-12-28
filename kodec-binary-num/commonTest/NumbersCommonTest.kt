package io.kodec

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NumbersCommonTest {
    @Test
    fun int24() {
        assertTrue(NumbersCommon.INT_24_MIN < 0)
        assertEquals(0.inv(),        NumbersCommon.INT_24_MIN shr 23)
        assertEquals(0.inv().shl(1), NumbersCommon.INT_24_MIN shr 22)

        assertTrue(NumbersCommon.INT_24_MIN < NumbersCommon.INT_24_MAX)
        assertTrue(NumbersCommon.INT_24_MIN == NumbersCommon.INT_24_MAX.inv())
    }

    private fun testLong(min: Long, max: Long, bits: Int) {
        assertTrue(min < 0)
        assertEquals(0L.inv(),        min shr (bits - 1))
        assertEquals(0L.inv().shl(1), min shr (bits - 2))

        assertTrue(min < max)
        assertTrue(min == max.inv())
    }

    @Test
    fun int40() {
        testLong(NumbersCommon.INT_40_MIN, NumbersCommon.INT_40_MAX, 40)
    }

    @Test
    fun int48() {
        testLong(NumbersCommon.INT_48_MIN, NumbersCommon.INT_48_MAX, 48)
    }

    @Test
    fun int56() {
        testLong(NumbersCommon.INT_56_MIN, NumbersCommon.INT_56_MAX, 56)
    }
}

fun Long.bitString(): String {
    return buildString(64) {
        for (i in 63 downTo 0) {
            if (i != 63 && (i + 1) % 8 == 0) append(' ')
            append(this@bitString.ushr(i).and(1))
        }
    }
}

fun Int.bitString(): String {
    return buildString(32) {
        for (i in 31 downTo 0) {
            if (i != 31 && (i + 1) % 8 == 0) append(' ')
            append(this@bitString.ushr(i).and(1))
        }
    }
}