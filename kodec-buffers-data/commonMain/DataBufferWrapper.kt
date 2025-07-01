package io.kodec.buffers

internal open class DataBufferWrapperLE(val buffer: Buffer): AbstractDataBufferLE(), Buffer by buffer {
    // kotlin can not resolve return-type conflict, so we 'override' to declare explicit return type
    override fun subBuffer(start: Int, endExclusive: Int): DataBuffer =
        super<AbstractDataBufferLE>.subBuffer(start, endExclusive)

    override fun equalsRange(other: Buffer, thisOffset: Int, otherOffset: Int, size: Int): Boolean =
        super<AbstractDataBufferLE>.equalsRange(other, thisOffset, otherOffset, size)
}

internal open class DataBufferWrapperBE(val buffer: Buffer): AbstractDataBufferBE(), Buffer by buffer {
    // kotlin can not resolve return-type conflict, so we 'override' to declare explicit return type
    override fun subBuffer(start: Int, endExclusive: Int): DataBuffer =
        super<AbstractDataBufferBE>.subBuffer(start, endExclusive)

    override fun equalsRange(other: Buffer, thisOffset: Int, otherOffset: Int, size: Int): Boolean =
        super<AbstractDataBufferBE>.equalsRange(other, thisOffset, otherOffset, size)
}

private fun Buffer.unwrap(): Buffer {
    return when (this) {
        is DataBufferWrapperLE -> buffer.unwrap()
        is DataBufferWrapperBE -> buffer.unwrap()
        else -> this
    }
}

fun Buffer.asDataBuffer(order: ByteOrder): DataBuffer {
    return when (order) {
        ByteOrder.BigEndian -> when (this) {
            is AbstractDataBufferBE -> this
            is ArrayDataBufferSafeBE -> this
            is ArrayDataBufferUnsafeBE -> this
            else -> DataBufferWrapperBE(unwrap())
        }
        ByteOrder.LittleEndian -> {
            // base ArrayReadBuffer is LE
            if (this::class == ArrayDataBuffer::class) return this as DataBuffer
            when (this) {
                is AbstractDataBufferLE -> this
                is ArrayDataBufferSafeLE -> this
                else -> DataBufferWrapperLE(unwrap())
            }
        }
        ByteOrder.Native -> asDataBuffer(NativeByteOrder)
    }
}