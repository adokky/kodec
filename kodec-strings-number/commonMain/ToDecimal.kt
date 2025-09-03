package io.kodec

import io.kodec.buffers.MutableBuffer
import io.kodec.buffers.OutputBuffer
import kotlin.jvm.JvmStatic

internal abstract class ToDecimal {
    protected fun putChar(dst: OutputBuffer, index: Int, c: Int): Int {
        dst[index] = c.toByte()
        return index + 1
    }

    protected fun putChar(dst: OutputBuffer, index: Int, c: Char): Int =
        putChar(dst, index, c.code)

    protected fun putDigit(dst: OutputBuffer, index: Int, d: Int): Int =
        putChar(dst, index, ('0'.code + d).toByte().toInt())

    protected fun put8Digits(dst: OutputBuffer, index: Int, m: Int): Int {
        /*
         * Left-to-right digits extraction:
         * algorithm 1 in [3], with b = 10, k = 8, n = 28.
         */
        put8DigitsLatin1(dst, index, m)
        return index + 8
    }

    protected fun removeTrailingZeroes(str: MutableBuffer, index: Int): Int {
        var index = index
        while (str[index - 1] == '0'.code) {
            --index
        }
        /* ... but do not remove the one directly to the right of '.' */
        if (str[index - 1] == '.'.code) {
            ++index
        }
        return index
    }

    protected fun putSpecial(dst: OutputBuffer, index: Int, type: Int): Int {
        val s = specialBytes(type)
        dst.putBytes(index, s)
        return s.size
    }

    protected companion object {
        /* Used for left-to-tight digit extraction */
        const val MASK_28: Int = (1 shl 28) - 1

        const val NON_SPECIAL: Int = 0 shl 8
        const val PLUS_ZERO: Int = 1 shl 8
        const val MINUS_ZERO: Int = 2 shl 8
        const val PLUS_INF: Int = 3 shl 8
        const val MINUS_INF: Int = 4 shl 8
        const val NAN: Int = 5 shl 8

        private fun put8DigitsLatin1(dst: OutputBuffer, index: Int, m: Int) {
            var y: Int = y(m)
            for (i in 0 .. 7) {
                val t = 10 * y
                dst[index + i] = ('0'.code + (t ushr 28)).toByte()
                y = t and MASK_28
            }
        }

        @JvmStatic
        fun y(a: Int): Int {
            /*
             * Algorithm 1 in [3] needs computation of
             *     floor((a + 1) 2^n / b^k) - 1
             * with a < 10^8, b = 10, k = 8, n = 28.
             * Noting that
             *     (a + 1) 2^n <= 10^8 2^28 < 10^17
             * For n = 17, m = 8 the table in section 10 of [1] leads to:
             */
            return multiplyHigh((a + 1).toLong().shl(28), 193428131138340668L)
                .ushr(20)
                .toInt() - 1
        }

        @JvmStatic
        fun special(type: Int): String = when (type) {
            PLUS_ZERO -> "0.0"
            MINUS_ZERO -> "-0.0"
            PLUS_INF -> "Infinity"
            MINUS_INF -> "-Infinity"
            else -> "NaN"
        }

        private val PLUS_ZERO_BYTES  = special(PLUS_ZERO).encodeToByteArray()
        private val MINUS_ZERO_BYTES = special(MINUS_ZERO).encodeToByteArray()
        private val PLUS_INF_BYTES   = special(PLUS_INF).encodeToByteArray()
        private val MINUS_INF_BYTES  = special(MINUS_INF).encodeToByteArray()
        private val NAN_BYTES        = special(0).encodeToByteArray()

        @JvmStatic
        fun specialBytes(type: Int): ByteArray = when (type) {
            PLUS_ZERO -> PLUS_ZERO_BYTES
            MINUS_ZERO -> MINUS_ZERO_BYTES
            PLUS_INF -> PLUS_INF_BYTES
            MINUS_INF -> MINUS_INF_BYTES
            else -> NAN_BYTES
        }
    }
}