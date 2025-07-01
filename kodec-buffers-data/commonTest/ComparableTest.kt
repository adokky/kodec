package io.kodec.buffers

import kotlin.math.sign
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ComparableTest {
    @Test
    fun equality() {
        ArrayBufferPairs.equalBufferPairs().forEach {
            assertEquals(0, it.first.compareTo(it.second))
            assertEquals(0, it.second.compareTo(it.first))
        }

        ArrayBufferPairs.notEqualBufferPairs().forEach {
            assertNotEquals(0, it.first.compareTo(it.second))
            assertNotEquals(0, it.second.compareTo(it.first))
        }
    }

    private fun test(min: ArrayDataBuffer, max: ArrayDataBuffer) {
        assertEquals(-1, min.compareTo(max).sign)
        assertEquals(1, max.compareTo(min).sign)
    }

    @Test
    fun non_equal() {
        test(dataBufferOf(), dataBufferOf(1))
        test(dataBufferOf(1), dataBufferOf(1, 1))
        test(dataBufferOf(0, 1), dataBufferOf(1))
        test(dataBufferOf(0, 1), dataBufferOf(1))
        test(dataBufferOf(0), dataBufferOf(Byte.MIN_VALUE))
    }
}