package io.kodec.buffers

internal class ArrayDataBufferSafeLE(array: ByteArray, start: Int, endExclusive: Int):
    ArrayDataBuffer(array, start = start, endExclusive = endExclusive)
{
    override fun getByte(pos: Int): Byte {
        if (pos !in 0..<size) indexOutOfBounds(pos)
        return super.getByte(pos)
    }

    override fun getInt16(pos: Int): Short {
        rangeCheck(index = pos, length = 2)
        return super.getInt16(pos)
    }

    override fun getInt24(pos: Int): Int {
        rangeCheck(index = pos, length = 3)
        return super.getInt24(pos)
    }

    override fun getInt32(pos: Int): Int {
        rangeCheck(index = pos, length = 4)
        return super.getInt32(pos)
    }

    override fun getInt40(pos: Int): Long {
        rangeCheck(index = pos, length = 5)
        return super.getInt40(pos)
    }

    override fun getInt48(pos: Int): Long {
        rangeCheck(index = pos, length = 6)
        return super.getInt48(pos)
    }

    override fun getInt56(pos: Int): Long {
        rangeCheck(index = pos, length = 7)
        return super.getInt56(pos)
    }

    override fun getInt64(pos: Int): Long {
        rangeCheck(index = pos, length = 8)
        return super.getInt64(pos)
    }

    override fun putInt8(pos: Int, v: Byte): Int {
        if (pos !in 0..<size) indexOutOfBounds(pos)
        return super.putInt8(pos, v)
    }

    override fun putInt16(pos: Int, v: Short): Int {
        rangeCheck(index = pos, length = 2)
        return super.putInt16(pos, v)
    }

    override fun putInt24(pos: Int, v: Int): Int {
        rangeCheck(index = pos, length = 3)
        return super.putInt24(pos, v)
    }

    override fun putInt32(pos: Int, v: Int): Int {
        rangeCheck(index = pos, length = 4)
        return super.putInt32(pos, v)
    }

    override fun putInt40(pos: Int, v: Long): Int {
        rangeCheck(index = pos, length = 5)
        return super.putInt40(pos, v)
    }

    override fun putInt48(pos: Int, v: Long): Int {
        rangeCheck(index = pos, length = 6)
        return super.putInt48(pos, v)
    }

    override fun putInt56(pos: Int, v: Long): Int {
        rangeCheck(index = pos, length = 7)
        return super.putInt56(pos, v)
    }

    override fun putInt64(pos: Int, v: Long): Int {
        rangeCheck(index = pos, length = 8)
        return super.putInt64(pos, v)
    }

    override fun copy(start: Int, endExclusive: Int): ArrayDataBufferSafeLE {
        return ArrayDataBufferSafeLE(array, start, endExclusive)
    }
}