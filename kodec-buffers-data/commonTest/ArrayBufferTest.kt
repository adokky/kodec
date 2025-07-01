package io.kodec.buffers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ArrayBufferTest {
    private fun ByteArray.test(body: (buf: ArrayDataBuffer) -> Unit) {
        for (rangeChecks in listOf(false, true)) {
            for (byteOrder in ByteOrder.entries) {
                body(asDataBuffer(byteOrder = byteOrder, rangeChecks = rangeChecks))
            }
        }
    }

    @Test
    fun emptyArray() {
        ByteArray(0).test { buf ->
            assertEquals(0, buf.size)
            assertEquals(0, buf.start)
            assertEquals(0, buf.endExclusive)

            for (func in ReadBufferFunctions.All) {
                for (pos in -1..1) {
                    assertFailsWith<IndexOutOfBoundsException> { func(buf, pos) }
                }
            }
        }
    }

    @Test
    fun simple() {
        val MAGIC_BYTE: Byte = 42

        ByteArray(1) { MAGIC_BYTE }.test { buf ->
            assertEquals(1, buf.size)
            assertEquals(0, buf.start)
            assertEquals(1, buf.endExclusive)

            assertEquals(MAGIC_BYTE.toInt() and 0xFF, buf[0])
            assertEquals(MAGIC_BYTE, buf.getInt8(0))
            assertTrue(buf.getBoolean(0))

            for (func in ReadBufferFunctions.SingleByte) {
                for (pos in listOf(-1, 1)) {
                    assertFailsWith<IndexOutOfBoundsException> { func(buf, pos) }
                }
            }

            for (func in ReadBufferFunctions.MultiByte) {
                for (pos in -1..1) {
                    assertFailsWith<IndexOutOfBoundsException> { func(buf, pos) }
                }
            }
        }
    }
}