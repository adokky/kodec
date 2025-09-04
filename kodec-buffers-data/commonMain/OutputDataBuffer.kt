package io.kodec.buffers

import io.kodec.StringsASCII
import io.kodec.StringsUTF16
import io.kodec.StringsUTF8
import io.kodec.VariableEncoding
import karamel.utils.asInt

interface OutputDataBuffer: OutputBuffer {
    fun putInt8(pos: Int, v: Byte): Int {
        set(pos, v.asInt())
        return 1
    }

    fun putInt64(pos: Int, v: Long): Int

    fun putInt56(pos: Int, v: Long): Int

    fun putInt48(pos: Int, v: Long): Int

    fun putInt40(pos: Int, v: Long): Int

    fun putInt32(pos: Int, v: Int): Int

    fun putInt24(pos: Int, v: Int): Int

    fun putInt16(pos: Int, v: Short): Int

    fun putFloat64(pos: Int, v: Double): Int = putInt64(pos, v.toRawBits())

    fun putFloat32(pos: Int, v: Float): Int = putInt32(pos, v.toRawBits())

    /** Does not support UTF-16 surrogate characters! [IllegalArgumentException] is thrown upon receiving one. */
    fun putCharUtf8(pos: Int, v: Char): Int {
        var index = pos
        return StringsUTF8.write(writeByte = { set(index++, it) }, v)
    }

    fun putCharUtf16(pos: Int, v: Char): Int = putInt16(pos, v.code.toShort())

    fun putBoolean(pos: Int, v: Boolean): Int {
        putInt8(pos, (if (v) 1 else 0).toByte())
        return 1
    }

    fun putVarInt32(pos: Int, value: Int, optimizePositive: Boolean = true): Int {
        var index = pos
        return VariableEncoding.writeInt32(value, optimizePositive) { byte -> putInt8(index++, byte) }
    }

    fun putVarInt64(pos: Int, value: Long, optimizePositive: Boolean = true): Int {
        var index = pos
        return VariableEncoding.writeInt64(value, optimizePositive) { byte -> putInt8(index++, byte) }
    }

    fun putStringUtf16(pos: Int, str: CharSequence, strOffset: Int = 0, endExclusive: Int = str.length): Int {
        var index = pos
        return StringsUTF16.writeBytes(str, strOffset, endExclusive) { byte: Int -> set(index++, byte) }
    }

    /**
     * @return number of written bytes
     */
    fun putStringUtf8(
        pos: Int,
        str: CharSequence,
        strStart: Int = 0,
        strEnd: Int = str.length
    ): Int {
        var index = pos
        return StringsUTF8.write(str, strStart, strEnd) { byte: Byte -> putInt8(index++, byte) }
    }

    /**
     * @param strStart the beginning (inclusive) of the [str] subrange, 0 by default
     * @param strEnd number of [str] characters to write
     * @return number of written bytes
     */
    fun putStringAscii(
        pos: Int,
        str: CharSequence,
        strStart: Int = 0,
        strEnd: Int = str.length
    ): Int {
        var index = pos
        return StringsASCII.write(str, strStart = strStart, strEnd = strEnd) { byte -> set(index++, byte) }
    }
}

interface MutableDataBuffer: DataBuffer, OutputDataBuffer, MutableBuffer

