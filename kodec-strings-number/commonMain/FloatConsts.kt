package io.kodec

import io.kodec.buffers.asBuffer
import kotlin.jvm.JvmStatic

internal object Float32Consts {
    /**
     * The number of bits used to represent a `float` value
     */
    private const val SIZE: Int = 32

    /**
     * The number of logical bits in the significand of a
     * `float` number, including the implicit bit.
     */
    const val SIGNIFICAND_WIDTH: Int = 24

    /**
     * Bias used in representing a `float` exponent.
     */
    const val EXP_BIAS: Int = (1 shl (SIZE - SIGNIFICAND_WIDTH - 1)) - 1 // 127

    /**
     * Bit mask to isolate the sign bit of a `float`.
     */
    const val SIGN_BIT_MASK: Int = 1 shl (SIZE - 1)

    /**
     * Bit mask to isolate the exponent field of a `float`.
     */
    const val EXP_BIT_MASK: Int = ((1 shl (SIZE - SIGNIFICAND_WIDTH)) - 1) shl (SIGNIFICAND_WIDTH - 1)

    /**
     * Bit mask to isolate the significand field of a `float`.
     */
    const val SIGNIF_BIT_MASK: Int = (1 shl (SIGNIFICAND_WIDTH - 1)) - 1

    const val INFINITY_REP = "Infinity"
    @JvmStatic val INFINITY_REP_ARRAY = INFINITY_REP.encodeToByteArray().asBuffer()
    const val INFINITY_LENGTH = INFINITY_REP.length
    const val NAN_REP = "NaN"
    const val NAN_LENGTH = NAN_REP.length

    // approximately ceil( log2( long5pow[i] ) )
    @JvmStatic
    val N_5_BITS = intArrayOf(0, 3, 5, 7, 10, 12, 14, 17, 19, 21, 24, 26, 28, 31, 33, 35, 38, 40, 42, 45, 47, 49, 52, 54, 56, 59, 61)
}

internal object Float64Consts {
    /**
     * The number of logical bits in the significand of a
     * `double` number, including the implicit bit.
     */
    const val SIGNIFICAND_WIDTH: Int = 53

    /**
     * Bias used in representing a `double` exponent.
     */
    const val EXP_BIAS: Int = (1 shl (64 - SIGNIFICAND_WIDTH - 1)) - 1 // 1023

    /**
     * Bit mask to isolate the sign bit of a `double`.
     */
    const val SIGN_BIT_MASK: Long = 1L shl (64 - 1)

    /**
     * Bit mask to isolate the exponent field of a `double`.
     */
    const val EXP_BIT_MASK: Long = ((1L shl (64 - SIGNIFICAND_WIDTH)) - 1) shl (SIGNIFICAND_WIDTH - 1)

    /**
     * Bit mask to isolate the significand field of a `double`.
     */
    const val SIGNIF_BIT_MASK: Long = (1L shl (SIGNIFICAND_WIDTH - 1)) - 1
}