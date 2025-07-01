package io.kodec.buffers

import kotlin.math.min

abstract class AbstractBuffer: Buffer, Comparable<Buffer> {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Buffer) return false
        if (size != other.size) return false
        return equalsRange(other)
    }

    override fun hashCode(): Int {
        var hash = 1
        for (i in 0 until size) {
            hash = 31 * hash + get(i)
        }
        return hash
    }

    override fun toString(): String = buildString(2 + size * 5) {
        append('[')
        for (i in 0 until size) {
            append(this@AbstractBuffer[i])
            append(", ")
        }
        if (size != 0) setLength(length - 2) // remove last comma
        append(']')
    }

    override fun compareTo(other: Buffer): Int {
        if (other === this) return 0

        for (i in 0 until min(size, other.size)) {
            val delta = get(i) - other[i]
            if (delta != 0) return delta
        }

        return size.compareTo(other.size)
    }

    override fun equalsRange(other: Buffer, thisOffset: Int, otherOffset: Int, size: Int): Boolean {
        if (checkEqualsRangeInputs(other, thisOffset, otherOffset, size)) return true
        if (thisOffset + size > this.size || otherOffset + size > other.size) return false
        return equalsRangeUnsafe(thisOffset, other, otherOffset, size)
    }

    protected open fun equalsRangeUnsafe(
        thisOffset: Int,
        other: Buffer,
        otherOffset: Int,
        size: Int
    ): Boolean {
        for (i in 0 ..< size) {
            if (this[thisOffset + i] != other[otherOffset + i]) return false
        }

        return true
    }
}


