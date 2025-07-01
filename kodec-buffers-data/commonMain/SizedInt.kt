package io.kodec.buffers

fun DataBuffer.getSizedAsInt32(pos: Int, sizeInBytes: Int): Int = when(sizeInBytes) {
    1 -> get(pos)
    2 -> getUInt16AsInt32(pos)
    3 -> getInt24(pos)
    4 -> getInt32(pos)
    else -> wrongSizeInBytes(sizeInBytes, maxSize = 4)
}

fun DataBuffer.getSizedAsInt64(pos: Int, sizeInBytes: Int): Long = when(sizeInBytes) {
    1 -> get(pos).toLong()
    2 -> getUInt16AsInt64(pos)
    3 -> getInt24(pos).toLong()
    4 -> getInt32(pos).toLong()
    5 -> getInt40(pos)
    6 -> getInt48(pos)
    7 -> getInt56(pos)
    8 -> getInt64(pos)
    else -> wrongSizeInBytes(sizeInBytes, maxSize = 8)
}

fun MutableDataBuffer.putSizedInt32(pos: Int, sizeInBytes: Int, value: Int): Int {
    when (sizeInBytes) {
        1 -> set(pos, value)
        2 -> putInt16(pos, value.toShort())
        3 -> putInt24(pos, value)
        4 -> putInt32(pos, value)
        else -> wrongSizeInBytes(sizeInBytes, maxSize = 4)
    }

    return sizeInBytes
}

fun MutableDataBuffer.putSizedInt64(pos: Int, sizeInBytes: Int, value: Long): Int {
    when (sizeInBytes) {
        1 -> set(pos, value.toInt())
        2 -> putInt16(pos, value.toShort())
        3 -> putInt24(pos, value.toInt())
        4 -> putInt32(pos, value.toInt())
        5 -> putInt40(pos, value)
        6 -> putInt48(pos, value)
        7 -> putInt56(pos, value)
        8 -> putInt64(pos, value)
        else -> wrongSizeInBytes(sizeInBytes, maxSize = 8)
    }

    return sizeInBytes
}

private fun wrongSizeInBytes(sizeInBytes: Int, maxSize: Int): Nothing {
    throw IllegalArgumentException("wrong sizeInBytes=$sizeInBytes, should be in range 1..$maxSize")
}