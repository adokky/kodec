package io.kodec

import kotlin.jvm.JvmStatic

/**
 * A deferred conversion result of successfully parsed decimal number.
 *
 * The purpose of this interface is to allow skipping the most expensive part of the conversion
 * ([doubleValue]/[floatValue]), which is useful when you only need to verify syntactic correctness
 * without requiring the actual numeric value.
 */
sealed interface StringToFpConverter {
    fun doubleValue(): Double
    fun floatValue(): Float

    companion object {
        @JvmStatic val POSITIVE_INFINITY: StringToFpConverter get() = PreparedStringToFpBuffer.POSITIVE_INFINITY
        @JvmStatic val NEGATIVE_INFINITY: StringToFpConverter get() = PreparedStringToFpBuffer.NEGATIVE_INFINITY
        @JvmStatic val NAN: StringToFpConverter get() = PreparedStringToFpBuffer.NOT_A_NUMBER
        @JvmStatic val POSITIVE_ZERO: StringToFpConverter get() = PreparedStringToFpBuffer.POSITIVE_ZERO
        @JvmStatic val NEGATIVE_ZERO: StringToFpConverter get() = PreparedStringToFpBuffer.NEGATIVE_ZERO

        @JvmStatic fun wrap(float: Float): StringToFpConverter = PreparedStringToFpBuffer(float.toDouble(), float)
        @JvmStatic fun wrap(double: Double): StringToFpConverter = PreparedStringToFpBuffer(double, double.toFloat())
    }
}