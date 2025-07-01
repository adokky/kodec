package io.kodec.buffers

import io.kodec.StringsASCII
import io.kodec.StringsUTF16
import io.kodec.StringsUTF8
import io.kodec.StringsUTF8.readFromByteStream
import io.kodec.VariableEncoding
import karamel.utils.IndexOutOfBoundsException
import karamel.utils.asInt
import kotlin.jvm.JvmField

interface DataBuffer: Buffer {
    data class ReadResult<T>(val bytesRead: Int, val result: T)

    fun getInt8(pos: Int): Byte = getByte(pos)

    fun getInt16(pos: Int): Short

    fun getInt24(pos: Int): Int

    fun getInt32(pos: Int): Int

    fun getInt40(pos: Int): Long

    fun getInt48(pos: Int): Long

    fun getInt56(pos: Int): Long

    fun getInt64(pos: Int): Long

    fun getBytes(
        pos: Int = 0,
        dst: ByteArray,
        dstOffset: Int = 0,
        length: Int = dst.size - pos
    ) {
        for (i in 0 ..< length) {
            dst[dstOffset + i] = getInt8(pos + i)
        }
    }

    fun getBytes(pos: Int, length: Int): ByteArray {
        val result = ByteArray(length)
        getBytes(pos = pos, dst = result, length = length)
        return result
    }

    fun getFloat64(pos: Int): Double = Double.fromBits(getInt64(pos))

    fun getFloat32(pos: Int): Float = Float.fromBits(getInt32(pos))

    fun getChar(pos: Int): Char = getInt16(pos).toInt().toChar()

    fun getBoolean(pos: Int): Boolean = getInt8(pos).toInt() != 0

    fun getVarInt32(pos: Int, optimizePositive: Boolean = true): ReadResult<Int> {
        var index = pos
        val v = VariableEncoding.readInt32(optimizePositive) { get(index++) }
        return ReadResult(bytesRead = index - pos, result = v)
    }

    fun getVarInt64(pos: Int, optimizePositive: Boolean = true): ReadResult<Long> {
        var index = pos
        val v = VariableEncoding.readInt64(optimizePositive) { get(index++) }
        return ReadResult(bytesRead = index - pos, result = v)
    }

    fun getStringAscii(pos: Int, dest: CharArray, destStart: Int = 0, destEnd: Int = dest.size) {
        var index = pos
        StringsASCII.readFromByteStream(dest, destStart, destEnd) { get(index++) }
    }

    /** Used the same "modified" UTF-8 format as in Java standard library */
    fun getStringUtf8(pos: Int, dest: CharArray, destStart: Int = 0, destEnd: Int = dest.size) {
        var index = pos
        StringsUTF8.readFromByteStreamInto(dest = dest, destStart = destStart, destEnd = destEnd) { get(index++) }
    }

    fun getStringUtf16(pos: Int, dest: CharArray, destStart: Int = 0, destEnd: Int = dest.size) {
        if (pos == size && destEnd == 0) return

        if (pos !in 0 until size) throw IndexOutOfBoundsException("pos=$pos, size=$size, destStart=$destStart, destEnd=$destEnd")

        var index = pos
        StringsUTF16.readFromByteStream(dest, destStart, destEnd,
            readByte = {
                val idx = index
                if (idx >= size) return@readFromByteStream -1
                val res = get(idx)
                index = idx + 1
                res
            }
        )
    }

    fun getStringUtf8UntilEnd(pos: Int = 0): String {
        if (pos !in 0..size) throw IndexOutOfBoundsException(pos, size)

        val sb = StringBuilder()

        if (pos < size) StringsUTF8.readFromBytes(
            getByte = { offset -> getInt8(pos + offset).asInt() },
            endExclusive = size - pos,
            appendChar = sb::append
        )

        return sb.toString()
    }

    override fun subBuffer(start: Int, endExclusive: Int): DataBuffer =
        SubDataBufferWrapper(this, start, endExclusive)

    operator fun iterator(): IntIterator = object : IntIterator() {
        var i = 0
        override fun hasNext(): Boolean = i < size
        override fun nextInt(): Int = get(i++)
    }

    companion object {
        @JvmField
        val Empty: DataBuffer = ArrayDataBuffer.Empty
    }
}

fun DataBuffer.getUInt8AsInt32(pos: Int): Int = getInt8(pos).toInt() and 0xFF

fun DataBuffer.getUInt8AsInt64(pos: Int): Long = getInt8(pos).toLong() and 0xFF

fun DataBuffer.getUInt16AsInt32(pos: Int): Int = getInt16(pos).toInt() and 0xFF_FF

fun DataBuffer.getUInt16AsInt64(pos: Int): Long = getInt16(pos).toLong() and 0xFF_FFL

/** @param length number of characters/bytes in resulted length */
fun DataBuffer.getStringAscii(pos: Int, length: Int): String {
    val chars = CharArray(length)
    getStringAscii(pos, chars, destEnd = length)
    return chars.concatToString()
}

/** @param length number of *characters* in resulted length */
fun DataBuffer.getStringUtf8(pos: Int, length: Int): String {
    val chars = CharArray(length)
    getStringUtf8(pos, chars, destEnd = length)
    return chars.concatToString()
}

fun DataBuffer.getStringUtf8ByteSized(pos: Int, byteLength: Int): String {
    val sb = StringBuilder(byteLength)

    var offset = pos
    val end = offset + byteLength

    readFromByteStream(
        readByte = { if (offset < end) get(offset++) else -1 },
        appendChar = { sb.append(it) }
    )

    return sb.toString()
}

/** @param length number of *characters* in resulted length */
fun DataBuffer.getStringUtf16(pos: Int, length: Int): String {
    val chars = CharArray(length)
    getStringUtf16(pos, chars, destEnd = length)
    return chars.concatToString()
}