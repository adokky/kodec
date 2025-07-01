package io.kodec

object VariableEncoding {
    inline fun readInt32(optimizePositive: Boolean = true, nextByte: () -> Int): Int {
        var result = 0
        var shift = 0

        while (shift < 7 * 5) {
            val b = nextByte()
            result = result or ((b and 0x7f) shl shift)
            if (b and 0x80 != 0x80) break
            shift += 7
        }

        if (!optimizePositive) result = unZigZag(result)

        return result
    }

    inline fun readInt64(optimizePositive: Boolean = true, nextByte: () -> Int): Long {
        var result = 0L
        var shift = 0

        while (shift < 7 * 10) {
            val b = nextByte().toLong()
            result = result or ((b and 0x7fL) shl shift)
            if (b and 0x80L != 0x80L) break
            shift += 7
        }

        if (!optimizePositive) result = unZigZag(result)

        return result
    }

    inline fun writeInt32(
        value: Int,
        optimizePositive: Boolean = true,
        writeByte: (Byte) -> Unit
    ): Int {
        var v = value
        if (!optimizePositive) v = zigZag(v)

        var written = 0
        while (true) {
            written++

            if (v and 0x7F.inv() == 0) {
                writeByte(v.toByte())
                break
            }

            writeByte(((v and 0x7F) or 0x80).toByte())
            v = v ushr 7
        }

        return written
    }

    inline fun writeInt64(
        value: Long,
        optimizePositive: Boolean = true,
        writeByte: (Byte) -> Unit
    ): Int {
        var v = value
        if (!optimizePositive) v = zigZag(v)

        var written = 0
        while (true) {
            written++

            if (v and 0x7F.inv() == 0L) {
                writeByte(v.toByte())
                break
            }

            writeByte(((v and 0x7F) or 0x80).toByte())
            v = v ushr 7
        }

        return written
    }

    fun zigZag(v: Int): Int = (v shl 1) xor (v shr 31)

    fun zigZag(v: Long): Long = (v shl 1) xor (v shr 63)

    fun unZigZag(result: Int): Int = (result ushr 1) xor -(result and 1)

    fun unZigZag(result: Long): Long = (result ushr 1) xor -(result and 1)
}


