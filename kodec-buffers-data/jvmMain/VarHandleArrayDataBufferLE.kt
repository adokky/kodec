package io.kodec.buffers

import io.kodec.NumbersCommon
import karamel.utils.asLong
import karamel.utils.unsafeCast
import java.lang.invoke.MethodHandles.byteArrayViewVarHandle
import java.lang.invoke.VarHandle

internal open class VarHandleArrayDataBufferLE(
    array: ByteArray,
    start: Int = 0,
    endExclusive: Int = array.size
): ArrayDataBuffer(array, start, endExclusive)
{
    override fun getInt16(pos: Int): Short {
        return shortHandle.get(array, start + pos).unsafeCast()
    }

    override fun getInt32(pos: Int): Int {
        return intHandle.get(array, start + pos).unsafeCast()
    }

    override fun getFloat32(pos: Int): Float {
        return floatHandle.get(array, start + pos).unsafeCast()
    }

    override fun getFloat64(pos: Int): Double {
        return doubleHandle.get(array, start + pos).unsafeCast()
    }

    override fun getInt64(pos: Int): Long {
        return longHandle.get(array, start + pos).unsafeCast()
    }

    override fun putInt16(pos: Int, v: Short): Int {
        shortHandle.set(array, start + pos, v)
        return 2
    }

    override fun putInt24(pos: Int, v: Int): Int {
        NumbersCommon.requireInt24Range(v)
        putInt16(pos, v.toShort())
        putInt8(pos + 2, (v shr 16).toByte())
        return 3
    }

    override fun putInt32(pos: Int, v: Int): Int {
        intHandle.set(array, start + pos, v)
        return 4
    }

    override fun putInt40(pos: Int, v: Long): Int {
        NumbersCommon.requireInt40Range(v)
        putInt32(pos, v.toInt())
        putInt8(pos + 4, (v shr 32).toByte())
        return 5
    }

    override fun putInt48(pos: Int, v: Long): Int {
        NumbersCommon.requireInt48Range(v)
        putInt32(pos, v.toInt())
        putInt8(pos + 4, (v shr 32).toByte())
        putInt8(pos + 5, (v shr 40).toByte())
        return 6
    }

    override fun putInt56(pos: Int, v: Long): Int {
        NumbersCommon.requireInt56Range(v)
        putInt32(pos, v.toInt())
        putInt8(pos + 4, (v shr 32).toByte())
        putInt8(pos + 5, (v shr 40).toByte())
        putInt8(pos + 6, (v shr 48).toByte())
        return 7
    }

    override fun putFloat32(pos: Int, v: Float): Int {
        floatHandle.set(array, start + pos, v)
        return 4
    }

    override fun putInt64(pos: Int, v: Long): Int {
        longHandle.set(array, start + pos, v)
        return 8
    }

    override fun putFloat64(pos: Int, v: Double): Int {
        doubleHandle.set(array, start + pos, v)
        return 8
    }

    override fun getInt40(pos: Int): Long {
        val w1 = get(pos)
        val w2 = getInt32(pos + 1)
        return w2.toLong().shl(8).or(w1.toLong())
    }

    override fun getInt48(pos: Int): Long {
        val w1 = getInt16(pos)
        val w2 = getInt32(pos + 2)
        return w2.toLong().shl(16).or(w1.asLong())
    }

    override fun getInt56(pos: Int): Long {
        val w1 = getInt24(pos) and 0xff_ff_ff
        val w2 = getInt32(pos + 3)
        return w2.toLong().shl(24).or(w1.toLong())
    }

    private companion object {
        val shortHandle: VarHandle = byteArrayViewVarHandle(ShortArray::class.java, java.nio.ByteOrder.LITTLE_ENDIAN)
        val intHandle: VarHandle = byteArrayViewVarHandle(IntArray::class.java, java.nio.ByteOrder.LITTLE_ENDIAN)
        val longHandle: VarHandle = byteArrayViewVarHandle(LongArray::class.java, java.nio.ByteOrder.LITTLE_ENDIAN)
        val floatHandle: VarHandle = byteArrayViewVarHandle(FloatArray::class.java, java.nio.ByteOrder.LITTLE_ENDIAN)
        val doubleHandle: VarHandle = byteArrayViewVarHandle(DoubleArray::class.java, java.nio.ByteOrder.LITTLE_ENDIAN)
    }
}