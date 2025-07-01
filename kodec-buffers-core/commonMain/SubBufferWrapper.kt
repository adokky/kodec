package io.kodec.buffers

@InternalBuffersApi
abstract class AbstractSubBufferWrapper(
    source: Buffer,
    start: Int,
    endExclusive: Int
): AbstractBuffer() {
    protected var internalSource: Buffer = source
    open val source: Buffer get() = internalSource

    var start: Int = start
        private set
    var endExclusive: Int = endExclusive
        private set

    final override var size: Int = endExclusive - start
        private set

    private var hashCode = 0

    init {
        checkRange()
    }

    protected fun setInternalSource(source: Buffer, start: Int, endExclusive: Int) {
        this.internalSource = source
        this.start = start
        this.endExclusive = endExclusive
        size = endExclusive - start
        hashCode = 0
        checkRange()
    }

    private fun checkRange() {
        require(start <= endExclusive) { "start=$start > endExclusive=$endExclusive" }
        require(start in 0..endExclusive) { "start not in source buffer range 0..$endExclusive" }
        require(endExclusive <= source.size) { "endExclusive=$endExclusive >= source.size=${source.size}" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Buffer) return false
        if (size != other.size) return false

        if (hashCode != 0 &&
            other is AbstractSubBufferWrapper &&
            other.hashCode != 0 &&
            hashCode != other.hashCode
        ) return false

        return equalsRange(other)
    }

    override fun hashCode(): Int {
        if (hashCode == 0) hashCode = super.hashCode()
        return hashCode
    }

    protected fun ioobe(pos: Int): Nothing =
        throw IndexOutOfBoundsException("attempt to read byte at index ${pos + 1}. length=$size")

    override fun get(pos: Int): Int {
        if (pos < 0 || start + pos >= endExclusive) ioobe(pos)
        return source[pos + start]
    }
}

@OptIn(InternalBuffersApi::class)
class SubBufferWrapper(
    source: Buffer,
    start: Int = 0,
    endExclusive: Int = source.size
): AbstractSubBufferWrapper(source, start, endExclusive) {
    fun setSource(
        source: Buffer,
        start: Int = 0,
        endExclusive: Int = source.size
    ) {
        setInternalSource(source, start, endExclusive)
    }
}