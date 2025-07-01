package io.kodec.buffers

import kotlin.test.*

class SubBufferTest {
    private val array = ByteArray(10)
    private val safeBufferFactories = listOf(::ArrayDataBufferSafeLE, ::ArrayDataBufferSafeBE)

    private fun testBuffer(buffer: ArrayDataBuffer, bufferStart: Int, bufferEnd: Int) {
        array.fill(0)

        buffer[0] = 42
        assertEquals(42, array[bufferStart])

        buffer.putInt16(bufferEnd - bufferStart - 2, 0x7F_FF)
        assertNotEquals(0, array[bufferEnd - 2])
        assertNotEquals(0, array[bufferEnd - 1])

        buffer[buffer.indices.last] = 56
        assertEquals(56, array[bufferEnd - 1])

        val bytes = ByteArray(buffer.size) { 71 }
        buffer.putBytes(0, bytes)
        fun checkArrayIntegrity() {
            assertTrue(array.take(bufferStart).all { it == 0.toByte() })
            assertTrue(array.takeLast(array.size - bufferEnd).all { it == 0.toByte() })
            assertTrue(bytes.contentEquals(array.copyOfRange(bufferStart, bufferEnd)))
        }
        checkArrayIntegrity()

        assertFailsWith<IndexOutOfBoundsException> { buffer[-1] = 0 }
        assertFailsWith<IndexOutOfBoundsException> { buffer.putInt16(-1, 0) }

        assertFailsWith<IndexOutOfBoundsException> { buffer[-1] }
        assertFailsWith<IndexOutOfBoundsException> { buffer[buffer.size] }

        assertFailsWith<IndexOutOfBoundsException> { buffer[buffer.size] = 0xff }
        assertFailsWith<IndexOutOfBoundsException> { buffer.putInt16(buffer.size - 1, Short.MIN_VALUE) }
        assertFailsWith<IndexOutOfBoundsException> { buffer.putInt24(buffer.size - 2, 0xff_ff_ff) }
        assertFailsWith<IndexOutOfBoundsException> { buffer.putInt32(buffer.size - 3, Int.MIN_VALUE) }
        assertFailsWith<IndexOutOfBoundsException> { buffer.putInt48(buffer.size - 5, 0xff_ff_ff_ff_ff) }
        assertFailsWith<IndexOutOfBoundsException> { buffer.putInt64(buffer.size - 7, Long.MIN_VALUE) }

        checkArrayIntegrity()
    }

    @Test
    fun nonWrappedBoundsChecking() {
        for (Buffer in safeBufferFactories) {
            val bufferStart = 1
            val bufferEnd = 9

            val buffer = Buffer(array, bufferStart, bufferEnd)

            testBuffer(buffer, bufferStart, bufferEnd)
        }
    }

    @Test
    fun wrapperTest() {
        for (Buffer in safeBufferFactories) {
            val parentBuffer = Buffer(array, 1, 9)

            assertFailsWith<IllegalArgumentException> { parentBuffer.subBuffer(2, 1) }
            assertFailsWith<IllegalArgumentException> { parentBuffer.subBuffer(-1, -1) }
            assertFailsWith<IllegalArgumentException> { parentBuffer.subBuffer(-1, 2) }
            assertFailsWith<IllegalArgumentException> { parentBuffer.subBuffer(1, -1) }
            assertFailsWith<IllegalArgumentException> { parentBuffer.subBuffer(1, 9) }

            testBuffer(
                parentBuffer.subBuffer(1, 7),
                bufferStart = parentBuffer.start + 1,
                bufferEnd = parentBuffer.start + 7
            )
        }
    }
}