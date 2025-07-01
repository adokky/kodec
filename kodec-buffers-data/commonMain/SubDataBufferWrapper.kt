package io.kodec.buffers

import karamel.utils.unsafeCast

@OptIn(InternalBuffersApi::class)
class SubDataBufferWrapper(
    source: DataBuffer,
    start: Int = 0,
    endExclusive: Int = source.size
): AbstractSubBufferWrapper(source, start, endExclusive), DataBuffer {
    override val source: DataBuffer get() = internalSource.unsafeCast()

    fun setSource(
        source: DataBuffer,
        start: Int = 0,
        endExclusive: Int = source.size
    ) {
        setInternalSource(source, start, endExclusive)
    }

    private fun ioobe(bytes: Int, pos: Int): Nothing {
        throw IndexOutOfBoundsException("attempt to read $bytes byte(s) at index ${pos + 1}. length=$size")
    }

    private fun ensure(pos: Int, bytes: Int) {
        if (start + pos + bytes > endExclusive || pos < 0) ioobe(bytes, pos)
    }

    override fun getInt16(pos: Int): Short {
        ensure(pos, 2)
        return source.getInt16(pos + start)
    }

    override fun getInt24(pos: Int): Int {
        ensure(pos, 3)
        return source.getInt24(pos + start)
    }

    override fun getInt32(pos: Int): Int {
        ensure(pos, 4)
        return source.getInt32(pos + start)
    }

    override fun getInt40(pos: Int): Long {
        ensure(pos, 5)
        return source.getInt40(pos + start)
    }

    override fun getInt48(pos: Int): Long {
        ensure(pos, 6)
        return source.getInt48(pos + start)
    }

    override fun getInt56(pos: Int): Long {
        ensure(pos, 7)
        return source.getInt56(pos + start)
    }

    override fun getInt64(pos: Int): Long {
        ensure(pos, 8)
        return source.getInt64(pos + start)
    }

    override fun get(pos: Int): Int {
        ensure(pos, 1)
        return source[pos + start]
    }

    override fun getBytes(pos: Int, dst: ByteArray, dstOffset: Int, length: Int) {
        ensure(pos, length)
        source.getBytes(pos, dst, dstOffset, length)
    }

    override fun getBytes(pos: Int, length: Int): ByteArray {
        ensure(pos, length)
        return source.getBytes(pos, length)
    }

    override fun getStringAscii(pos: Int, dest: CharArray, destStart: Int, destEnd: Int) {
        ensure(pos, destEnd)
        source.getStringAscii(pos, dest, destStart, destEnd)
    }

    override fun getFloat32(pos: Int): Float {
        ensure(pos, 4)
        return source.getFloat32(pos)
    }

    override fun getFloat64(pos: Int): Double {
        ensure(pos, 8)
        return super.getFloat64(pos)
    }

    override fun getChar(pos: Int): Char {
        ensure(pos, 2)
        return super.getChar(pos)
    }
}