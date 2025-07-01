package io.kodec.buffers

import dev.adokky.eqtester.testEquality
import kotlin.test.Test
import kotlin.test.assertEquals

class GenericBufferTest {
    private fun testArray(): ByteArray = (1..9).map { (it * 11).toByte() }.toByteArray()

    @Test
    fun equality() {
        testEquality {
            group { CustomBuffer(testArray()) }
            group { CustomBuffer(byteArrayOf(-111, 0, 0, 3)) }
            group { CustomBuffer(byteArrayOf()) }
            group { CustomBuffer(testArray(), 2, 7) }
            group { CustomBuffer(testArray(), 2, 8) }
            group { CustomBuffer(testArray(), 3, 7) }
        }
    }

    @Test
    fun put_bytes() {
        var buf: MutableBuffer = CustomBuffer(ArrayBuffer(6))

        buf.putBytes(0, bufferOf())
        assertEquals(bufferOf(0, 0, 0, 0, 0, 0), buf)

        buf.putBytes(2, bufferOf(5, 67, 91), startIndex = 2, endIndex = 3)
        assertEquals(bufferOf(0, 0, 91, 0, 0, 0), buf)

        buf.putBytes(3, bufferOf(5, 67, 91))
        assertEquals(bufferOf(0, 0, 91, 5, 67, 91), buf)

        buf = CustomBuffer(testArray(), 2, 7) as MutableBuffer
        buf.putBytes(2, bufferOf(1, 2, 3), startIndex = 1, endIndex = 2)
        assertEquals(bufferOf(33, 44, 2, 66, 77), buf)
    }

    @Test
    fun clear_range() {
        fun test(buf: MutableBuffer) {
            buf.clear(3, 4)
            assertEquals(bufferOf(22, 33, 44, 0, 66), buf)
            buf.clear(3)
            assertEquals(bufferOf(22, 33, 44, 0, 0), buf)
            buf.clear()
            assertEquals(ArrayBuffer(5), buf)
        }

        (testArray().asBuffer(start = 1, endExclusive = 6) as MutableBuffer).let(::test)

        CustomBuffer(testArray().asBuffer(start = 1, endExclusive = 6)).let(::test)
    }
}