package io.kodec.buffers

import kotlin.test.Test
import kotlin.test.assertEquals

class BasicOperationsTest: TestBase() {
    @Test
    fun basic_mutation() = test {
        val buf = buffer(2, 7)
        assertEquals(mutableBufferOf(33, 44, 55, 66, 77), buf)
        buf[1] = 100
        assertEquals(mutableBufferOf(33, 100, 55, 66, 77), buf)
    }

    @Test
    fun put_bytes() = test {
        var buf = buffer(ByteArray(6))

        buf.putBytes(0, mutableBufferOf())
        assertEquals(mutableBufferOf(0, 0, 0, 0, 0, 0), buf)

        buf.putBytes(2, mutableBufferOf(5, 67, 91), startIndex = 2, endIndex = 3)
        assertEquals(mutableBufferOf(0, 0, 91, 0, 0, 0), buf)

        buf.putBytes(3, mutableBufferOf(5, 67, 91))
        assertEquals(mutableBufferOf(0, 0, 91, 5, 67, 91), buf)

        buf = buffer(start = 2, end = 7)
        buf.putBytes(2, mutableBufferOf(1, 2, 3), startIndex = 1, endIndex = 2)
        assertEquals(mutableBufferOf(33, 44, 2, 66, 77), buf)
    }

    @Test
    fun clear_range() {
        val buf = testArray().asArrayBuffer(start = 1, endExclusive = 6)

        buf.clear(3, 4)
        assertEquals(mutableBufferOf(22, 33, 44, 0, 66), buf)
        buf.clear(3)
        assertEquals(mutableBufferOf(22, 33, 44, 0, 0), buf)
        buf.clear()
        assertEquals(ArrayBuffer(5), buf)
    }
}