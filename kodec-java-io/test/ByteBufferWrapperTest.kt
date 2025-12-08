package io.kodec.java

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class ByteBufferWrapperTest {
    @Test
    fun testGetSet() {
        val buffer = ByteBuffer.allocate(10)
        val wrapper = ByteBufferWrapper(buffer)
        
        wrapper[0] = 0x42
        assertEquals(0x42, wrapper[0])
        
        wrapper[1] = 0xFF
        assertEquals(0xFF, wrapper[1])
    }

    @Test
    fun testGetByte() {
        val buffer = ByteBuffer.allocate(5)
        buffer.put(0, 0x42.toByte())
        buffer.put(1, 0xFF.toByte())
        val wrapper = ByteBufferWrapper(buffer)
        
        assertEquals(0x42.toByte(), wrapper.getByte(0))
        assertEquals(0xFF.toByte(), wrapper.getByte(1))
    }

    @Test
    fun testSize() {
        val buffer = ByteBuffer.allocate(10)
        val wrapper = ByteBufferWrapper(buffer)
        
        assertEquals(10, wrapper.size)
    }

    @Test
    fun testSubBuffer() {
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
    fun testFill() {
        val buffer = ByteBuffer.allocate(5)
        val wrapper = ByteBufferWrapper(buffer)
        
        wrapper.fill(0xAA, 1, 4)
        
        assertEquals(0x00, wrapper[0]) // unchanged
        assertEquals(0xAA, wrapper[1])
        assertEquals(0xAA, wrapper[2])
        assertEquals(0xAA, wrapper[3])
        assertEquals(0x00, wrapper[4]) // unchanged
    }

    @Test
    fun testToByteArray() {
        val buffer = ByteBuffer.allocate(5)
        buffer.put(0, 0x01.toByte())
        buffer.put(1, 0x02.toByte())
        buffer.put(2, 0x03.toByte())
        val wrapper = ByteBufferWrapper(buffer)
        
        val result = wrapper.toByteArray(1, 3)
        assertArrayEquals(byteArrayOf(0x02.toByte(), 0x03.toByte()), result)
    }

    @Test
    fun testPutBytes() {
        val buffer = ByteBuffer.allocate(10)
        val wrapper = ByteBufferWrapper(buffer)
        val bytes = byteArrayOf(0x01.toByte(), 0x02.toByte(), 0x03.toByte())
        
        wrapper.putBytes(2, bytes, 0, 3)
        
        assertEquals(0x01, wrapper[2])
        assertEquals(0x02, wrapper[3])
        assertEquals(0x03, wrapper[4])
    }
}
