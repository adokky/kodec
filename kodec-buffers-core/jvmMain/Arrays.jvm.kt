package io.kodec.buffers

actual fun ByteArray.contentEquals(
    other: ByteArray,
    start: Int,
    end: Int,
    otherStart: Int,
    otherEnd: Int
): Boolean = java.util.Arrays.equals(this, start, end, other, otherStart, otherEnd)