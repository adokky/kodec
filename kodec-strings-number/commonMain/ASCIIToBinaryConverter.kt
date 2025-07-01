package io.kodec

import kotlin.jvm.JvmStatic

/**
 * A converter which can process an ASCII `String` representation
 * of a single or double precision floating point value into a
 * `float` or a `double`.
 */
interface ASCIIToBinaryConverter {
    fun doubleValue(): Double
    fun floatValue(): Float

    companion object {
        @JvmStatic val PositiveInfinity: ASCIIToBinaryConverter get() = PreparedASCIIToBinaryBuffer.POSITIVE_INFINITY
        @JvmStatic val NegativeInfinity: ASCIIToBinaryConverter get() = PreparedASCIIToBinaryBuffer.NEGATIVE_INFINITY
        @JvmStatic val NaN: ASCIIToBinaryConverter get() = PreparedASCIIToBinaryBuffer.NOT_A_NUMBER
    }
}