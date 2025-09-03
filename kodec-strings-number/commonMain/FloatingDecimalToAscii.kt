package io.kodec

import io.kodec.buffers.ArrayBuffer
import karamel.utils.ThreadLocal

/**
 * A class for converting between ASCII and decimal representations of a single
 * or double precision floating point number.
 *
 * For full details about this code see the following references:
 *
 * [1] Giulietti, "The Schubfach way to render doubles",
 *     https://drive.google.com/file/d/1gp5xv4CAa78SVgCeWfGqqI4FfYYYuNFb
 *
 * [2] IEEE Computer Society, "IEEE Standard for Floating-Point Arithmetic"
 *
 * [3] Bouvier & Zimmermann, "Division-Free Binary-to-Decimal Conversion"
 *
 * Divisions are avoided altogether for the benefit of those architectures
 * that do not provide specific machine instructions or where they are slow.
 * This is discussed in section 10 of [1].
 */
@PublishedApi
internal class FloatingDecimalToAscii private constructor() {
    companion object {
        private val tl = ThreadLocal<FloatingDecimalToAscii> { FloatingDecimalToAscii() }
        fun getThreadLocalInstance(): FloatingDecimalToAscii = tl.get()
    }

    val buffer = ArrayBuffer(DoubleToDecimal.MAX_CHARS)

    fun appendTo(value: Float, output: Appendable) {
        val size = FloatToDecimal.putDecimal(buffer, index = 0, value)
        outputBytes(size, output)
    }

    fun appendTo(value: Double, output: Appendable) {
        val size = DoubleToDecimal.putDecimal(buffer, index = 0, value)
        outputBytes(size, output)
    }

    fun writeDigits(value: Float, writeByte: (Int) -> Unit): Int {
        val size = FloatToDecimal.putDecimal(buffer, index = 0, value)
        outputBytes(size, writeByte)
        return size
    }

    fun writeDigits(value: Double, writeByte: (Int) -> Unit): Int {
        val size = DoubleToDecimal.putDecimal(buffer, index = 0, value)
        outputBytes(size, writeByte)
        return size
    }

    private inline fun iterateBytes(size: Int, body: (Int) -> Unit) {
        val arr = buffer.array
        val start = buffer.start
        for (i in start ..< start + size) {
            body(arr[i].toInt())
        }
    }

    private fun outputBytes(size: Int, writeByte: (Int) -> Unit) {
        iterateBytes(size) {
            writeByte(it)
        }
    }

    private fun outputBytes(size: Int, output: Appendable) {
        iterateBytes(size) {
            output.append(it.toChar())
        }
    }
}