package io.kodec

import io.kodec.buffers.MutableBuffer
import io.kodec.buffers.OutputBuffer
import kotlin.jvm.JvmStatic

object NumberToString {
    @PublishedApi
    @JvmStatic
    internal val DigitTens = byteArrayOf(
        48, 48, 48, 48, 48, 48, 48, 48, 48, 48,
        49, 49, 49, 49, 49, 49, 49, 49, 49, 49,
        50, 50, 50, 50, 50, 50, 50, 50, 50, 50,
        51, 51, 51, 51, 51, 51, 51, 51, 51, 51,
        52, 52, 52, 52, 52, 52, 52, 52, 52, 52,
        53, 53, 53, 53, 53, 53, 53, 53, 53, 53,
        54, 54, 54, 54, 54, 54, 54, 54, 54, 54,
        55, 55, 55, 55, 55, 55, 55, 55, 55, 55,
        56, 56, 56, 56, 56, 56, 56, 56, 56, 56,
        57, 57, 57, 57, 57, 57, 57, 57, 57, 57
    )

    @PublishedApi
    @JvmStatic
    internal val DigitOnes = byteArrayOf(
        48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
        48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
        48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
        48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
        48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
        48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
        48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
        48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
        48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
        48, 49, 50, 51, 52, 53, 54, 55, 56, 57
    )

    /** @return number of bytes written (number of characters) */
    @JvmStatic
    fun putDigits(value: Float, output: MutableBuffer, offset: Int): Int =
        FloatToDecimal.putDecimal(output, offset, value)

    /** @return number of bytes written (number of characters) */
    @JvmStatic
    fun putDigits(value: Double, output: MutableBuffer, offset: Int): Int =
        DoubleToDecimal.putDecimal(output, offset, value)

    /** @return number of bytes written (number of characters) */
    @JvmStatic
    fun writeDigits(value: Float, writeByte: (Int) -> Unit): Int =
        FloatingDecimalToAscii.getThreadLocalInstance().writeDigits(value, writeByte)

    /** @return number of bytes written (number of characters) */
    @JvmStatic
    fun writeDigits(value: Double, writeByte: (Int) -> Unit): Int =
        FloatingDecimalToAscii.getThreadLocalInstance().writeDigits(value, writeByte)

    /** @return number of bytes written (number of digits) */
    fun putDigits(value: Long, output: MutableBuffer, offset: Int): Int {
        val buffer = FloatingDecimalToAscii.getThreadLocalInstance().buffer.array
        val charPos = prepareDigits(buffer, value)
        return writeBufferReversed(output, buffer, charPos, offset)
    }

    /** @return number of bytes written (number of digits) */
    fun putDigits(value: Int, output: MutableBuffer, offset: Int): Int {
        val buffer = FloatingDecimalToAscii.getThreadLocalInstance().buffer.array
        val charPos = prepareDigits(buffer, value)
        return writeBufferReversed(output, buffer, charPos, offset)
    }

    @JvmStatic
    fun toString(v: Float): String = FloatToDecimal.toString(v, null)

    @JvmStatic
    fun toString(v: Double): String = DoubleToDecimal.toString(v, null)

    @PublishedApi
    internal inline fun writeBufferReversed(writeByte: (Byte) -> Unit, source: ByteArray, charPos: Int): Int {
        var pos = charPos
        while (pos < source.size) {
            writeByte(source[pos])
            pos++
        }
        return pos - charPos
    }

    /** @return number of bytes written (number of digits) */
    inline fun writeDigits(value: Long, writeByte: (Byte) -> Unit): Int {
        val buffer = FloatingDecimalToAscii.getThreadLocalInstance().buffer.array
        val charPos = prepareDigits(buffer, value)
        return writeBufferReversed(writeByte, buffer, charPos)
    }

    /** @return number of bytes written (number of digits) */
    inline fun writeDigits(value: Int, writeByte: (Byte) -> Unit): Int {
        val buffer = FloatingDecimalToAscii.getThreadLocalInstance().buffer.array
        val charPos = prepareDigits(buffer, value)
        return writeBufferReversed(writeByte, buffer, charPos)
    }

    /** @return first char index of resulting string in [buffer] */
    @PublishedApi
    internal fun prepareDigits(buffer: ByteArray, value: Long): Int {
        var n = value
        if (n >= 0L) n = -n

        var charPos = buffer.size

        var r: Int
        while (n <= -2147483648L) {
            val q = n / 100L
            r = (q * 100L - n).toInt()
            n = q
            buffer[--charPos] = DigitOnes[r]
            buffer[--charPos] = DigitTens[r]
        }

        var q2: Int
        var i2 = n.toInt()
        while (i2 <= -100) {
            q2 = i2 / 100
            r = q2 * 100 - i2
            i2 = q2
            buffer[--charPos] = DigitOnes[r]
            buffer[--charPos] = DigitTens[r]
        }

        q2 = i2 / 10
        r = q2 * 10 - i2
        buffer[--charPos] = (48 + r).toByte()
        if (q2 < 0) {
            buffer[--charPos] = (48 - q2).toByte()
        }

        if (value < 0L) buffer[--charPos] = 45

        return charPos
    }

    /** @return first char index of resulting string in [buffer] */
    @PublishedApi
    internal fun prepareDigits(buffer: ByteArray, value: Int): Int {
        var n = value
        if (n >= 0L) n = -n

        var charPos = buffer.size

        var q: Int
        var r: Int
        while (n <= -100) {
            q = n / 100
            r = q * 100 - n
            n = q
            buffer[--charPos] = DigitOnes[r]
            buffer[--charPos] = DigitTens[r]
        }

        q = n / 10
        r = q * 10 - n
        buffer[--charPos] = (48 + r).toByte()
        if (q < 0) {
            buffer[--charPos] = (48 - q).toByte()
        }

        if (value < 0) buffer[--charPos] = 45

        return charPos
    }

    private fun writeBufferReversed(dest: OutputBuffer, source: ByteArray, charPos: Int, destOffset: Int): Int {
        var pos = charPos
        while (pos < source.size) {
            dest[destOffset + (pos - charPos)] = source[pos]
            pos++
        }
        return pos - charPos
    }
}