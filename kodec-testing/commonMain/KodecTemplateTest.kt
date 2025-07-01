package io.kodec

import kotlin.test.assertEquals

abstract class KodecTemplateTest {
    private val buffer = ByteArray(1000)

    protected fun getBytes(fill: ((Byte) -> Unit) -> Int): ByteArray {
        var i = 0
        val size = fill { byte: Byte -> buffer[i++] = byte }
        assertEquals(i, size)
        return buffer.copyOf(i)
    }
}