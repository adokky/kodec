package io.kodec

import io.kodec.NumbersBigEndian.INT_24_MAX
import io.kodec.NumbersBigEndian.INT_40_MAX
import io.kodec.NumbersBigEndian.INT_48_MAX
import io.kodec.NumbersBigEndian.INT_56_MAX
import karamel.utils.asLong

object NumbersLittleEndian {
    // streamed byte access
    
    inline fun readInt16(readByte: () -> Int): Short {
        return (readByte() or (readByte() shl 8)).toShort()
    }

    inline fun readInt24(readByte: () -> Int): Int {
        return (readByte()
                or (readByte() shl 8)
                or (readByte() shl 16))
    }

    inline fun readInt32(readByte: () -> Int): Int {
        return (readByte()
            or (readByte() shl 8)
            or (readByte() shl 16)
            or (readByte() shl 24))
    }

    inline fun readInt40(readByte: () -> Int): Long {
        return ((readByte()
            or (readByte() shl 8)
            or (readByte() shl 16)
            or (readByte() shl 24)).asLong()
            or (readByte().toLong() shl 32))
    }

    inline fun readInt48(readByte: () -> Int): Long {
        return ((readByte()
            or (readByte() shl 8)
            or (readByte() shl 16)
            or (readByte() shl 24)).asLong()
            or (readByte().toLong() shl 32)
            or (readByte().toLong() shl 40))
    }

    inline fun readInt56(readByte: () -> Int): Long {
        return ((readByte()
            or (readByte() shl 8)
            or (readByte() shl 16)
            or (readByte() shl 24)).asLong()
            or (readByte().toLong() shl 32)
            or (readByte().toLong() shl 40)
            or (readByte().toLong() shl 48))
    }

    inline fun readInt64(readByte: () -> Int): Long {
        return ((readByte()
            or (readByte() shl 8)
            or (readByte() shl 16)
            or (readByte() shl 24)).asLong()
            or (readByte().toLong() shl 32)
            or (readByte().toLong() shl 40)
            or (readByte().toLong() shl 48)
            or (readByte().toLong() shl 56))
    }

    inline fun writeInt16(v: Short, writeByte: (Int) -> Unit): Int {
        writeByte(v.toInt())
        writeByte((v.toInt() ushr 8))
        return 2
    }

    inline fun writeInt24Unsafe(v: Int, writeByte: (Int) -> Unit): Int {
        writeByte(v)
        writeByte((v ushr 8))
        writeByte((v ushr 16))
        return 3
    }

    /**
     * @param v *binary* representation of 24-bit unsigned integer in which all 8 left bits must be zero.
     */
    inline fun writeInt24(v: Int, writeByte: (Int) -> Unit): Int {
        require(v ushr 24 == 0) { "'writeInt24' accepts only Int in range 0..$INT_24_MAX" }
        return writeInt24Unsafe(v, writeByte)
    }

    inline fun writeInt32(v: Int, writeByte: (Int) -> Unit): Int {
        writeByte(v)
        writeByte((v ushr 8))
        writeByte((v ushr 16))
        writeByte((v ushr 24))
        return 4
    }

    inline fun writeInt40Unsafe(v: Long, writeByte: (Int) -> Unit): Int {
        writeByte(v.toInt())
        writeByte((v ushr 8).toInt())
        writeByte((v ushr 16).toInt())
        writeByte((v ushr 24).toInt())
        writeByte((v ushr 32).toInt())
        return 5
    }

    /**
     * @param v *binary* representation of 40-bit unsigned integer in which all 16 left bits must be zero.
     */
    inline fun writeInt40(v: Long, writeByte: (Int) -> Unit): Int {
        require(v ushr 40 == 0L) { "'writeInt40' accepts only Long in range 0..$INT_40_MAX" }
        return writeInt40Unsafe(v, writeByte)
    }

    inline fun writeInt48Unsafe(v: Long, writeByte: (Int) -> Unit): Int {
        writeByte(v.toInt())
        writeByte((v ushr 8).toInt())
        writeByte((v ushr 16).toInt())
        writeByte((v ushr 24).toInt())
        writeByte((v ushr 32).toInt())
        writeByte((v ushr 40).toInt())
        return 6
    }

    /**
     * @param v *binary* representation of 48-bit unsigned integer in which all 16 left bits must be zero.
     */
    inline fun writeInt48(v: Long, writeByte: (Int) -> Unit): Int {
        require(v ushr 48 == 0L) { "'writeInt48' accepts only Long in range 0..$INT_48_MAX" }
        return writeInt48Unsafe(v, writeByte)
    }

    inline fun writeInt56Unsafe(v: Long, writeByte: (Int) -> Unit): Int {
        writeByte(v.toInt())
        writeByte((v ushr 8).toInt())
        writeByte((v ushr 16).toInt())
        writeByte((v ushr 24).toInt())
        writeByte((v ushr 32).toInt())
        writeByte((v ushr 40).toInt())
        writeByte((v ushr 48).toInt())
        return 7
    }

    /**
     * @param v *binary* representation of 56-bit unsigned integer in which all 16 left bits must be zero.
     */
    inline fun writeInt56(v: Long, writeByte: (Int) -> Unit): Int {
        require(v ushr 56 == 0L) { "'writeInt56' accepts only Long in range 0..$INT_56_MAX" }
        return writeInt56Unsafe(v, writeByte)
    }

    inline fun writeInt64(v: Long, writeByte: (Int) -> Unit): Int {
        writeByte(v.toInt())
        writeByte((v ushr 8).toInt())
        writeByte((v ushr 16).toInt())
        writeByte((v ushr 24).toInt())
        writeByte((v ushr 32).toInt())
        writeByte((v ushr 40).toInt())
        writeByte((v ushr 48).toInt())
        writeByte((v ushr 56).toInt())
        return 8
    }

    // random byte access

    inline fun getInt16(getByte: (offset: Int) -> Int): Short {
        return (getByte(0) or (getByte(1) shl 8)).toShort()
    }

    inline fun getInt32(getByte: (offset: Int) -> Int): Int {
        return (getByte(0)
            or (getByte(1) shl 8)
            or (getByte(2) shl 16)
            or (getByte(3) shl 24))
    }

    inline fun getInt24(getByte: (offset: Int) -> Int): Int {
        return (getByte(0)
            or (getByte(1) shl 8)
            or (getByte(2) shl 16))
    }

    inline fun getInt40(getByte: (offset: Int) -> Int): Long {
        return ((getByte(0)
             or (getByte(1) shl 8)
             or (getByte(2) shl 16)
             or (getByte(3) shl 24)).asLong()
             or (getByte(4).toLong() shl 32))
    }

    inline fun getInt48(getByte: (offset: Int) -> Int): Long {
        return ((getByte(0)
             or (getByte(1) shl 8)
             or (getByte(2) shl 16)
             or (getByte(3) shl 24)).asLong()
             or (getByte(4).toLong() shl 32)
             or (getByte(5).toLong() shl 40))
    }

    inline fun getInt56(getByte: (offset: Int) -> Int): Long {
        return (((getByte(0)
              or (getByte(1) shl 8)
              or (getByte(2) shl 16)
              or (getByte(3) shl 24)).asLong()
              or (getByte(4).toLong() shl 32)
              or (getByte(5).toLong() shl 40))
              or (getByte(6).toLong() shl 48))
    }

    inline fun getInt64(getByte: (offset: Int) -> Int): Long {
        return (((getByte(0)
              or (getByte(1) shl 8)
              or (getByte(2) shl 16)
              or (getByte(3) shl 24)).asLong()
              or (getByte(4).toLong() shl 32)
              or (getByte(5).toLong() shl 40))
              or (getByte(6).toLong() shl 48)
              or (getByte(7).toLong() shl 56))
    }

    inline fun putInt16(v: Short, putByte: (offset: Int, byte: Int) -> Unit): Int {
        putByte(0, v.toInt())
        putByte(1, (v.toInt() ushr 8))
        return 2
    }

    inline fun putInt24Unsafe(v: Int, putByte: (offset: Int, byte: Int) -> Unit): Int {
        putByte(0, v)
        putByte(1, (v ushr 8))
        putByte(2, (v ushr 16))
        return 3
    }

    inline fun putInt24(v: Int, putByte: (pos: Int, value: Int) -> Unit): Int {
        require(v ushr 24 == 0) { "'putInt24' accepts only Long in range 0..$INT_24_MAX" }
        return putInt24Unsafe(v, putByte)
    }

    inline fun putInt32(v: Int, putByte: (offset: Int, byte: Int) -> Unit): Int {
        putByte(0, v)
        putByte(1, (v ushr 8))
        putByte(2, (v ushr 16))
        putByte(3, (v ushr 24))
        return 4
    }

    inline fun putInt40Unsafe(v: Long, putByte: (offset: Int, byte: Int) -> Unit): Int {
        putByte(0, v.toInt())
        putByte(1, (v ushr 8).toInt())
        putByte(2, (v ushr 16).toInt())
        putByte(3, (v ushr 24).toInt())
        putByte(4, (v ushr 32).toInt())
        return 5
    }

    inline fun putInt40(v: Long, putByte: (pos: Int, value: Int) -> Unit): Int {
        require(v ushr 40 == 0L) { "'putInt40' accepts only Long in range 0..$INT_40_MAX" }
        return putInt40Unsafe(v, putByte)
    }

    inline fun putInt48Unsafe(v: Long, putByte: (offset: Int, byte: Int) -> Unit): Int {
        putByte(0, v.toInt())
        putByte(1, (v ushr 8).toInt())
        putByte(2, (v ushr 16).toInt())
        putByte(3, (v ushr 24).toInt())
        putByte(4, (v ushr 32).toInt())
        putByte(5, (v ushr 40).toInt())
        return 6
    }

    inline fun putInt48(v: Long, putByte: (pos: Int, value: Int) -> Unit): Int {
        require(v ushr 48 == 0L) { "'putInt48' accepts only Long in range 0..$INT_48_MAX" }
        return putInt48Unsafe(v, putByte)
    }

    inline fun putInt56Unsafe(v: Long, putByte: (offset: Int, byte: Int) -> Unit): Int {
        putByte(0, v.toInt())
        putByte(1, (v ushr 8).toInt())
        putByte(2, (v ushr 16).toInt())
        putByte(3, (v ushr 24).toInt())
        putByte(4, (v ushr 32).toInt())
        putByte(5, (v ushr 40).toInt())
        putByte(6, (v ushr 48).toInt())
        return 7
    }

    inline fun putInt56(v: Long, putByte: (pos: Int, value: Int) -> Unit): Int {
        require(v ushr 56 == 0L) { "'putInt56' accepts only Long in range 0..$INT_56_MAX" }
        return putInt56Unsafe(v, putByte)
    }

    inline fun putInt64(v: Long, putByte: (offset: Int, byte: Int) -> Unit): Int {
        putByte(0, v.toInt())
        putByte(1, (v ushr 8).toInt())
        putByte(2, (v ushr 16).toInt())
        putByte(3, (v ushr 24).toInt())
        putByte(4, (v ushr 32).toInt())
        putByte(5, (v ushr 40).toInt())
        putByte(6, (v ushr 48).toInt())
        putByte(7, (v ushr 56).toInt())
        return 8
    }
}