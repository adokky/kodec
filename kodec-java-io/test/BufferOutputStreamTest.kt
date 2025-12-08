package io.kodec.java

import io.kodec.buffers.asArrayBuffer
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test

class BufferOutputStreamTest {
    @Test
    fun testWriteInt() {
        val buffer = ByteArray(5)
        val outputStream = BufferOutputStream(buffer.asArrayBuffer(), 0)

        outputStream.write(0x42)
        assertArrayEquals(byteArrayOf(0x42.toByte()), buffer.sliceArray(0..0))
    }

    @Test
    fun testWriteByteArray() {
        val buffer = ByteArray(10)
        val outputStream = BufferOutputStream(buffer.asArrayBuffer(), 2)
        val data = byteArrayOf(0x01, 0x02, 0x03)

        outputStream.write(data, 0, 3)

        assertArrayEquals(
            byteArrayOf(0x00, 0x00, 0x01, 0x02, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00),
            buffer
        )
    }
}
