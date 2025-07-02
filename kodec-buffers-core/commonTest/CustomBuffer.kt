package io.kodec.buffers

import karamel.utils.asInt

class CustomBuffer(
    val array: ByteArray,
    val start: Int = 0,
    val end: Int = array.size
) : MutableBuffer {
    constructor(other: ArrayBuffer): this(other.array, other.start, other.endExclusive)

    override val size: Int = end - start

    override fun set(pos: Int, byte: Int) {
        array[start + pos] = byte.toByte()
    }

    override fun get(pos: Int): Int = array[start + pos].asInt()

    override fun equals(other: Any?): Boolean {
        other as? Buffer ?: return false

        if (other.size != size) return false

        for (i in indices) {
            if (other[i] != this[i]) return false
        }

        return true
    }

    override fun hashCode(): Int {
        var hash = 1
        for (i in 0 until size) {
            hash = 31 * hash + get(i)
        }
        return hash
    }

    override fun toString(): String = array.sliceArray(start..<end).contentToString()
}