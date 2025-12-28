package io.kodec.buffers

internal actual fun createDataBufferBE(
    array: ByteArray,
    start: Int,
    endExclusive: Int,
    rangeChecks: Boolean
): ArrayDataBuffer {
    return when(rangeChecks) {
        true -> VarHandleSafeArrayDataBufferBE(array, start, endExclusive)
        else -> VarHandleArrayDataBufferBE(array, start, endExclusive)
    }
}

internal actual fun createDataBufferLE(
    array: ByteArray,
    start: Int,
    endExclusive: Int,
    rangeChecks: Boolean
): ArrayDataBuffer {
    return when(rangeChecks) {
        true -> VarHandleSafeArrayDataBufferLE(array, start, endExclusive)
        else -> VarHandleArrayDataBufferLE(array, start, endExclusive)
    }
}