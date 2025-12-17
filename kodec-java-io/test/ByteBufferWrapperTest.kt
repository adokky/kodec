package io.kodec.java

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import kotlin.test.assertFailsWith

class ByteBufferWrapperTest {
    private fun createTestBuffer(): ByteBuffer {
        val buffer = ByteBuffer.allocate(10)
        for (i in 0 ..< 10) {
            buffer.put(i, (i % 256).toByte())
        }
        return buffer
    }

    private fun createTestBytes(): ByteArray {
        return (0..2).map { (it % 256).toByte() }.toByteArray()
    }

    @Test
    fun test_invalid_range_validation() {
        val buffer = ByteBuffer.allocate(5)

        assertFailsWith<IllegalArgumentException> {
            ByteBufferWrapper(buffer, 2, 10) // endExclusive > buffer.limit()
        }
        assertFailsWith<IllegalArgumentException> {
            ByteBufferWrapper(buffer, -1, 3) // start < 0
        }
        assertFailsWith<IllegalArgumentException> {
            ByteBufferWrapper(buffer, 3, 2) // start >= endExclusive
        }
    }

    @Test
    fun test_reset() {
        val buffer1 = ByteBuffer.allocate(10)
        val buffer2 = ByteBuffer.allocate(5)

        val wrapper = ByteBufferWrapper(buffer1, 2, 8)

        // Fill the original buffer
        wrapper[0] = 0x42
        wrapper[1] = 0xFF

        // Reset to a different buffer with a different range
        wrapper.reset(buffer2, 1, 4)

        // Check that we're now using the new buffer
        wrapper[0] = 0xAA
        wrapper[1] = 0xBB

        // Verify values were written to the correct positions
        assertEquals(0xAA, wrapper[0])
        assertEquals(0xBB, wrapper[1])

        // Verify the original buffer was not affected (at the correct offset)
        assertEquals(0x42.toByte(), buffer1.get(2)) // offset 2 in buffer1
        assertEquals(0xFF.toByte(), buffer1.get(3)) // offset 3 in buffer1

        // Verify the new buffer was modified
        assertEquals(0xAA.toByte(), buffer2.get(1))
        assertEquals(0xBB.toByte(), buffer2.get(2))
    }

    @Test
    fun test_get_set() {
        val buffer = ByteBuffer.allocate(10)
        val wrapper = ByteBufferWrapper(buffer)

        wrapper[0] = 0x42
        assertEquals(0x42, wrapper[0])

        wrapper[1] = 0xFF
        assertEquals(0xFF, wrapper[1])
    }

    @Test
    fun test_get_set_with_custom_range() {
        val buffer = ByteBuffer.allocate(10)
        val wrapper = ByteBufferWrapper(buffer, 2, 8)

        wrapper[0] = 0x42
        assertEquals(0x42, wrapper[0])

        wrapper[1] = 0xFF
        assertEquals(0xFF, wrapper[1])

        assertEquals(0x42.toByte(), buffer.get(2))
        assertEquals(0xFF.toByte(), buffer.get(3))
    }

    @Test
    fun test_get_byte() {
        val buffer = ByteBuffer.allocate(5)
        buffer.put(0, 0x42.toByte())
        buffer.put(1, 0xFF.toByte())
        val wrapper = ByteBufferWrapper(buffer)

        assertEquals(0x42.toByte(), wrapper.getByte(0))
        assertEquals(0xFF.toByte(), wrapper.getByte(1))
    }

    @Test
    fun test_get_byte_with_custom_range() {
        val buffer = ByteBuffer.allocate(10)
        buffer.put(2, 0x42.toByte())
        buffer.put(3, 0xFF.toByte())
        val wrapper = ByteBufferWrapper(buffer, 2, 6)

        assertEquals(0x42.toByte(), wrapper.getByte(0))
        assertEquals(0xFF.toByte(), wrapper.getByte(1))
    }

    @Test
    fun test_size() {
        val buffer = ByteBuffer.allocate(10)
        val wrapper = ByteBufferWrapper(buffer)

        assertEquals(10, wrapper.size)
    }

    @Test
    fun test_size_with_custom_range() {
        val buffer = ByteBuffer.allocate(10)
        val wrapper = ByteBufferWrapper(buffer, 2, 8)

        assertEquals(6, wrapper.size)
    }

    @Test
    fun test_sub_buffer() {
        val buffer = ByteBuffer.allocate(10)
        buffer.put(0, 0x01.toByte())
        buffer.put(1, 0x02.toByte())
        buffer.put(2, 0x03.toByte())
        val wrapper = ByteBufferWrapper(buffer)

        val subBuffer = wrapper.subBuffer(1, 3)
        assertEquals(0x02, subBuffer[0])
        assertEquals(0x03, subBuffer[1])
    }

    @Test
    fun test_sub_buffer_with_custom_range() {
        val buffer = ByteBuffer.allocate(10)
        buffer.put(2, 0x01.toByte())
        buffer.put(3, 0x02.toByte())
        buffer.put(4, 0x03.toByte())
        val wrapper = ByteBufferWrapper(buffer, 2, 7)

        val subBuffer = wrapper.subBuffer(1, 3)
        assertEquals(0x02, subBuffer[0])
        assertEquals(0x03, subBuffer[1])
    }

    @Test
    fun test_fill() {
        val buffer = ByteBuffer.allocate(5)
        val wrapper = ByteBufferWrapper(buffer)

        wrapper.fill(0xAA, 1, 4)

        assertEquals(0x00, wrapper[0])
        assertEquals(0xAA, wrapper[1])
        assertEquals(0xAA, wrapper[2])
        assertEquals(0xAA, wrapper[3])
        assertEquals(0x00, wrapper[4])
    }

    @Test
    fun test_fill_with_custom_range() {
        val buffer = ByteBuffer.allocate(10)
        val wrapper = ByteBufferWrapper(buffer, 2, 8)

        wrapper.fill(0xAA, 1, 4)

        assertEquals(0x00, wrapper[0])
        assertEquals(0xAA, wrapper[1])
        assertEquals(0xAA, wrapper[2])
        assertEquals(0xAA, wrapper[3])
        assertEquals(0x00, wrapper[4])

        assertEquals(0xAA.toByte(), buffer.get(3))
        assertEquals(0xAA.toByte(), buffer.get(4))
        assertEquals(0xAA.toByte(), buffer.get(5))
    }

    @Test
    fun test_to_byte_array() {
        val buffer = ByteBuffer.allocate(5)
        buffer.put(0, 0x01.toByte())
        buffer.put(1, 0x02.toByte())
        buffer.put(2, 0x03.toByte())
        val wrapper = ByteBufferWrapper(buffer)

        val result = wrapper.toByteArray(1, 3)
        assertArrayEquals(byteArrayOf(0x02.toByte(), 0x03.toByte()), result)
    }

    @Test
    fun test_to_byte_array_with_custom_range() {
        val buffer = ByteBuffer.allocate(10)
        buffer.put(2, 0x01.toByte())
        buffer.put(3, 0x02.toByte())
        buffer.put(4, 0x03.toByte())
        val wrapper = ByteBufferWrapper(buffer, 2, 7)

        val result = wrapper.toByteArray(1, 3)
        assertArrayEquals(byteArrayOf(0x02.toByte(), 0x03.toByte()), result)
    }

    @Test
    fun test_put_bytes() {
        val buffer = ByteBuffer.allocate(10)
        val wrapper = ByteBufferWrapper(buffer)
        val bytes = byteArrayOf(0x01.toByte(), 0x02.toByte(), 0x03.toByte())

        wrapper.putBytes(2, bytes, 0, 3)

        assertEquals(0x01, wrapper[2])
        assertEquals(0x02, wrapper[3])
        assertEquals(0x03, wrapper[4])
    }

    @Test
    fun test_put_bytes_with_custom_range() {
        val buffer = ByteBuffer.allocate(10)
        val wrapper = ByteBufferWrapper(buffer, 2, 8)
        val bytes = byteArrayOf(0x01.toByte(), 0x02.toByte(), 0x03.toByte())

        wrapper.putBytes(2, bytes, 0, 3)

        assertEquals(0x01, wrapper[2])
        assertEquals(0x02, wrapper[3])
        assertEquals(0x03, wrapper[4])

        assertEquals(0x01.toByte(), buffer.get(4))
        assertEquals(0x02.toByte(), buffer.get(5))
        assertEquals(0x03.toByte(), buffer.get(6))
    }

    @Test
    fun test_original_byte_array_not_modified_outside_range() {
        val buffer = createTestBuffer()
        val originalBytes = buffer.array().clone()

        val wrapper = ByteBufferWrapper(buffer, 2, 8)

        // Modify data within wrapper range
        wrapper[0] = 0x42
        wrapper[1] = 0xFF

        // Verify that only the bytes within the wrapper range have changed
        val expectedBytes = originalBytes.clone()
        expectedBytes[2] = 0x42.toByte()
        expectedBytes[3] = 0xFF.toByte()

        assertArrayEquals(expectedBytes, buffer.array())
    }

    @Test
    fun test_original_byte_array_not_modified_outside_range_with_fill() {
        val buffer = createTestBuffer()
        val originalBytes = buffer.array().clone()

        val wrapper = ByteBufferWrapper(buffer, 2, 8)

        // Fill range
        wrapper.fill(0xAA, 1, 4)

        // Verify that only the bytes within the wrapper range have changed
        val expectedBytes = originalBytes.clone()
        expectedBytes[3] = 0xAA.toByte()
        expectedBytes[4] = 0xAA.toByte()
        expectedBytes[5] = 0xAA.toByte()

        assertArrayEquals(expectedBytes, buffer.array())
    }

    @Test
    fun test_original_byte_array_not_modified_outside_range_with_put_bytes() {
        val buffer = createTestBuffer()
        val originalBytes = buffer.array().clone()

        val wrapper = ByteBufferWrapper(buffer, 2, 8)
        val bytes = createTestBytes()

        // Put bytes
        wrapper.putBytes(2, bytes, 0, 3)

        // Verify that only the bytes within the wrapper range have changed
        val expectedBytes = originalBytes.clone()
        expectedBytes[4] = bytes[0]
        expectedBytes[5] = bytes[1]
        expectedBytes[6] = bytes[2]

        assertArrayEquals(expectedBytes, buffer.array())
    }
}
