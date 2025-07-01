package io.kodec.buffers

import io.kodec.NumbersBigEndian
import karamel.utils.asInt

internal class ArrayDataBufferUnsafeBE(array: ByteArray, start: Int, endExclusive: Int):
    ArrayDataBuffer(array, start = start, endExclusive = endExclusive)
{
    override val byteOrder: ByteOrder get() = ByteOrder.BigEndian

    override fun getInt16(pos: Int): Short =
        NumbersBigEndian.getInt16 { offset -> array[this.start + pos + offset].asInt() }
    override fun getInt24(pos: Int): Int   =
        NumbersBigEndian.getInt24 { offset -> array[this.start + pos + offset].asInt() }
    override fun getInt32(pos: Int): Int   =
        NumbersBigEndian.getInt32 { offset -> array[this.start + pos + offset].asInt() }
    override fun getInt40(pos: Int): Long  =
        NumbersBigEndian.getInt40 { offset -> array[this.start + pos + offset].asInt() }
    override fun getInt48(pos: Int): Long  =
        NumbersBigEndian.getInt48 { offset -> array[this.start + pos + offset].asInt() }
    override fun getInt56(pos: Int): Long  =
        NumbersBigEndian.getInt56 { offset -> array[this.start + pos + offset].asInt() }
    override fun getInt64(pos: Int): Long  =
        NumbersBigEndian.getInt64 { offset -> array[this.start + pos + offset].asInt() }

    override fun putInt16(pos: Int, v: Short): Int =
        NumbersBigEndian.putInt16(v) { offset, byte -> array[this.start + pos + offset] = byte.toByte() }
    override fun putInt24(pos: Int, v: Int): Int =
        NumbersBigEndian.putInt24(v) { offset, byte -> array[this.start + pos + offset] = byte.toByte() }
    override fun putInt32(pos: Int, v: Int): Int =
        NumbersBigEndian.putInt32(v) { offset, byte -> array[this.start + pos + offset] = byte.toByte() }
    override fun putInt40(pos: Int, v: Long): Int =
        NumbersBigEndian.putInt40(v) { offset, byte -> array[this.start + pos + offset] = byte.toByte() }
    override fun putInt48(pos: Int, v: Long): Int =
        NumbersBigEndian.putInt48(v) { offset, byte -> array[this.start + pos + offset] = byte.toByte() }
    override fun putInt56(pos: Int, v: Long): Int =
        NumbersBigEndian.putInt56(v) { offset, byte -> array[this.start + pos + offset] = byte.toByte() }
    override fun putInt64(pos: Int, v: Long): Int =
        NumbersBigEndian.putInt64(v) { offset, byte -> array[this.start + pos + offset] = byte.toByte() }

    override fun copy(start: Int, endExclusive: Int): ArrayDataBufferUnsafeBE {
        return ArrayDataBufferUnsafeBE(array, start, endExclusive)
    }
}