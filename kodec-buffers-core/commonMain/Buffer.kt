package io.kodec.buffers

import kotlin.jvm.JvmField

interface Buffer {
    val size: Int

    /**
     * @return unsigned value of byte at specified [pos]
     * @throws IndexOutOfBoundsException if [pos] is out of bounds
     */
    operator fun get(pos: Int): Int

    /** @return unsigned value of byte at specified [pos] or -1 if [pos] is out of bounds */
    fun tryGet(pos: Int): Int = if (pos in 0..<size) get(pos) else -1

    /**
     * @return value of byte at specified [pos]
     * @throws IndexOutOfBoundsException if [pos] is out of bounds
     */
    fun getByte(pos: Int): Byte = get(pos).toByte()

    /**
     * Returns a view of the portion of this buffer between the
     * specified [start] (inclusive) and [endExclusive].
     * Any changes in this buffer are reflected in the returned buffer.
     */
    fun subBuffer(start: Int, endExclusive: Int = size): Buffer =
        SubBufferWrapper(this, start, endExclusive)

    fun toByteArray(start: Int = 0, endExclusive: Int = size): ByteArray {
        checkRange(start, endExclusive)
        return ByteArray(endExclusive - start) { i -> get(start + i).toByte() }
    }

    fun equalsRange(
        other: Buffer,
        thisOffset: Int = 0,
        otherOffset: Int = 0,
        size: Int = other.size - otherOffset
    ): Boolean {
        if (checkEqualsRangeInputs(other, thisOffset, otherOffset, size)) return true

        if (thisOffset + size > this.size || otherOffset + size > other.size) return false

        for (i in 0 until size) {
            if (this[thisOffset + i] != other[otherOffset + i]) return false
        }

        return true
    }

    companion object {
        @JvmField
        val Empty: Buffer = emptyByteArray.asArrayBuffer()
    }
}

internal fun Buffer.checkEqualsRangeInputs(
    other: Buffer,
    thisOffset: Int = 0,
    otherOffset: Int = 0,
    size: Int = other.size - otherOffset
): Boolean {
    if (thisOffset < 0 || otherOffset < 0) {
        invalidEqualsRangeArgs(other, thisOffset, otherOffset, size)
    }

    if (size == 0) {
        if (thisOffset > this.size || otherOffset > other.size) {
            invalidEqualsRangeArgs(other, thisOffset, otherOffset, size)
        }
        return true
    } else {
        if (thisOffset >= this.size || otherOffset >= other.size || size < 0) {
            invalidEqualsRangeArgs(other, thisOffset, otherOffset, size)
        }
    }

    return false
}

private fun Buffer.invalidEqualsRangeArgs(
    data: Buffer,
    thisOffset: Int,
    otherOffset: Int,
    size: Int
): Nothing = throw IllegalArgumentException(
    "invalid arguments: thisOffset=$thisOffset, otherOffset=$otherOffset, size=$size" +
    " (this.size=${this.size}, other.size=${data.size})"
)

@PublishedApi
internal fun Buffer.checkRange(start: Int, end: Int) {
    if (start < 0 || end > size || start > end)
        invalidRange(start, end)
}

private fun Buffer.invalidRange(start: Int, endExclusive: Int): Nothing {
    throw IllegalArgumentException("invalid range: $start..<$endExclusive, size=$size")
}

val Buffer.indices: IntRange get() = 0 until size