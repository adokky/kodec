package io.kodec

import kotlin.jvm.JvmField

internal class PreparedASCIIToBinaryBuffer(
    private val doubleVal: Double,
    private val floatVal: Float
) : ASCIIToBinaryConverter {
    override fun doubleValue(): Double = doubleVal
    override fun floatValue(): Float = floatVal

    companion object {
        @JvmField val POSITIVE_INFINITY = PreparedASCIIToBinaryBuffer(Double.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        @JvmField val NEGATIVE_INFINITY = PreparedASCIIToBinaryBuffer(Double.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)
        @JvmField val NOT_A_NUMBER = PreparedASCIIToBinaryBuffer(Double.NaN, Float.NaN)
        @JvmField val POSITIVE_ZERO = PreparedASCIIToBinaryBuffer(0.0, 0.0f)
        @JvmField val NEGATIVE_ZERO = PreparedASCIIToBinaryBuffer(-0.0, -0.0f)
    }
}