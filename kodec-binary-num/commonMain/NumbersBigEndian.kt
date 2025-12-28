package io.kodec

object NumbersBigEndian {
    // streamed byte access

    inline fun readInt16(readByte: () -> Int): Short {
        return (readByte() shl 8 or (readByte())).toShort()
    }

    inline fun readInt24(readByte: () -> Int): Int {
        return (readByte().toByte().toInt() shl 16
            or (readByte() shl 8)
            or (readByte()))
    }

    inline fun readUInt24(readByte: () -> Int): Int {
        return (readByte() shl 16
            or (readByte() shl 8)
            or (readByte()))
    }

    inline fun readInt32(readByte: () -> Int): Int {
        return (readByte() shl 24
            or (readByte() shl 16)
            or (readByte() shl 8)
            or (readByte()))
    }

    inline fun readInt40(readByte: () -> Int): Long {
        return ((readByte().toByte().toLong() shl 32)
                or (readByte().toLong() shl 24)
                or (readInt24(readByte).toLong() and 0xFF_FF_FFL))
    }

    inline fun readUInt40(readByte: () -> Int): Long {
        return ((readByte().toLong() shl 32)
                or (readByte().toLong() shl 24)
                or (readInt24(readByte).toLong() and 0xFF_FF_FFL))
    }

    inline fun readInt48(readByte: () -> Int): Long {
        return (readByte().toByte().toLong() shl 40
            or (readByte().toLong() shl 32)
            or (readByte().toLong() shl 24)
            or (readInt24(readByte).toLong() and 0xFF_FF_FFL))
    }

    inline fun readUInt48(readByte: () -> Int): Long {
        return (readByte().toLong() shl 40
                or (readByte().toLong() shl 32)
                or (readByte().toLong() shl 24)
                or (readInt24(readByte).toLong() and 0xFF_FF_FFL))
    }

    inline fun readInt56(readByte: () -> Int): Long {
        return ((readByte().toByte().toLong() shl 48)
                or (readByte().toLong() shl 40)
                or (readByte().toLong() shl 32)
                or (readByte().toLong() shl 24)
                or (readInt24(readByte).toLong() and 0xFF_FF_FFL))
    }

    inline fun readUInt56(readByte: () -> Int): Long {
        return ((readByte().toLong() shl 48)
                or (readByte().toLong() shl 40)
                or (readByte().toLong() shl 32)
                or (readByte().toLong() shl 24)
                or (readInt24(readByte).toLong() and 0xFF_FF_FFL))
    }

    inline fun readInt64(readByte: () -> Int): Long {
        return (readByte().toLong() shl 56
            or (readByte().toLong() shl 48)
            or (readByte().toLong() shl 40)
            or (readByte().toLong() shl 32)
            or (readByte().toLong() shl 24)
            or (readInt24(readByte).toLong() and 0xFF_FF_FFL))
    }

    inline fun writeInt16(v: Short, writeByte: (Int) -> Unit): Int {
        writeByte((v.toInt() ushr 8))
        writeByte(v.toInt())
        return 2
    }

    inline fun writeInt24Unsafe(v: Int, writeByte: (Int) -> Unit): Int {
        writeByte((v ushr 16))
        writeByte((v ushr 8))
        writeByte(v)
        return 3
    }

    inline fun writeInt24(v: Int, writeByte: (Int) -> Unit): Int {
        NumbersCommon.requireInt24Range(v)
        return writeInt24Unsafe(v, writeByte)
    }

    /**
     * @param v *binary* representation of 24-bit unsigned integer in which all 8 left bits must be zero.
     */
    inline fun writeUInt24(v: Int, writeByte: (Int) -> Unit): Int {
        NumbersCommon.requireUInt24Range(v)
        return writeInt24Unsafe(v, writeByte)
    }

    inline fun writeInt32(v: Int, writeByte: (Int) -> Unit): Int {
        writeByte((v ushr 24))
        writeByte((v ushr 16))
        writeByte((v ushr 8))
        writeByte(v)
        return 4
    }

    inline fun writeInt40Unsafe(v: Long, writeByte: (Int) -> Unit): Int {
        writeByte((v ushr 32).toInt())
        writeByte((v ushr 24).toInt())
        writeByte((v ushr 16).toInt())
        writeByte((v ushr 8).toInt())
        writeByte(v.toInt())
        return 5
    }

    inline fun writeInt40(v: Long, writeByte: (Int) -> Unit): Int {
        NumbersCommon.requireInt40Range(v)
        return writeInt40Unsafe(v, writeByte)
    }

    /**
     * @param v *binary* representation of 40-bit unsigned integer in which all 24 left bits must be zero.
     */
    inline fun writeUInt40(v: Long, writeByte: (Int) -> Unit): Int {
        NumbersCommon.requireUInt40Range(v)
        return writeInt40Unsafe(v, writeByte)
    }

    inline fun writeInt48Unsafe(v: Long, writeByte: (Int) -> Unit): Int {
        writeByte((v ushr 40).toInt())
        writeByte((v ushr 32).toInt())
        writeByte((v ushr 24).toInt())
        writeByte((v ushr 16).toInt())
        writeByte((v ushr 8).toInt())
        writeByte(v.toInt())
        return 6
    }

    inline fun writeInt48(v: Long, writeByte: (Int) -> Unit): Int {
        NumbersCommon.requireInt48Range(v)
        return writeInt48Unsafe(v, writeByte)
    }

    /**
     * @param v *binary* representation of 48-bit unsigned integer in which all 16 left bits must be zero.
     */
    inline fun writeUInt48(v: Long, writeByte: (Int) -> Unit): Int {
        NumbersCommon.requireUInt48Range(v)
        return writeInt48Unsafe(v, writeByte)
    }

    inline fun writeInt56Unsafe(v: Long, writeByte: (Int) -> Unit): Int {
        writeByte((v ushr 48).toInt())
        writeByte((v ushr 40).toInt())
        writeByte((v ushr 32).toInt())
        writeByte((v ushr 24).toInt())
        writeByte((v ushr 16).toInt())
        writeByte((v ushr 8).toInt())
        writeByte(v.toInt())
        return 7
    }

    inline fun writeInt56(v: Long, writeByte: (Int) -> Unit): Int {
        NumbersCommon.requireInt56Range(v)
        return writeInt56Unsafe(v, writeByte)
    }

    /**
     * @param v *binary* representation of 56-bit unsigned integer in which all 8 left bits must be zero.
     */
    inline fun writeUInt56(v: Long, writeByte: (Int) -> Unit): Int {
        NumbersCommon.requireUInt56Range(v)
        return writeInt56Unsafe(v, writeByte)
    }

    inline fun writeInt64(v: Long, writeByte: (Int) -> Unit): Int {
        writeByte((v ushr 56).toInt())
        writeByte((v ushr 48).toInt())
        writeByte((v ushr 40).toInt())
        writeByte((v ushr 32).toInt())
        writeByte((v ushr 24).toInt())
        writeByte((v ushr 16).toInt())
        writeByte((v ushr 8).toInt())
        writeByte(v.toInt())
        return 8
    }

    // random byte access

    inline fun getInt16(getByte: (offset: Int) -> Int): Short {
        return (getByte(0) shl 8 or (getByte(1))).toShort()
    }

    inline fun getInt24(getByte: (offset: Int) -> Int): Int {
        return (getByte(0).toByte().toInt() shl 16
            or (getByte(1) shl 8)
            or (getByte(2)))
    }

    inline fun getUInt24(getByte: (offset: Int) -> Int): Int {
        return (getByte(0) shl 16
            or (getByte(1) shl 8)
            or (getByte(2)))
    }

    inline fun getInt32(getByte: (offset: Int) -> Int): Int {
        return (getByte(0) shl 24
            or (getByte(1) shl 16)
            or (getByte(2) shl 8)
            or (getByte(3)))
    }

    inline fun getInt40(getByte: (offset: Int) -> Int): Long {
        return (getByte(0).toByte().toLong() shl 32
            or (getByte(1).toLong() shl 24)
            or (getInt24 { offset -> getByte(2 + offset) }.toLong() and NumbersCommon.UINT_24_MAX.toLong()))
    }

    inline fun getUInt40(getByte: (offset: Int) -> Int): Long {
        return (getByte(0).toLong() shl 32
            or (getByte(1).toLong() shl 24)
            or (getInt24 { offset -> getByte(2 + offset) }.toLong() and NumbersCommon.UINT_24_MAX.toLong()))
    }

    inline fun getInt48(getByte: (offset: Int) -> Int): Long {
        return (getByte(0).toByte().toLong() shl 40
            or (getByte(1).toLong() shl 32)
            or (getByte(2).toLong() shl 24)
            or (getInt24 { offset -> getByte(3 + offset) }.toLong() and NumbersCommon.UINT_24_MAX.toLong()))
    }

    inline fun getUInt48(getByte: (offset: Int) -> Int): Long {
        return (getByte(0).toLong() shl 40
            or (getByte(1).toLong() shl 32)
            or (getByte(2).toLong() shl 24)
            or (getInt24 { offset -> getByte(3 + offset) }.toLong() and NumbersCommon.UINT_24_MAX.toLong()))
    }

    inline fun getInt56(getByte: (offset: Int) -> Int): Long {
        return ((getByte(0).toByte().toLong() shl 48)
            or (getByte(1).toLong() shl 40)
            or (getByte(2).toLong() shl 32)
            or (getByte(3).toLong() shl 24)
            or (getInt24 { offset -> getByte(4 + offset) }.toLong() and NumbersCommon.UINT_24_MAX.toLong()))
    }

    inline fun getUInt56(getByte: (offset: Int) -> Int): Long {
        return ((getByte(0).toLong() shl 48)
            or (getByte(1).toLong() shl 40)
            or (getByte(2).toLong() shl 32)
            or (getByte(3).toLong() shl 24)
            or (getInt24 { offset -> getByte(4 + offset) }.toLong() and NumbersCommon.UINT_24_MAX.toLong()))
    }

    inline fun getInt64(getByte: (offset: Int) -> Int): Long {
        return (getByte(0).toLong() shl 56
            or (getByte(1).toLong() shl 48)
            or (getByte(2).toLong() shl 40)
            or (getByte(3).toLong() shl 32)
            or (getByte(4).toLong() shl 24)
            or (getInt24 { offset -> getByte(5 + offset) }.toLong() and NumbersCommon.UINT_24_MAX.toLong()))
    }

    inline fun putInt16(v: Short, putByte: (pos: Int, value: Int) -> Unit): Int {
        putByte(0, (v.toInt() ushr 8))
        putByte(1, v.toInt())
        return 2
    }

    inline fun putInt24Unsafe(v: Int, putByte: (pos: Int, value: Int) -> Unit): Int {
        putByte(0, (v ushr 16))
        putByte(1, (v ushr 8))
        putByte(2, v)
        return 3
    }

    inline fun putInt24(v: Int, putByte: (pos: Int, value: Int) -> Unit): Int {
        NumbersCommon.requireInt24Range(v)
        return putInt24Unsafe(v, putByte)
    }

    inline fun putUInt24(v: Int, putByte: (pos: Int, value: Int) -> Unit): Int {
        NumbersCommon.requireUInt24Range(v)
        return putInt24Unsafe(v, putByte)
    }

    inline fun putInt32(v: Int, putByte: (pos: Int, value: Int) -> Unit): Int {
        putByte(0, (v ushr 24))
        putByte(1, (v ushr 16))
        putByte(2, (v ushr 8))
        putByte(3, v)
        return 4
    }

    inline fun putInt40Unsafe(v: Long, writeByte: (pos: Int, value: Int) -> Unit): Int {
        writeByte(0, (v ushr 32).toInt())
        writeByte(1, (v ushr 24).toInt())
        writeByte(2, (v ushr 16).toInt())
        writeByte(3, (v ushr 8).toInt())
        writeByte(4, v.toInt())
        return 5
    }

    inline fun putInt40(v: Long, putByte: (pos: Int, value: Int) -> Unit): Int {
        NumbersCommon.requireInt40Range(v)
        return putInt40Unsafe(v, putByte)
    }

    inline fun putUInt40(v: Long, putByte: (pos: Int, value: Int) -> Unit): Int {
        NumbersCommon.requireUInt40Range(v)
        return putInt40Unsafe(v, putByte)
    }

    inline fun putInt48Unsafe(v: Long, putByte: (pos: Int, value: Int) -> Unit): Int {
        putByte(0, (v ushr 40).toInt())
        putByte(1, (v ushr 32).toInt())
        putByte(2, (v ushr 24).toInt())
        putByte(3, (v ushr 16).toInt())
        putByte(4, (v ushr 8).toInt())
        putByte(5, v.toInt())
        return 6
    }

    inline fun putInt48(v: Long, putByte: (pos: Int, value: Int) -> Unit): Int {
        NumbersCommon.requireInt48Range(v)
        return putInt48Unsafe(v, putByte)
    }

    inline fun putUInt48(v: Long, putByte: (pos: Int, value: Int) -> Unit): Int {
        NumbersCommon.requireUInt48Range(v)
        return putInt48Unsafe(v, putByte)
    }

    inline fun putInt56Unsafe(v: Long, putByte: (pos: Int, value: Int) -> Unit): Int {
        putByte(0, (v ushr 48).toInt())
        putByte(1, (v ushr 40).toInt())
        putByte(2, (v ushr 32).toInt())
        putByte(3, (v ushr 24).toInt())
        putByte(4, (v ushr 16).toInt())
        putByte(5, (v ushr 8).toInt())
        putByte(6, v.toInt())
        return 7
    }

    inline fun putInt56(v: Long, putByte: (pos: Int, value: Int) -> Unit): Int {
        NumbersCommon.requireInt56Range(v)
        return putInt56Unsafe(v, putByte)
    }

    inline fun putUInt56(v: Long, putByte: (pos: Int, value: Int) -> Unit): Int {
        NumbersCommon.requireUInt56Range(v)
        return putInt56Unsafe(v, putByte)
    }

    inline fun putInt64(v: Long, putByte: (pos: Int, value: Int) -> Unit): Int {
        putByte(0, (v ushr 56).toInt())
        putByte(1, (v ushr 48).toInt())
        putByte(2, (v ushr 40).toInt())
        putByte(3, (v ushr 32).toInt())
        putByte(4, (v ushr 24).toInt())
        putByte(5, (v ushr 16).toInt())
        putByte(6, (v ushr 8).toInt())
        putByte(7, v.toInt())
        return 8
    }
}