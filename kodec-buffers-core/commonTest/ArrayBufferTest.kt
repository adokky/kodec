package io.kodec.buffers

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ArrayBufferTest {
    private fun testArray(): ByteArray = (1..9).map { (it * 11).toByte() }.toByteArray()

    @Test
    fun basic_properties() {
        val buf = ArrayBuffer(6)

        assertContentEquals(ByteArray(6), buf.array)
        assertEquals(6, buf.size)
        assertEquals(0, buf.start)
        assertEquals(6, buf.endExclusive)

        buf.putBytes(0, mutableBufferOf(1, 2, 3))
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
}