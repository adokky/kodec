package io.kodec.buffers

import dev.adokky.eqtester.testEquality
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ArrayBufferTest {
    private fun testArray(): ByteArray = (1..9).map { (it * 11).toByte() }.toByteArray()

    @Test
    fun basic_mutation() {
        val buf = testArray().asBuffer(2, 7)
        assertEquals(bufferOf(33, 44, 55, 66, 77), buf)
        buf[1] = 100
        assertEquals(bufferOf(33, 100, 55, 66, 77), buf)
    }

    @Test
    fun equality() {
        testEquality {
            group { testArray().asBuffer() }
            group { byteArrayOf(-111, 0, 0, 3).asBuffer() }
            group { Buffer.Empty }
            group { testArray().asBuffer(2, 7) }
            group { testArray().asBuffer(2, 8) }
            group { testArray().asBuffer(3, 7) }
        }
    }

    @Test
    fun basic_properties() {
        val buf = ArrayBuffer(6)

        assertContentEquals(ByteArray(6), buf.array)
        assertEquals(6, buf.size)
        assertEquals(0, buf.start)
        assertEquals(6, buf.endExclusive)

        buf.putBytes(0, bufferOf(1, 2, 3))
        assertEquals(6, buf.size)
        assertEquals(0, buf.start)
        assertEquals(6, buf.endExclusive)

        buf.setArray(byteArrayOf(11, 22, 33), start = 1)
        assertContentEquals(byteArrayOf(11, 22, 33), buf.array)
        assertEquals(2, buf.size)
        assertEquals(1, buf.start)
        assertEquals(3, buf.endExclusive)

        buf.setArray(byteArrayOf(3, 4, 6), start = 1, endExclusive = 2)
        assertContentEquals(byteArrayOf(3, 4, 6), buf.array)
        assertEquals(1, buf.size)
        assertEquals(1, buf.start)
        assertEquals(2, buf.endExclusive)
    }

    @Test
    fun put_bytes() {
        val buf = ArrayBuffer(6)

        buf.putBytes(0, bufferOf())
        assertEquals(bufferOf(0, 0, 0, 0, 0, 0), buf)

        buf.putBytes(2, bufferOf(5, 67, 91), startIndex = 2, endIndex = 3)
        assertEquals(bufferOf(0, 0, 91, 0, 0, 0), buf)

        buf.putBytes(3, bufferOf(5, 67, 91))
        assertEquals(bufferOf(0, 0, 91, 5, 67, 91), buf)

        val array = testArray()
        buf.setArray(array, start = 2, endExclusive = 7)
        buf.putBytes(2, bufferOf(1, 2, 3), startIndex = 1, endIndex = 2)
        assertEquals(bufferOf(33, 44, 2, 66, 77), buf)
    }

    @Test
    fun clear_range() {
        val buf = testArray().asBuffer(start = 1, endExclusive = 6)

        buf.clear(3, 4)
        assertEquals(bufferOf(22, 33, 44, 0, 66), buf)
        buf.clear(3)
        assertEquals(bufferOf(22, 33, 44, 0, 0), buf)
        buf.clear()
        assertEquals(ArrayBuffer(5), buf)
    }
}