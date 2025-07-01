package io.kodec.buffers

import karamel.utils.IndexOutOfBoundsException
import karamel.utils.asInt
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

open class ArrayBuffer(
    array: ByteArray,
    start: Int = 0,
    endExclusive: Int = array.size
): AbstractBuffer(), MutableBuffer {
    var array: ByteArray = array
        private set
    var start: Int = start
        private set
    var endExclusive: Int = endExclusive
        private set

    final override val size: Int get() = endExclusive - start

    private var hashCode = 0

    init {
        initialRangeChecks()
    }

    private fun initialRangeChecks() {
        if (array.isEmpty()) {
            require(start == 0)
            require(endExclusive == 0)
        } else {
            require(start <= endExclusive) { "'start' ($start) > 'endExclusive' ($endExclusive)" }
            require(start in array.indices) { "offset=$start is out of provided array bounds ${array.indices}" }
            require(endExclusive in 0..array.size) { "endExclusive=$endExclusive is out of bounds (array size=${array.size})" }
        }
    }

    fun setArray(
        array: ByteArray,
        start: Int = 0,
        endExclusive: Int = array.size
    ) {
        this.array = array
        this.start = start
        this.endExclusive = endExclusive

        initialRangeChecks()
    }

    fun setSize(size: Int) {
        val newEnd = start + size
        require(size >= 0 && newEnd <= array.size)
        this.endExclusive = newEnd
    }

    override fun set(pos: Int, byte: Int) {
        array[start + pos] = byte.toByte()
    }

    final override fun get(pos: Int): Int = getByte(pos).asInt()

    override fun getByte(pos: Int): Byte = array[start + pos]

    @JvmOverloads
    fun putBytes(pos: Int, bytes: ArrayBuffer, startIndex: Int = 0, endIndex: Int = bytes.size) {
        bytes.array.copyInto(this.array,
            destinationOffset = this.start + pos,
            startIndex = bytes.start + startIndex,
            endIndex = bytes.start + endIndex
        )
    }

    @JvmOverloads
    fun clear(start: Int = 0, endExclusive: Int = size) {
        array.fill(0, fromIndex = this.start + start, toIndex = this.start + endExclusive)
    }

    override fun toByteArray(fromIndex: Int, toIndex: Int): ByteArray =
        array.copyOfRange(fromIndex = start + fromIndex, toIndex = endExclusive)

    fun toByteArray(range: IntRange): ByteArray = toByteArray(range.first, range.last + 1)

    protected open fun copy(start: Int, endExclusive: Int): ArrayBuffer = ArrayBuffer(array, start, endExclusive)

    override fun subBuffer(start: Int, endExclusive: Int): ArrayBuffer {
        require(start in indices) {
            "start=$start is out of buffer range $indices"
        }

        require(this.start + endExclusive in this.start..this.endExclusive) {
            "invalid sub-buffer range: $start..${endExclusive - 1}. Parent range: ${this.indices}"
        }

        return copy(
            start = this.start + start,
            endExclusive = this.start + endExclusive
        )
    }

    @InternalBuffersApi
    fun setHashCodeUnsafe(hashCode: Int) { this.hashCode = hashCode }

    fun resetHashCode() { this.hashCode = 0 }

    override fun hashCode(): Int {
        if (hashCode == 0) {
            hashCode = when {
                start == 0 && size == array.size -> array.contentHashCode()
                else -> super.hashCode()
            }
        }

        return hashCode
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other is ArrayBuffer) return equals(other)
        return super.equals(other)
    }

    fun equals(other: ArrayBuffer): Boolean {
        val hash = hashCode
        val otherHash = other.hashCode
        if (hash * otherHash != 0 && hash != otherHash) return false

        return array.contentEquals(
            other = other.array,
            start = start,
            end = endExclusive,
            otherStart = other.start,
            otherEnd = other.endExclusive
        )
    }

    override fun equalsRangeUnsafe(thisOffset: Int, other: Buffer, otherOffset: Int, size: Int): Boolean {
        if (other is ArrayBuffer) {
            return fastEqualsRange(other, thisOffset, size, otherOffset)
        }

        return super.equalsRangeUnsafe(thisOffset, other, otherOffset, size)
    }

    private fun fastEqualsRange(
        other: ArrayBuffer,
        thisOffset: Int,
        size: Int,
        otherOffset: Int
    ): Boolean = array.contentEquals(
        other.array,
        start = this.start + thisOffset,
        end = this.start + thisOffset + size,
        otherStart = other.start + otherOffset,
        otherEnd = other.start + otherOffset + size
    )

    companion object {
        @JvmStatic
        val isRangeChacksEnabled: Boolean get() = RANGE_CHECK_ENABLED

        @JvmField
        val Empty: ArrayBuffer = emptyByteArray.asBuffer()
    }
}

internal class ArrayBufferSafe(
    array: ByteArray,
    start: Int,
    endExclusive: Int
): ArrayBuffer(array, start = start, endExclusive = endExclusive) {
    override fun getByte(pos: Int): Byte {
        if (pos !in 0..<size) throw IndexOutOfBoundsException(index = pos, size = size)
        return super.getByte(pos)
    }

    override fun copy(start: Int, endExclusive: Int): ArrayBuffer {
        return ArrayBufferSafe(array, start, endExclusive)
    }
}

fun ArrayBuffer(size: Int, rangeChecks: Boolean = RANGE_CHECK_ENABLED): ArrayBuffer =
    ByteArray(size).asBuffer(
        endExclusive = size,
        rangeChecks = rangeChecks
    )

fun ByteArray.asBuffer(
    start: Int = 0,
    endExclusive: Int = this.size,
    rangeChecks: Boolean = RANGE_CHECK_ENABLED
): ArrayBuffer = when {
    rangeChecks -> ArrayBufferSafe(this, start, endExclusive)
    else -> ArrayBuffer(this, start, endExclusive)
}

fun bufferOf(vararg bytes: Byte): ArrayBuffer = bytes.asBuffer()