package io.kodec.buffers

import kotlin.test.Test
import kotlin.test.assertEquals

class ArraysContentEqualsTest {
    @Test
    fun byteArrayContentEquals() {
        fun check(
            result: Boolean,
            arr1: ByteArray,
            arr2: ByteArray,
            start: Int, end: Int,
            otherStart: Int, otherEnd: Int
        ) {
            assertEquals(result, arr1.contentEquals(arr2, start, end, otherStart, otherEnd))
            assertEquals(result, arr2.contentEquals(arr1, otherStart, otherEnd, start, end))
        }

        check(true,  byteArrayOf(), byteArrayOf(), 0, 0, 0, 0)

        check(true,  byteArrayOf(42), byteArrayOf(42), 0, 1, 0, 1)
        check(false, byteArrayOf(42), byteArrayOf(42), 1, 1, 0, 1)
        check(false, byteArrayOf(42), byteArrayOf(42), 0, 1, 1, 1)

        check(true,  byteArrayOf(42, 56), byteArrayOf(42), 0, 1, 0, 1)
        check(false, byteArrayOf(42, 56), byteArrayOf(42), 1, 2, 0, 1)

        check(false, byteArrayOf(42, 56, 3), byteArrayOf(56), 0, 2, 0, 1)
        check(false, byteArrayOf(42, 56, 3), byteArrayOf(56), 0, 2, 1, 1)
        check(false, byteArrayOf(42, 56, 3), byteArrayOf(56), 0, 1, 1, 1)
        check(true,  byteArrayOf(42, 56, 3), byteArrayOf(56), 1, 2, 0, 1)

        run {
            val a1 = byteArrayOf(42, 56, -71, 3)
            val a2 = byteArrayOf(1, -2, 56, -71, 12)
            check(true,  a1, a2, 1, 3, 2, 4)
            check(false, a1, a2, 0, 3, 2, 4)
            check(false, a1, a2, 1, 3, 2, 5)
            check(false, a1, a2, 1, 3, 1, 4)
            check(false, a1, a2, 0, 3, 0, 3)
            check(true,  a1, a2, 2, 2, 2, 2)
        }
    }
}