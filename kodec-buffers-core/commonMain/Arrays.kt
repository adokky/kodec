package io.kodec.buffers

internal fun ByteArray.contentEqualsCommon(
    other: ByteArray,
    start: Int, end: Int,
    otherStart: Int, otherEnd: Int
): Boolean {
    val size = end - start
    if (size != otherEnd - otherStart) return false

    for (i in 0..<size) {
        if (this[start + i] != other[otherStart + i]) return false
    }

    return true
}

expect fun ByteArray.contentEquals(
    other: ByteArray,
    start: Int, end: Int,
    otherStart: Int, otherEnd: Int
): Boolean

val emptyByteArray: ByteArray = ByteArray(0)