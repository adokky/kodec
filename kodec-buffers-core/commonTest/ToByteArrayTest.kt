package io.kodec.buffers

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class ToByteArrayTest: TestBase() {
    @Test
    fun whole_buffer() = test {
        val buf = buffer(start = 1, end = 6) // 22, 33, 44, 55, 66
        assertContentEquals(byteArrayOf(22, 33, 44, 55, 66), buf.toByteArray())
    }

    @Test
    fun only_end() = test {
        val buf = buffer(start = 1, end = 6) // 22, 33, 44, 55, 66
        assertContentEquals(byteArrayOf(22, 33, 44), buf.toByteArray(endExclusive = 3))
    }

    @Test
    fun only_start() = test {
        val buf = buffer(start = 1, end = 6) // 22, 33, 44, 55, 66
        assertContentEquals(byteArrayOf(55, 66), buf.toByteArray(start = 3))
    }

    @Test
    fun custom_range() = test {
        val buf = buffer(start = 1, end = 6) // 22, 33, 44, 55, 66
        val arr = buf.toByteArray(start = 2, endExclusive = 4)
        assertContentEquals(byteArrayOf(44, 55), arr)
    }

    @Test
    fun to_empty_byte_array() = test {
        val buf = buffer(start = 1, end = 6)
        val arr = buf.toByteArray(start = 2, endExclusive = 2)
        assertContentEquals(byteArrayOf(), arr)
    }

    @Test
    fun invalid_ranges() = test {
        val buf = testArray().asArrayBuffer(start = 1, endExclusive = 6)
        assertFailsWith<IllegalArgumentException> { buf.toByteArray(start = 5, endExclusive = 6) }
        assertFailsWith<IllegalArgumentException> { buf.toByteArray(start = -2, endExclusive = -1) }
        assertFailsWith<IllegalArgumentException> { buf.toByteArray(endExclusive = 6) }
        assertFailsWith<IllegalArgumentException> { buf.toByteArray(start = 2, endExclusive = 1) }
        assertFailsWith<IllegalArgumentException> { buf.toByteArray(endExclusive = -1) }
    }
}