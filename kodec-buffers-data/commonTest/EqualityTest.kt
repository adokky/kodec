package io.kodec.buffers

import dev.adokky.eqtester.testEquality
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class EqualityTest {
    @Test
    fun equal_buffers() {
        ArrayBufferPairs.equalBufferPairs().forEach {
            assertEquals(it.first, it.second)
        }
    }

    @Test
    fun not_equal_buffers() {
        ArrayBufferPairs.notEqualBufferPairs().forEach {
            assertNotEquals(it.first, it.second)
        }
    }

    @Test
    fun automated() {
        testEquality {
            group { byteArrayOf().asDataBuffer() }
            group { byteArrayOf(1).asDataBuffer() }
            group { byteArrayOf(-100, 0, 23, 7).asDataBuffer() }
            group { byteArrayOf(-100, 0, 23, 7).asDataBuffer(2, 3) }
            group { byteArrayOf(-100, 0, 23, 7).asDataBuffer(2) }
            group { byteArrayOf(-100, 0, 23, 7).asDataBuffer(endExclusive = 3) }
        }
    }
}