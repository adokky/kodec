package io.kodec

/**
 * Estimate decimal exponent. (If it is small-ish,
 * we could double-check.)
 *
 * First, scale the mantissa bits such that 1 <= d2 < 2.
 * We are then going to estimate
 * log10(d2) ~=~  (d2-1.5)/1.5 + log(1.5)
 * and so we can estimate
 * log10(d) ~=~ log10(d2) + binExp * log10(2)
 * take the floor and call it decExp.
 */
internal fun estimateDecExp(fractBits: Long, binExp: Int): Int {
    val d2: Double = Double.fromBits(FloatingDecimalToAscii.EXP_ONE or (fractBits and Float64Consts.SIGNIF_BIT_MASK))
    val d = (d2 - 1.5) * 0.289529654 + 0.176091259 + (binExp.toDouble() * 0.301029995663981)
    val dBits: Long = d.toRawBits() //can't be NaN here so use raw
    val exponent: Int = ((dBits and Float64Consts.EXP_BIT_MASK) shr FloatingDecimalToAscii.EXP_SHIFT).toInt() - Float64Consts.EXP_BIAS
    val isNegative = (dBits and Float64Consts.SIGN_BIT_MASK) != 0L // discover sign
    return if (exponent in 0..51) { // hot path
        val mask: Long = Float64Consts.SIGNIF_BIT_MASK shr exponent
        val r = (((dBits and Float64Consts.SIGNIF_BIT_MASK) or FloatingDecimalToAscii.FRACT_HOB) shr (FloatingDecimalToAscii.EXP_SHIFT - exponent)).toInt()
        if (isNegative) (if (((mask and dBits) == 0L)) -r else -r - 1) else r
    } else if (exponent < 0) {
        (if (((dBits and Float64Consts.SIGN_BIT_MASK.inv()) == 0L)) 0 else (if (isNegative) -1 else 0))
    } else { //if (exponent >= 52)
        d.toInt()
    }
}

/**
 * Calculates `insignificantDigitsForPow2(v) == insignificantDigits(1L<<v></v>)`
 */
internal fun insignificantDigitsForPow2(p2: Int): Int =
    if (p2 !in 2 until insignificantDigitsNumber.size) 0
    else insignificantDigitsNumber[p2]

/**
 * If `insignificant == (1L << ixd)`
 * i = insignificantDigitsNumber[[idx]] is the same as:
 *
 *    int i;
 *    for ( i = 0; insignificant >= 10L; i++ )
 *    insignificant /= 10L;
 */
private val insignificantDigitsNumber = intArrayOf(
    0, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 3,
    4, 4, 4, 5, 5, 5, 6, 6, 6, 6, 7, 7, 7,
    8, 8, 8, 9, 9, 9, 9, 10, 10, 10, 11, 11, 11,
    12, 12, 12, 12, 13, 13, 13, 14, 14, 14,
    15, 15, 15, 15, 16, 16, 16, 17, 17, 17,
    18, 18, 18, 19
)