package io.kodec

import kotlin.jvm.JvmField

internal data class PreparedStringToFpBuffer(
    private val doubleVal: Double,
    private val floatVal: Float
) : StringToFpConverter {
    override fun doubleValue(): Double = doubleVal
    override fun floatValue(): Float = floatVal

    override fun toString(): String = doubleVal.toString()

    companion object {
        @JvmField val POSITIVE_INFINITY = PreparedStringToFpBuffer(Double.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        @JvmField val NEGATIVE_INFINITY = PreparedStringToFpBuffer(Double.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)
        @JvmField val NOT_A_NUMBER = PreparedStringToFpBuffer(Double.NaN, Float.NaN)
        @JvmField val POSITIVE_ZERO = PreparedStringToFpBuffer(0.0, 0.0f)
        @JvmField val NEGATIVE_ZERO = PreparedStringToFpBuffer(-0.0, -0.0f)
    }
}