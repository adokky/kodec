package io.kodec

import karamel.utils.asInt
import kotlin.jvm.JvmStatic
import kotlin.math.max
import kotlin.math.min

/**
 * A buffered implementation of `ASCIIToBinaryConverter`.
 */
internal class ASCIIToBinaryBuffer(
    var isNegative: Boolean,
    var decExponent: Int,
    val digits: ByteArray,
    var nDigits: Int
) : ASCIIToBinaryConverter {
    constructor(): this(false, 0, ByteArray(30), 0)

    /**
     * Takes a FloatingDecimal, which we presumably just scanned in,
     * and finds out what its value is, as a double.
     *
     * AS A SIDE EFFECT, SET roundDir TO INDICATE PREFERRED
     * ROUNDING DIRECTION in case the result is really destined
     * for a single-precision float.
     */
    override fun doubleValue(): Double {
        val kDigits = min(nDigits.toDouble(), (FloatingDecimalToAscii.MAX_DECIMAL_DIGITS + 1).toDouble())
            .toInt()
        // convert the lead kDigits to a long integer.
        // (special performance hack: start to do it using int)
        var iValue = digits[0].asInt() - '0'.code
        val iDigits = min(kDigits.toDouble(), FloatingDecimalToAscii.INT_DECIMAL_DIGITS.toDouble())
            .toInt()
        for (i in 1 until iDigits) {
            iValue = iValue * 10 + digits[i].asInt() - '0'.code
        }
        var lValue = iValue.toLong()
        for (i in iDigits until kDigits) {
            lValue = lValue * 10L + (digits[i].asInt() - '0'.code).toLong()
        }
        var dValue = lValue.toDouble()
        var exp = decExponent - kDigits

        // lValue now contains a long integer with the value of
        // the first kDigits digits of the number.
        // dValue contains the (double) of the same.
        if (nDigits <= FloatingDecimalToAscii.MAX_DECIMAL_DIGITS) {
            // possibly an easy case.
            // We know that the digits can be represented
            // exactly. And if the exponent isn't too outrageous,
            // the whole thing can be done with one operation,
            // thus one rounding error.
            // Note that all our constructors trim all leading and
            // trailing zeros, so simple values (including zero)
            // will always end up here
            if (exp == 0 || dValue == 0.0) {
                return if (isNegative) -dValue else dValue // small floating integer
            } else if (exp >= 0) {
                if (exp <= MAX_SMALL_TEN) {
                    // Can get the answer with one operation,
                    // thus one roundoff.
                    val rValue = dValue * SMALL_10_POW[exp]
                    return if (isNegative) -rValue else rValue
                }
                val slop = FloatingDecimalToAscii.MAX_DECIMAL_DIGITS - kDigits
                if (exp <= MAX_SMALL_TEN + slop) {
                    // We can multiply dValue by 10^(slop)
                    // and it is still "small" and exact.
                    // Then we can multiply by 10^(exp-slop)
                    // with one rounding.
                    dValue *= SMALL_10_POW[slop]
                    val rValue = dValue * SMALL_10_POW[exp - slop]
                    return if (isNegative) -rValue else rValue
                }
                // Else we have a hard case with a positive exp.
            } else {
                if (exp >= -MAX_SMALL_TEN) {
                    // Can get the answer in one division.
                    val rValue = dValue / SMALL_10_POW[-exp]
                    return if (isNegative) -rValue else rValue
                }
                // Else we have a hard case with a negative exp.
            }
        }

        // Harder cases:
        // The sum of digits plus exponent is greater than
        // what we think we can do with one error.
        // Start by approximating the right answer by,
        // naively, scaling by powers of 10.
        if (exp > 0) {
            if (decExponent > FloatingDecimalToAscii.MAX_DECIMAL_EXPONENT + 1) {
                // Let's face it. This is going to be Infinity.
                return if (isNegative) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
            }
            if ((exp and 15) != 0) {
                dValue *= SMALL_10_POW[exp and 15]
            }
            if ((4.let { exp = exp shr it; exp }) != 0) {
                var j = 0
                while (exp > 1) {
                    if ((exp and 1) != 0) {
                        dValue *= BIG_10_POW[j]
                    }
                    j++
                    exp = exp shr 1
                }
                // The reason for the weird exp > 1 condition
                // in the above loop was so that the last multiply
                // would get unrolled. We handle it here.
                // It could overflow.
                var t = dValue * BIG_10_POW[j]
                if (t.isInfinite()) {
                    // It did overflow.
                    // Look more closely at the result.
                    // If the exponent is just one too large,
                    // then use the maximum finite as our estimate
                    // value. Else call the result infinity and punt it.
                    // I presume this could happen because
                    // rounding forces the result here to be
                    // an ULP or two larger than Double.MAX_VALUE.
                    t = dValue / 2.0
                    t *= BIG_10_POW[j]
                    if (t.isInfinite()) {
                        return if (isNegative) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
                    }
                    t = Double.MAX_VALUE
                }
                dValue = t
            }
        } else if (exp < 0) {
            exp = -exp
            if (decExponent < FloatingDecimalToAscii.MIN_DECIMAL_EXPONENT - 1) {
                // Let's face it. This is going to be zero.
                return if (isNegative) -0.0 else 0.0
            }
            if ((exp and 15) != 0) {
                dValue /= SMALL_10_POW[exp and 15]
            }
            if ((4.let { exp = exp shr it; exp }) != 0) {
                var j = 0
                while (exp > 1) {
                    if ((exp and 1) != 0) dValue *= TINY_10_POW[j]
                    j++
                    exp = exp shr 1
                }
                // The reason for the weird exp > 1 condition
                // in the above loop was so that the last multiply
                // would get unrolled. We handle it here.
                // It could underflow.
                var t = dValue * TINY_10_POW[j]
                if (t == 0.0) {
                    // It did underflow.
                    // Look more closely at the result.
                    // If the exponent is just one too small,
                    // then use the minimum finite as our estimate
                    // value. Else call the result 0.0 and punt it.
                    // I presume this could happen because
                    // rounding forces the result here to be
                    // an ULP or two less than Double.MIN_VALUE.
                    t = dValue * 2.0
                    t *= TINY_10_POW[j]
                    if (t == 0.0) return if (isNegative) -0.0 else 0.0
                    t = Double.MIN_VALUE
                }
                dValue = t
            }
        }

        // dValue is now approximately the result.
        // The hard part is adjusting it, by comparison
        // with FDBigInteger arithmetic.
        // Formulate the EXACT big-number result as
        // bigD0 * 10^exp
        if (nDigits > FloatingDecimalToAscii.MAX_NDIGITS) {
            nDigits = FloatingDecimalToAscii.MAX_NDIGITS + 1
            digits[FloatingDecimalToAscii.MAX_NDIGITS] = '1'.code.toByte()
        }
        var bigD0 = FDBigInteger(lValue, digits, kDigits, nDigits)
        exp = decExponent - nDigits

        var ieeeBits: Long = dValue.toRawBits() // IEEE-754 bits of double candidate
        val B5 = max(0.0, -exp.toDouble()).toInt() // powers of 5 in bigB, value is not modified inside correctionLoop
        val D5 = max(0.0, exp.toDouble()).toInt() // powers of 5 in bigD, value is not modified inside correctionLoop
        bigD0 = bigD0.multByPow52(D5, 0)
        bigD0.isImmutable = true
        // prevent bigD0 modification inside correctionLoop
        var bigD: FDBigInteger? = null
        var prevD2 = 0

        correctionLoop@ while (true) {
            // here ieeeBits can't be NaN, Infinity or zero
            var binexp = (ieeeBits ushr FloatingDecimalToAscii.EXP_SHIFT).toInt()
            var bigBbits = ieeeBits and Float64Consts.SIGNIF_BIT_MASK
            if (binexp > 0) {
                bigBbits = bigBbits or FloatingDecimalToAscii.FRACT_HOB
            } else { // Normalize denormalized numbers.
                val leadingZeros: Int = bigBbits.countLeadingZeroBits()
                val shift = leadingZeros - (63 - FloatingDecimalToAscii.EXP_SHIFT)
                bigBbits = bigBbits shl shift
                binexp = 1 - shift
            }
            binexp -= Float64Consts.EXP_BIAS
            val lowOrderZeros: Int = bigBbits.countTrailingZeroBits()
            bigBbits = bigBbits ushr lowOrderZeros
            val bigIntExp = binexp - FloatingDecimalToAscii.EXP_SHIFT + lowOrderZeros
            val bigIntNBits = FloatingDecimalToAscii.EXP_SHIFT + 1 - lowOrderZeros

            // Scale bigD, bigB appropriately for
            // big-integer operations.
            // Naively, we multiply by powers of ten
            // and powers of two. What we actually do
            // is keep track of the powers of 5 and
            // powers of 2 we would use, then factor out
            // common divisors before doing the work.
            var B2 = B5 // powers of 2 in bigB
            var D2 = D5 // powers of 2 in bigD
            var Ulp2: Int // powers of 2 in halfUlp.
            if (bigIntExp >= 0) {
                B2 += bigIntExp
            } else {
                D2 -= bigIntExp
            }
            Ulp2 = B2

            // shift bigB and bigD left by a number s. t.
            // halfUlp is still an integer.
            val hulpbias = if (binexp <= -Float64Consts.EXP_BIAS) {
                // This is going to be a denormalized number
                // (if not actually zero).
                // half an ULP is at 2^-(DoubleConsts.EXP_BIAS+EXP_SHIFT+1)
                binexp + lowOrderZeros + Float64Consts.EXP_BIAS
            } else {
                1 + lowOrderZeros
            }
            B2 += hulpbias
            D2 += hulpbias
            // if there are common factors of 2, we might just as well
            // factor them out, as they add nothing useful.
            val common2 = min(B2.toDouble(), min(D2.toDouble(), Ulp2.toDouble())).toInt()
            B2 -= common2
            D2 -= common2
            Ulp2 -= common2
            // do multiplications by powers of 5 and 2
            val bigB: FDBigInteger = FDBigInteger.valueOfMulPow52(bigBbits, B5, B2)
            if (bigD == null || prevD2 != D2) {
                bigD = bigD0.leftShift(D2)
                prevD2 = D2
            }

            // to recap:
            // bigB is the scaled-big-int version of our floating-point
            // candidate.
            // bigD is the scaled-big-int version of the exact value
            // as we understand it.
            // halfUlp is 1/2 an ulp of bigB, except for special cases
            // of exact powers of 2
            //
            // the plan is to compare bigB with bigD, and if the difference
            // is less than halfUlp, then we're satisfied. Otherwise,
            // use the ratio of difference to halfUlp to calculate a fudge
            // factor to add to the floating value, then go around again.
            var diff: FDBigInteger
            var cmpResult: Int
            val overvalue: Boolean
            if ((bigB.cmp(bigD).also { cmpResult = it }) > 0) {
                overvalue = true // our candidate is too big.
                diff = bigB.leftInplaceSub(bigD) // bigB is not user further - reuse
                if ((bigIntNBits == 1) && (bigIntExp > -Float64Consts.EXP_BIAS + 1)) {
                    // candidate is a normalized exact power of 2 and
                    // is too big (larger than Double.MIN_NORMAL). We will be subtracting.
                    // For our purposes, ulp is the ulp of the
                    // next smaller range.
                    Ulp2 -= 1
                    if (Ulp2 < 0) {
                        // rats. Cannot de-scale ulp this far.
                        // must scale diff in other direction.
                        Ulp2 = 0
                        diff = diff.leftShift(1)
                    }
                }
            } else if (cmpResult < 0) {
                overvalue = false // our candidate is too small.
                diff = bigD.rightInplaceSub(bigB) // bigB is not user further - reuse
            } else {
                // the candidate is exactly right!
                // this happens with surprising frequency
                break@correctionLoop
            }
            cmpResult = diff.cmpPow52(B5, Ulp2)
            if ((cmpResult) < 0) {
                // difference is small.
                // this is close enough
                break@correctionLoop
            } else if (cmpResult == 0) {
                // difference is exactly half an ULP
                // round to some other value maybe, then finish
                if ((ieeeBits and 1L) != 0L) { // half ties to even
                    ieeeBits += (if (overvalue) -1 else 1).toLong() // nextDown or nextUp
                }
                break@correctionLoop
            } else {
                // difference is non-trivial.
                // could scale addend by ratio of difference to
                // halfUlp here, if we bothered to compute that difference.
                // Most of the time ( I hope ) it is about 1 anyway.
                ieeeBits += (if (overvalue) -1 else 1).toLong() // nextDown or nextUp
                if (ieeeBits == 0L || ieeeBits == Float64Consts.EXP_BIT_MASK) { // 0.0 or Double.POSITIVE_INFINITY
                    break@correctionLoop  // oops. Fell off end of range.
                }
                continue  // try again.
            }
        }

        if (isNegative) {
            ieeeBits = ieeeBits or Float64Consts.SIGN_BIT_MASK
        }

        return Double.fromBits(ieeeBits)
    }

    /**
     * Takes a FloatingDecimal, which we presumably just scanned in,
     * and finds out what its value is, as a float.
     * This is distinct from doubleValue() to avoid the extremely
     * unlikely case of a double rounding error, wherein the conversion
     * to double has one rounding error, and the conversion of that double
     * to a float has another rounding error, IN THE WRONG DIRECTION,
     * ( because of the preference to a zero low-order bit ).
     */
    override fun floatValue(): Float {
        val kDigits = min(nDigits.toDouble(), (FloatingDecimalToAscii.SINGLE_MAX_DECIMAL_DIGITS + 1).toDouble()).toInt()

        // convert the lead kDigits to an integer.
        var iValue = digits[0].asInt() - '0'.code
        for (i in 1 until kDigits) {
            iValue = iValue * 10 + digits[i].asInt() - '0'.code
        }
        var fValue = iValue.toFloat()
        var exp = decExponent - kDigits

        // iValue now contains an integer with the value of
        // the first kDigits digits of the number.
        // fValue contains the (float) of the same.
        if (nDigits <= FloatingDecimalToAscii.SINGLE_MAX_DECIMAL_DIGITS) {
            // possibly an easy case.
            // We know that the digits can be represented
            // exactly. And if the exponent isn't too outrageous,
            // the whole thing can be done with one operation,
            // thus one rounding error.
            // Note that all our constructors trim all leading and
            // trailing zeros, so simple values (including zero)
            // will always end up here.
            if (exp == 0 || fValue == 0.0f) {
                return if (isNegative) -fValue else fValue // small floating integer
            } else if (exp >= 0) {
                if (exp <= SINGLE_MAX_SMALL_TEN) {
                    // Can get the answer with one operation,
                    // thus one round off.
                    fValue *= SINGLE_SMALL_10_POW[exp]
                    return if (isNegative) -fValue else fValue
                }
                val slop = FloatingDecimalToAscii.SINGLE_MAX_DECIMAL_DIGITS - kDigits
                if (exp <= SINGLE_MAX_SMALL_TEN + slop) {
                    // We can multiply fValue by 10^(slop)
                    // and it is still "small" and exact.
                    // Then we can multiply by 10^(exp-slop)
                    // with one rounding.
                    fValue *= SINGLE_SMALL_10_POW[slop]
                    fValue *= SINGLE_SMALL_10_POW[exp - slop]
                    return if (isNegative) -fValue else fValue
                }
                // Else we have a hard case with a positive exp.
            } else {
                if (exp >= -SINGLE_MAX_SMALL_TEN) {
                    // Can get the answer in one division.
                    fValue /= SINGLE_SMALL_10_POW[-exp]
                    return if (isNegative) -fValue else fValue
                }
                // Else we have a hard case with a negative exp.
            }
        } else if ((decExponent >= nDigits) && (nDigits + decExponent <= FloatingDecimalToAscii.MAX_DECIMAL_DIGITS)) {
            // In double-precision, this is an exact floating integer.
            // So we can compute to double, then shorten to float
            // with one round, and get the right answer.
            //
            // First, finish accumulating digits.
            // Then convert that integer to a double, multiply
            // by the appropriate power of ten, and convert to float.
            var lValue = iValue.toLong()
            for (i in kDigits until nDigits) {
                lValue = lValue * 10L + (digits[i].asInt() - '0'.code).toLong()
            }
            var dValue = lValue.toDouble()
            exp = decExponent - nDigits
            dValue *= SMALL_10_POW[exp]
            fValue = dValue.toFloat()
            return if (isNegative) -fValue else fValue
        }

        // Harder cases:
        // The sum of digits plus exponent is greater than
        // what we think we can do with one error.
        //
        // Start by approximating the right answer by,
        // naively, scaling by powers of 10.
        // Scaling uses doubles to avoid overflow/underflow.
        var dValue = fValue.toDouble()
        if (exp > 0) {
            if (decExponent > FloatingDecimalToAscii.SINGLE_MAX_DECIMAL_EXPONENT + 1) {
                // Let's face it. This is going to be
                // Infinity. Cut to the chase.
                return if (isNegative) Float.NEGATIVE_INFINITY else Float.POSITIVE_INFINITY
            }
            if ((exp and 15) != 0) {
                dValue *= SMALL_10_POW[exp and 15]
            }
            if ((4.let { exp = exp shr it; exp }) != 0) {
                var j = 0
                while (exp > 0) {
                    if ((exp and 1) != 0) dValue *= BIG_10_POW[j]
                    j++
                    exp = exp shr 1
                }
            }
        } else if (exp < 0) {
            exp = -exp
            if (decExponent < FloatingDecimalToAscii.SINGLE_MIN_DECIMAL_EXPONENT - 1) {
                // Let's face it. This is going to be
                // zero. Cut to the chase.
                return if (isNegative) -0.0f else 0.0f
            }
            if ((exp and 15) != 0) {
                dValue /= SMALL_10_POW[exp and 15]
            }
            if ((4.let { exp = exp shr it; exp }) != 0) {
                var j = 0
                while (exp > 0) {
                    if ((exp and 1) != 0) dValue *= TINY_10_POW[j]
                    j++
                    exp = exp shr 1
                }
            }
        }

        fValue = dValue.toFloat().coerceIn(Float.MIN_VALUE, Float.MAX_VALUE)

        // fValue is now approximately the result.
        // The hard part is adjusting it, by comparison
        // with FDBigInteger arithmetic.
        // Formulate the EXACT big-number result as
        // bigD0 * 10^exp
        if (nDigits > FloatingDecimalToAscii.SINGLE_MAX_NDIGITS) {
            nDigits = FloatingDecimalToAscii.SINGLE_MAX_NDIGITS + 1
            digits[FloatingDecimalToAscii.SINGLE_MAX_NDIGITS] = '1'.code.toByte()
        }
        var bigD0 = FDBigInteger(iValue.toLong(), digits, kDigits, nDigits)
        exp = decExponent - nDigits

        var ieeeBits: Int = fValue.toRawBits() // IEEE-754 bits of float candidate
        val B5 = max(0.0, -exp.toDouble()).toInt() // powers of 5 in bigB, value is not modified inside correctionLoop
        val D5 = max(0.0, exp.toDouble()).toInt() // powers of 5 in bigD, value is not modified inside correctionLoop
        bigD0 = bigD0.multByPow52(D5, 0)
        bigD0.isImmutable = true
        // prevent bigD0 modification inside correctionLoop
        var bigD: FDBigInteger? = null
        var prevD2 = 0

        correctionLoop@ while (true) {
            // here ieeeBits can't be NaN, Infinity or zero
            var binexp = ieeeBits ushr FloatingDecimalToAscii.SINGLE_EXP_SHIFT
            var bigBbits = ieeeBits and Float32Consts.SIGNIF_BIT_MASK
            if (binexp > 0) {
                bigBbits = bigBbits or FloatingDecimalToAscii.SINGLE_FRACT_HOB
            } else { // Normalize denormalized numbers.
                val leadingZeros: Int = bigBbits.countLeadingZeroBits()
                val shift = leadingZeros - (31 - FloatingDecimalToAscii.SINGLE_EXP_SHIFT)
                bigBbits = bigBbits shl shift
                binexp = 1 - shift
            }
            binexp -= Float32Consts.EXP_BIAS
            val lowOrderZeros: Int = bigBbits.countTrailingZeroBits()
            bigBbits = bigBbits ushr lowOrderZeros
            val bigIntExp = binexp - FloatingDecimalToAscii.SINGLE_EXP_SHIFT + lowOrderZeros
            val bigIntNBits = FloatingDecimalToAscii.SINGLE_EXP_SHIFT + 1 - lowOrderZeros

            // Scale bigD, bigB appropriately for
            // big-integer operations.
            // Naively, we multiply by powers of ten
            // and powers of two. What we actually do
            // is keep track of the powers of 5 and
            // powers of 2 we would use, then factor out
            // common divisors before doing the work.
            var B2 = B5 // powers of 2 in bigB
            var D2 = D5 // powers of 2 in bigD
            var Ulp2: Int // powers of 2 in halfUlp.
            if (bigIntExp >= 0) {
                B2 += bigIntExp
            } else {
                D2 -= bigIntExp
            }
            Ulp2 = B2
            // shift bigB and bigD left by a number s. t.
            // halfUlp is still an integer.
            val hulpbias: Int = if (binexp <= -Float32Consts.EXP_BIAS) {
                // This is going to be a denormalized number
                // (if not actually zero).
                // half an ULP is at 2^-(FloatConsts.EXP_BIAS+SINGLE_EXP_SHIFT+1)
                binexp + lowOrderZeros + Float32Consts.EXP_BIAS
            } else {
                1 + lowOrderZeros
            }
            B2 += hulpbias
            D2 += hulpbias
            // if there are common factors of 2, we might just as well
            // factor them out, as they add nothing useful.
            val common2 = min(B2.toDouble(), min(D2.toDouble(), Ulp2.toDouble())).toInt()
            B2 -= common2
            D2 -= common2
            Ulp2 -= common2
            // do multiplications by powers of 5 and 2
            val bigB: FDBigInteger = FDBigInteger.valueOfMulPow52(bigBbits.toLong(), B5, B2)
            if (bigD == null || prevD2 != D2) {
                bigD = bigD0.leftShift(D2)
                prevD2 = D2
            }
            // to recap:
            // bigB is the scaled-big-int version of our floating-point
            // candidate.
            // bigD is the scaled-big-int version of the exact value
            // as we understand it.
            // halfUlp is 1/2 an ulp of bigB, except for special cases
            // of exact powers of 2
            //
            // the plan is to compare bigB with bigD, and if the difference
            // is less than halfUlp, then we're satisfied. Otherwise,
            // use the ratio of difference to halfUlp to calculate a fudge
            // factor to add to the floating value, then go around again.
            var diff: FDBigInteger
            var cmpResult: Int
            val overvalue: Boolean
            if ((bigB.cmp(bigD).also { cmpResult = it }) > 0) {
                overvalue = true // our candidate is too big.
                diff = bigB.leftInplaceSub(bigD) // bigB is not user further - reuse
                if ((bigIntNBits == 1) && (bigIntExp > -Float32Consts.EXP_BIAS + 1)) {
                    // candidate is a normalized exact power of 2 and
                    // is too big (larger than Float.MIN_NORMAL). We will be subtracting.
                    // For our purposes, ulp is the ulp of the
                    // next smaller range.
                    Ulp2 -= 1
                    if (Ulp2 < 0) {
                        // rats. Cannot de-scale ulp this far.
                        // must scale diff in other direction.
                        Ulp2 = 0
                        diff = diff.leftShift(1)
                    }
                }
            } else if (cmpResult < 0) {
                overvalue = false // our candidate is too small.
                diff = bigD.rightInplaceSub(bigB) // bigB is not user further - reuse
            } else {
                // the candidate is exactly right!
                // this happens with surprising frequency
                break@correctionLoop
            }

            cmpResult = diff.cmpPow52(B5, Ulp2)
            if ((cmpResult) < 0) {
                // difference is small.
                // this is close enough
                break@correctionLoop
            } else if (cmpResult == 0) {
                // difference is exactly half an ULP
                // round to some other value maybe, then finish
                if ((ieeeBits and 1) != 0) { // half ties to even
                    ieeeBits += if (overvalue) -1 else 1 // nextDown or nextUp
                }
                break@correctionLoop
            } else {
                // difference is non-trivial.
                // could scale addend by ratio of difference to
                // halfUlp here, if we bothered to compute that difference.
                // Most of the time ( I hope ) it is about 1 anyway.
                ieeeBits += if (overvalue) -1 else 1 // nextDown or nextUp
                if (ieeeBits == 0 || ieeeBits == Float32Consts.EXP_BIT_MASK) { // 0.0 or Float.POSITIVE_INFINITY
                    break@correctionLoop  // oops. Fell off end of range.
                }
                continue  // try again.
            }
        }
        if (isNegative) ieeeBits = ieeeBits or Float32Consts.SIGN_BIT_MASK
        return Float.fromBits(ieeeBits)
    }

    companion object {
        /**
         * All the positive powers of 10 that can be
         * represented exactly in double/float.
         */
        @JvmStatic private val SMALL_10_POW = doubleArrayOf(
            1.0e0,
            1.0e1, 1.0e2, 1.0e3, 1.0e4, 1.0e5,
            1.0e6, 1.0e7, 1.0e8, 1.0e9, 1.0e10,
            1.0e11, 1.0e12, 1.0e13, 1.0e14, 1.0e15,
            1.0e16, 1.0e17, 1.0e18, 1.0e19, 1.0e20,
            1.0e21, 1.0e22
        )

        @JvmStatic private val SINGLE_SMALL_10_POW = floatArrayOf(
            1.0e0f,
            1.0e1f, 1.0e2f, 1.0e3f, 1.0e4f, 1.0e5f,
            1.0e6f, 1.0e7f, 1.0e8f, 1.0e9f, 1.0e10f
        )

        @JvmStatic private val BIG_10_POW = doubleArrayOf(1e16, 1e32, 1e64, 1e128, 1e256)
        @JvmStatic private val TINY_10_POW = doubleArrayOf(1e-16, 1e-32, 1e-64, 1e-128, 1e-256)

        @JvmStatic private val MAX_SMALL_TEN = SMALL_10_POW.size - 1
        @JvmStatic private val SINGLE_MAX_SMALL_TEN = SINGLE_SMALL_10_POW.size - 1
    }
}