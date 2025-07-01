package io.kodec.buffers

import io.kodec.NumbersBigEndian
import karamel.utils.asInt

internal class ArrayDataBufferSafeBE(array: ByteArray, start: Int = 0, endExclusive: Int = 0):
    ArrayDataBuffer(array, start = start, endExclusive = endExclusive)
{
    override val byteOrder: ByteOrder get() = ByteOrder.BigEndian

    override fun getByte(pos: Int): Byte {
        if (pos !in 0..<size) indexOutOfBounds(pos)
        return super.getByte(pos)
    }

    override fun getInt16(pos: Int): Short {
        rangeCheck(index = pos, length = 2)
        return NumbersBigEndian.getInt16 { offset -> array[this.start + pos + offset].asInt() }
    }

    override fun getInt24(pos: Int): Int {
        rangeCheck(index = pos, length = 3)
        return NumbersBigEndian.getInt24 { offset -> array[this.start + pos + offset].asInt() }
    }

    override fun getInt32(pos: Int): Int {
        rangeCheck(index = pos, length = 4)
        return NumbersBigEndian.getInt32 { offset -> array[this.start + pos + offset].asInt() }
    }

    override fun getInt40(pos: Int): Long {
        rangeCheck(index = pos, length = 5)
        return NumbersBigEndian.getInt40 { offset -> array[this.start + pos + offset].asInt() }
    }

    override fun getInt48(pos: Int): Long {
        rangeCheck(index = pos, length = 6)
        return NumbersBigEndian.getInt48 { offset -> array[this.start + pos + offset].asInt() }
    }

    override fun getInt56(pos: Int): Long {
        rangeCheck(index = pos, length = 7)
        return NumbersBigEndian.getInt56 { offset -> array[this.start + pos + offset].asInt() }
    }

    override fun getInt64(pos: Int): Long {
        rangeCheck(index = pos, length = 8)
        return NumbersBigEndian.getInt64 { offset -> array[this.start + pos + offset].asInt() }
    }

    override fun putInt8(pos: Int, v: Byte): Int {
        if (pos !in 0..<size) indexOutOfBounds(pos)
        return super.putInt8(pos, v)
    }

    override fun putInt16(pos: Int, v: Short): Int {
        rangeCheck(index = pos, length = 2)
        return NumbersBigEndian.putInt16(v) { offset, byte -> array[this.start + pos + offset] = byte.toByte() }
    }

    override fun putInt24(pos: Int, v: Int): Int {
        rangeCheck(index = pos, length = 3)
        return NumbersBigEndian.putInt24(v) { offset, byte -> array[this.start + pos + offset] = byte.toByte() }
    }

    override fun putInt32(pos: Int, v: Int): Int {
        rangeCheck(index = pos, length = 4)
        return NumbersBigEndian.putInt32(v) { offset, byte -> array[this.start + pos + offset] = byte.toByte() }
    }

    override fun putInt40(pos: Int, v: Long): Int {
        rangeCheck(index = pos, length = 5)
        return NumbersBigEndian.putInt40(v) { offset, byte -> array[this.start + pos + offset] = byte.toByte() }
    }

    override fun putInt48(pos: Int, v: Long): Int {
        rangeCheck(index = pos, length = 6)
        return NumbersBigEndian.putInt48(v) { offset, byte -> array[this.start + pos + offset] = byte.toByte() }
    }

    override fun putInt56(pos: Int, v: Long): Int {
        rangeCheck(index = pos, length = 7)
        return NumbersBigEndian.putInt56(v) { offset, byte -> array[this.start + pos + offset] = byte.toByte() }
    }

    override fun putInt64(pos: Int, v: Long): Int {
        rangeCheck(index = pos, length = 8)
        return NumbersBigEndian.putInt64(v) { offset, byte -> array[this.start + pos + offset] = byte.toByte() }
    }

    override fun copy(start: Int, endExclusive: Int): ArrayDataBufferSafeBE {
        return ArrayDataBufferSafeBE(array, start, endExclusive)
    }
}