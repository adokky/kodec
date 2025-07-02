package io.kodec.buffers

open class OutputDataBufferWrapperLE(internal val buffer: OutputBuffer): AbstractOutputDataBufferLE(), OutputBuffer by buffer

open class OutputDataBufferWrapperBE(internal val buffer: OutputBuffer): AbstractOutputDataBufferBE(), OutputBuffer by buffer

private fun OutputBuffer.unwrap(): OutputBuffer = when (this) {
    is OutputDataBufferWrapperLE -> buffer.unwrap()
    is OutputDataBufferWrapperBE -> buffer.unwrap()
    else -> this
}

fun ArrayBuffer.asDataBuffer(
    order: ByteOrder = ByteOrder.Native,
    rangeChecks: Boolean = ArrayBuffer.isRangeChacksEnabled
): ArrayDataBuffer = when(order) {
    ByteOrder.BigEndian -> if (rangeChecks)
        ArrayDataBufferSafeBE(array, start, endExclusive) else
        ArrayDataBufferUnsafeBE(array, start, endExclusive)
    ByteOrder.LittleEndian -> if (rangeChecks)
        ArrayDataBufferSafeLE(array, start, endExclusive) else
        ArrayDataBuffer(array, start, endExclusive)
    ByteOrder.Native -> asDataBuffer(NativeByteOrder)
}

fun OutputBuffer.asDataBuffer(order: ByteOrder = ByteOrder.Native): OutputDataBuffer {
    return when (order) {
        ByteOrder.BigEndian -> when (this) {
            is AbstractOutputDataBufferBE -> this
            is ArrayDataBufferSafeBE -> this
            is ArrayDataBufferUnsafeBE -> this
            is ArrayBuffer -> asDataBuffer(order)
            else -> OutputDataBufferWrapperBE(unwrap())
        }
        ByteOrder.LittleEndian -> {
            // base ArrayDataBuffer is LE
            if (this::class == ArrayDataBuffer::class) return this as OutputDataBuffer
            when (this) {
                is AbstractOutputDataBufferLE -> this
                is ArrayDataBufferSafeLE -> this
                is ArrayBuffer -> asDataBuffer(order)
                else -> OutputDataBufferWrapperLE(unwrap())
            }
        }
        ByteOrder.Native -> asDataBuffer(NativeByteOrder)
    }
}