package io.kodec.buffers

internal object ArrayBufferPairs {
    fun equalBufferPairs(): Sequence<Pair<ArrayDataBuffer, ArrayDataBuffer>> = sequence {
        yield(ArrayDataBuffer(9) to ArrayDataBuffer(9))
        yield(emptyByteArray.asDataBuffer() to emptyByteArray.asDataBuffer())
        yield(
            byteArrayOf(1, 2, 3, -100).asDataBuffer(byteOrder = ByteOrder.BigEndian) to
            byteArrayOf(1, 2, 3, -100).asDataBuffer(byteOrder = ByteOrder.LittleEndian)
        )

        fun bytes() = byteArrayOf(100, -100, 1, 2, 3, 127)

        yield(
            bytes().asDataBuffer(byteOrder = ByteOrder.BigEndian) to
            bytes().asDataBuffer(byteOrder = ByteOrder.LittleEndian)
        )

        fun bytes2() = byteArrayOf(0, 1, 2, 3, -1, 1, 2, 3, 4)

        yield(
            bytes2().asDataBuffer(start = 1, endExclusive = 4, byteOrder = ByteOrder.BigEndian) to
            bytes2().asDataBuffer(start = 5, endExclusive = 8, byteOrder = ByteOrder.LittleEndian)
        )
    }

    fun notEqualBufferPairs(): Sequence<Pair<ArrayDataBuffer, ArrayDataBuffer>> = sequence {
        yield(ArrayDataBuffer(2) to ArrayDataBuffer(1))
        yield(emptyByteArray.asDataBuffer() to ArrayDataBuffer(1))
        yield(
            byteArrayOf(1, 2, 3, -100).asDataBuffer() to
            byteArrayOf(1, 2, 4, -101).asDataBuffer()
        )

        yield(
            byteArrayOf(100, -100, 1, 2, 3, 127).asDataBuffer(byteOrder = ByteOrder.BigEndian) to
            byteArrayOf(100, -100, 1, 2, 4, 127).asDataBuffer(byteOrder = ByteOrder.LittleEndian)
        )

        fun bytes2() = byteArrayOf(0, 1, 2, 3, -1, 1, 2, 3, 4)

        yield(
            bytes2().asDataBuffer(start = 1, endExclusive = 4) to
            bytes2().asDataBuffer(start = 4, endExclusive = 7)
        )

        yield(
            bytes2().asDataBuffer(start = 1, endExclusive = 3) to
            bytes2().asDataBuffer(start = 5, endExclusive = 8)
        )
    }
}