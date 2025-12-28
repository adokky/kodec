package io.kodec.buffers

internal actual fun createDataBufferBE(
    array: ByteArray,
    start: Int,
    endExclusive: Int,
    rangeChecks: Boolean
): ArrayDataBuffer {
    return when {
        rangeChecks -> ArrayDataBufferSafeBE(array, start, endExclusive)
        else -> ArrayDataBufferUnsafeBE(array, start, endExclusive)
    }
}

internal actual fun createDataBufferLE(
    array: ByteArray,
    start: Int,
    endExclusive: Int,
    rangeChecks: Boolean
): ArrayDataBuffer {
    return when {
        rangeChecks -> ArrayDataBufferSafeLE(array, start, endExclusive)
        else -> ArrayDataBuffer(array, start, endExclusive)
    }
}