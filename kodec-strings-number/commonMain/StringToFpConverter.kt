package io.kodec

import kotlin.jvm.JvmStatic

/**
 * A deferred conversion result of sucessfully parsed decimal number.
 *
 * The purpose of this interface is to give you the ability to skip the
 * most expensive part of the conversion ([doubleValue]/[floatValue]).
 * This is the case when you don't need the actual conversion result,
 * but only a guarantee of syntactic correctness.
 */
interface StringToFpConverter {
    fun doubleValue(): Double
    fun floatValue(): Float

    companion object {
        @JvmStatic val PositiveInfinity: StringToFpConverter get() = PreparedStringToFpBuffer.POSITIVE_INFINITY
        @JvmStatic val NegativeInfinity: StringToFpConverter get() = PreparedStringToFpBuffer.NEGATIVE_INFINITY
        @JvmStatic val NaN: StringToFpConverter get() = PreparedStringToFpBuffer.NOT_A_NUMBER
        @JvmStatic val Zero: StringToFpConverter get() = PreparedStringToFpBuffer.POSITIVE_ZERO

        @JvmStatic fun wrap(float: Float, double: Double): StringToFpConverter = PreparedStringToFpBuffer(double, float)
    }
}