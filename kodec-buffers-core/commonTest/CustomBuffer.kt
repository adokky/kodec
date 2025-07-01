package io.kodec.buffers

import karamel.utils.asInt

class CustomBuffer(
    val array: ByteArray,
    val start: Int = 0,
    val end: Int = array.size
) : AbstractBuffer(), MutableBuffer {
    constructor(other: ArrayBuffer): this(other.array, other.start, other.endExclusive)

    override val size: Int = end - start

    override fun set(pos: Int, byte: Int) {
        array[start + pos] = byte.toByte()
    }

    override fun get(pos: Int): Int = array[start + pos].asInt()
}