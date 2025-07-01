package io.kodec.buffers

actual fun ByteArray.contentEquals(
    other: ByteArray,
    start: Int,
    end: Int,
    otherStart: Int,
    otherEnd: Int
): Boolean = contentEqualsCommon(other, start, end, otherStart, otherEnd)