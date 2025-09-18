package io.kodec.buffers

import io.kodec.*
import karamel.utils.IndexOutOfBoundsException
import karamel.utils.asInt
import kotlin.jvm.JvmField

open class ArrayDataBuffer internal constructor(
    array: ByteArray,
    start: Int = 0,
    endExclusive: Int = array.size
): ArrayBuffer(array, start, endExclusive), MutableDataBuffer {
    open val byteOrder: ByteOrder get() = ByteOrder.LittleEndian

    override fun getInt16(pos: Int): Short =
        NumbersLittleEndian.getInt16 { offset -> array[this.start + pos + offset].asInt() }
    override fun getInt24(pos: Int): Int   =
        NumbersLittleEndian.getInt24 { offset -> array[this.start + pos + offset].asInt() }
    override fun getInt32(pos: Int): Int   =
        NumbersLittleEndian.getInt32 { offset -> array[this.start + pos + offset].asInt() }
    override fun getInt40(pos: Int): Long  =
        NumbersLittleEndian.getInt40 { offset -> array[this.start + pos + offset].asInt() }
    override fun getInt48(pos: Int): Long  =
        NumbersLittleEndian.getInt48 { offset -> array[this.start + pos + offset].asInt() }
    override fun getInt56(pos: Int): Long  =
        NumbersLittleEndian.getInt56 { offset -> array[this.start + pos + offset].asInt() }
    override fun getInt64(pos: Int): Long  =
        NumbersLittleEndian.getInt64 { offset -> array[this.start + pos + offset].asInt() }

    override fun putInt8(pos: Int, v: Byte): Int {
        array[start + pos] = v
        return 1
    }

    override fun set(pos: Int, byte: Int) { putInt8(pos, byte.toByte()) }

    override fun putInt64(pos: Int, v: Long): Int =
        NumbersLittleEndian.putInt64(v) { offset, byte -> array[this.start + pos + offset] = byte.toByte() }
    override fun putInt56(pos: Int, v: Long): Int =
        NumbersLittleEndian.putInt56(v) { offset, byte -> array[this.start + pos + offset] = byte.toByte() }
    override fun putInt48(pos: Int, v: Long): Int =
        NumbersLittleEndian.putInt48(v) { offset, byte -> array[this.start + pos + offset] = byte.toByte() }
    override fun putInt40(pos: Int, v: Long): Int =
        NumbersLittleEndian.putInt40(v) { offset, byte -> array[this.start + pos + offset] = byte.toByte() }
    override fun putInt32(pos: Int, v: Int): Int =
        NumbersLittleEndian.putInt32(v) { offset, byte -> array[this.start + pos + offset] = byte.toByte() }
    override fun putInt24(pos: Int, v: Int): Int =
        NumbersLittleEndian.putInt24(v) { offset, byte -> array[this.start + pos + offset] = byte.toByte() }
    override fun putInt16(pos: Int, v: Short): Int =
        NumbersLittleEndian.putInt16(v) { offset, byte -> array[this.start + pos + offset] = byte.toByte() }

    protected fun indexOutOfBounds(pos: Int): Nothing =
        throw IndexOutOfBoundsException(pos, size)

    protected fun rangeCheck(index: Int, length: Int) {
        if (index < 0 || (index + length > size)) indexOutOfBounds(index)
    }

    override fun copy(start: Int, endExclusive: Int): ArrayDataBuffer = ArrayDataBuffer(array, start, endExclusive)

    override fun subBuffer(start: Int, endExclusive: Int): ArrayDataBuffer {
        return super<ArrayBuffer>.subBuffer(start, endExclusive) as ArrayDataBuffer
    }

    companion object {
        @JvmField
        val Empty: ArrayDataBuffer = emptyByteArray.asDataBuffer()
    }
}

fun ArrayDataBuffer(
    size: Int,
    byteOrder: ByteOrder = ByteOrder.Native,
    rangeChecks: Boolean = ArrayBuffer.isRangeChacksEnabled
): ArrayDataBuffer = ByteArray(size).asDataBuffer(
    endExclusive = size,
    byteOrder = byteOrder,
    rangeChecks = rangeChecks
)

fun ByteArray.asDataBuffer(
    start: Int = 0,
    endExclusive: Int = this.size,
    byteOrder: ByteOrder = ByteOrder.Native,
    rangeChecks: Boolean = ArrayBuffer.isRangeChacksEnabled
): ArrayDataBuffer = when(byteOrder) {
    ByteOrder.BigEndian -> when {
        rangeChecks -> ArrayDataBufferSafeBE(this, start, endExclusive)
        else -> ArrayDataBufferUnsafeBE(this, start, endExclusive)
    }
    ByteOrder.LittleEndian -> when {
        rangeChecks -> ArrayDataBufferSafeLE(this, start, endExclusive)
        else -> ArrayDataBuffer(this, start, endExclusive)
    }
    ByteOrder.Native -> asDataBuffer(start, endExclusive, byteOrder = NativeByteOrder, rangeChecks)
}

fun dataBufferOf(vararg bytes: Byte): ArrayDataBuffer = bytes.asDataBuffer()

