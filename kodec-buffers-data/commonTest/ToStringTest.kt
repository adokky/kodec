package io.kodec.buffers

import kotlin.test.Test
import kotlin.test.assertEquals

class ToStringTest {
    @Test
    fun empty() {
        assertEquals("[]", emptyByteArray.asDataBuffer().toString())
        assertEquals("[]", byteArrayOf(1, 2).asDataBuffer(1, 1).toString())
        assertEquals("[]", byteArrayOf(1, 2).asDataBuffer(0, 0).toString())
    }

    @Test
    fun non_empty() {
        val bytes = listOf(0, 1, 2, 127, 128, 255)
        val buf = bytes.map { it.toByte() }.toByteArray().asDataBuffer()
        assertEquals(bytes.toString(), buf.toString())

        assertEquals("[255]", byteArrayOf(255.toByte()).asDataBuffer().toString())
    }
}