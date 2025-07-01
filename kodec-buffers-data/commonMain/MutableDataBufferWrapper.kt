package io.kodec.buffers

open class MutableDataBufferWrapperLE(internal val buffer: MutableBuffer): AbstractMutableDataBufferLE(), MutableBuffer by buffer

open class MutableDataBufferWrapperBE(internal val buffer: MutableBuffer): AbstractMutableDataBufferBE(), MutableBuffer by buffer

private fun MutableBuffer.unwrap(): MutableBuffer = when (this) {
    is MutableDataBufferWrapperLE -> buffer.unwrap()
    is MutableDataBufferWrapperBE -> buffer.unwrap()
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

fun MutableBuffer.asDataBuffer(order: ByteOrder = ByteOrder.Native): MutableDataBuffer {
    return when (order) {
        ByteOrder.BigEndian -> when (this) {
            is AbstractMutableDataBufferBE -> this
            is ArrayDataBufferSafeBE -> this
            is ArrayDataBufferUnsafeBE -> this
            is ArrayBuffer -> asDataBuffer(order)
            else -> MutableDataBufferWrapperBE(unwrap())
        }
        ByteOrder.LittleEndian -> {
            // base ArrayDataBuffer is LE
            if (this::class == ArrayDataBuffer::class) return this as MutableDataBuffer
            when (this) {
                is AbstractMutableDataBufferLE -> this
                is ArrayDataBufferSafeLE -> this
                is ArrayBuffer -> asDataBuffer(order)
                else -> MutableDataBufferWrapperLE(unwrap())
            }
        }
        ByteOrder.Native -> asDataBuffer(NativeByteOrder)
    }
}