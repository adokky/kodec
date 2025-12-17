package io.kodec.java

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class ByteBufferAsBufferTest {
    @Test
    fun test_as_buffer_with_array() {
        val array = byteArrayOf(1, 2, 3, 4, 5)
        val buffer = ByteBuffer.wrap(array)

        val result = buffer.asBuffer()

        assertArrayEquals(array, result.toByteArray(0, 5))
    }

    @Test
    fun test_as_buffer_with_range() {
        val array = byteArrayOf(1, 2, 3, 4, 5)
        val buffer = ByteBuffer.wrap(array)

        val result = buffer.asBuffer(1, 4)

        assertArrayEquals(byteArrayOf(2, 3, 4), result.toByteArray(0, 3))
    }

    @Test
    fun test_as_buffer_with_position_and_limit() {
        val array = byteArrayOf(1, 2, 3, 4, 5)
        val buffer = ByteBuffer.wrap(array)
        buffer.position(1)
        buffer.limit(4)

        val result = buffer.asBuffer()

        assertArrayEquals(byteArrayOf(2, 3, 4), result.toByteArray(0, 3))
    }

    @Test
    fun test_as_buffer_with_position_and_limit_and_range() {
        val array = byteArrayOf(1, 2, 3, 4, 5)
        val buffer = ByteBuffer.wrap(array)
        buffer.position(1)
        buffer.limit(4)

        val result = buffer.asBuffer(1, 3)

        assertArrayEquals(byteArrayOf(2, 3), result.toByteArray(0, 2))
    }

    @Test
    fun test_as_buffer_with_direct_buffer() {
        val buffer = ByteBuffer.allocateDirect(5)
        buffer.put(0, 1.toByte())
        buffer.put(1, 2.toByte())
        buffer.put(2, 3.toByte())
        buffer.put(3, 4.toByte())
        buffer.put(4, 5.toByte())
        buffer.position(0)

        val result = buffer.asBuffer()

        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5), result.toByteArray(0, 5))
    }
}
