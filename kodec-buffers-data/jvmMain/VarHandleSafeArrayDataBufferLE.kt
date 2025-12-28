package io.kodec.buffers

internal open class VarHandleSafeArrayDataBufferLE(
    array: ByteArray,
    start: Int = 0,
    endExclusive: Int = array.size
): VarHandleArrayDataBufferLE(array, start, endExclusive) {
    override fun getByte(pos: Int): Byte {
        if (pos !in 0..<size) indexOutOfBounds(pos)
        return array[start + pos]
    }

    override fun getInt16(pos: Int): Short {
        rangeCheck(pos, 2)
        return super.getInt16(pos)
    }

    override fun getInt24(pos: Int): Int {
        rangeCheck(pos, 3)
        return super.getInt24(pos)
    }

    override fun getInt32(pos: Int): Int {
        rangeCheck(pos, 4)
        return super.getInt32(pos)
    }

    override fun getFloat32(pos: Int): Float {
        rangeCheck(pos, 4)
        return super.getFloat32(pos)
    }

    override fun getFloat64(pos: Int): Double {
        rangeCheck(pos, 8)
        return super.getFloat64(pos)
    }

    override fun getInt64(pos: Int): Long {
        rangeCheck(pos, 8)
        return super.getInt64(pos)
    }

    override fun putInt8(pos: Int, v: Byte): Int {
        if (pos !in 0..<size) indexOutOfBounds(pos)
        return super.putInt8(pos, v)
    }

    override fun putInt16(pos: Int, v: Short): Int {
        rangeCheck(pos, 2)
        return super.putInt16(pos, v)
    }

    override fun putInt32(pos: Int, v: Int): Int {
        rangeCheck(pos, 4)
        return super.putInt32(pos, v)
    }

    override fun putFloat32(pos: Int, v: Float): Int {
        rangeCheck(pos, 4)
        return super.putFloat32(pos, v)
    }

    override fun putInt64(pos: Int, v: Long): Int {
        rangeCheck(pos, 8)
        return super.putInt64(pos, v)
    }

    override fun putFloat64(pos: Int, v: Double): Int {
        rangeCheck(pos, 8)
        return super.putFloat64(pos, v)
    }

    override fun getInt40(pos: Int): Long {
        rangeCheck(pos, 5)
        return super.getInt40(pos)
    }

    override fun getInt48(pos: Int): Long {
        rangeCheck(pos, 6)
        return super.getInt48(pos)
    }

    override fun getInt56(pos: Int): Long {
        rangeCheck(pos, 7)
        return super.getInt56(pos)
    }

    override fun putInt24(pos: Int, v: Int): Int {
        rangeCheck(pos, 3)
        return super.putInt24(pos, v)
    }

    override fun putInt40(pos: Int, v: Long): Int {
        rangeCheck(pos, 5)
        return super.putInt40(pos, v)
    }

    override fun putInt48(pos: Int, v: Long): Int {
        rangeCheck(pos, 6)
        return super.putInt48(pos, v)
    }

    override fun putInt56(pos: Int, v: Long): Int {
        rangeCheck(pos, 7)
        return super.putInt56(pos, v)
    }
}