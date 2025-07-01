package io.kodec

import io.kodec.buffers.ArrayBuffer
import io.kodec.buffers.MutableBuffer
import karamel.utils.ThreadLocal
import kotlin.jvm.JvmStatic
import kotlin.math.max
import kotlin.math.min

/**
 * A class for converting between ASCII and decimal representations of a single
 * or double precision floating point number.
 */
@PublishedApi
internal class FloatingDecimalToAscii private constructor() {
    companion object {
        // Constants of the implementation;
        // most are IEEE-754 related.
        // (There are more really boring constants at the end.)
        const val EXP_SHIFT: Int = Float64Consts.SIGNIFICAND_WIDTH - 1
        const val FRACT_HOB: Long = (1L shl EXP_SHIFT) // assumed High-Order bit
        const val EXP_ONE: Long = (Float64Consts.EXP_BIAS.toLong()) shl EXP_SHIFT // exponent of 1.0
        const val MAX_SMALL_BIN_EXP: Int = 62
        const val MIN_SMALL_BIN_EXP: Int = -(63 / 3)
        const val MAX_DECIMAL_DIGITS: Int = 15
        const val MAX_DECIMAL_EXPONENT: Int = 308
        const val MIN_DECIMAL_EXPONENT: Int = -324
        const val BIG_DECIMAL_EXPONENT: Int = 324 // i.e. abs(MIN_DECIMAL_EXPONENT)
        const val MAX_NDIGITS: Int = 1100
        const val SINGLE_EXP_SHIFT: Int = Float32Consts.SIGNIFICAND_WIDTH - 1
        const val SINGLE_FRACT_HOB: Int = 1 shl SINGLE_EXP_SHIFT
        const val SINGLE_MAX_DECIMAL_DIGITS: Int = 7
        const val SINGLE_MAX_DECIMAL_EXPONENT: Int = 38
        const val SINGLE_MIN_DECIMAL_EXPONENT: Int = -45
        const val SINGLE_MAX_NDIGITS: Int = 200
        const val INT_DECIMAL_DIGITS: Int = 9

        @JvmStatic
        private val B2AC_POSITIVE_INFINITY = Float32Consts.INFINITY_REP.toCharArray()
        @JvmStatic
        private val B2AC_NEGATIVE_INFINITY = "-${Float32Consts.INFINITY_REP}".toCharArray()
        @JvmStatic
        private val B2AC_NOT_A_NUMBER = Float32Consts.NAN_REP.toCharArray()
        @JvmStatic
        private val B2AC_POSITIVE_ZERO = charArrayOf('0')
        @JvmStatic
        private val B2AC_NEGATIVE_ZERO = charArrayOf('-', '0')

        private val tl = ThreadLocal<FloatingDecimalToAscii> { FloatingDecimalToAscii() }
        fun getThreadLocalInstance(): FloatingDecimalToAscii = tl.get()
    }

    // Temporal state ==========================================================

    private var isNegative: Boolean = false
    private var decimalExponent: Int = 0
    private var firstDigitIndex = 0
    private var nDigits = 0
    private val digits = CharArray(20)
    val tempBuffer: ArrayBuffer = ArrayBuffer(30)

    // The fields below provide additional information about the result of
    // the binary to decimal digits conversion done in dtoa() and roundup()
    // methods. They are changed if needed by those two methods.
    //
    // True if the dtoa() binary to decimal conversion was exact.
    private var exactDecimalConversion = false

    // True if the result of the binary to decimal conversion was rounded-up
    // at the end of the conversion process, i.e. roundUp() method was called.
    private var decimalDigitsRoundedUp = false

    // ==========================================================================

    fun appendTo(d: Double, output: Appendable) {
        prepare(d) { constResult ->
            for (c in constResult) output.append(c)
            return
        }

        appendCharsInto(output)
    }

    fun appendTo(f: Float, output: Appendable) {
        prepare(f) { constResult ->
            for (c in constResult) output.append(c)
            return
        }

        appendCharsInto(output)
    }

    private fun appendCharsInto(output: Appendable) {
        val len = getChars(tempBuffer)
        repeat(len) { i ->
            output.append(tempBuffer[i].toChar())
        }
    }

    fun putDigits(f: Float, output: MutableBuffer, offset: Int): Int {
        prepare(f) { constResult ->
            for ((i, c) in constResult.withIndex()) output[offset + i] = c.code
            return constResult.size
        }
        return getChars(output, offset)
    }

    fun putDigits(d: Double, output: MutableBuffer, offset: Int): Int {
        prepare(d) { constResult ->
            for ((i, c) in constResult.withIndex()) output[offset + i] = c.code
            return constResult.size
        }
        return getChars(output, offset)
    }

    fun writeDigits(f: Float, writeByte: (Int) -> Unit): Int {
        prepare(f) { constResult ->
            for (c in constResult) writeByte(c.code)
            return constResult.size
        }
        var i = 0
        getChars { writeByte(it); i++ }
        return i
    }

    fun writeDigits(d: Double, writeByte: (Int) -> Unit): Int {
        prepare(d) { constResult ->
            for (c in constResult) writeByte(c.code)
            return constResult.size
        }
        var i = 0
        getChars { writeByte(it); i++ }
        return i
    }

    private fun reset(isNegative: Boolean = false) {
        this.isNegative = isNegative
        decimalExponent = 0
        firstDigitIndex = 0
        digits.fill(0.toChar())
        nDigits = 0
    }

    private inline fun prepare(d: Double, outputSpecialCase: (CharArray) -> Unit) {
        val dBits: Long = d.toRawBits()
        val isNegative = (dBits and Float64Consts.SIGN_BIT_MASK) != 0L
        var fractBits = dBits and Float64Consts.SIGNIF_BIT_MASK
        var binExp = ((dBits and Float64Consts.EXP_BIT_MASK) shr EXP_SHIFT).toInt()
        // Discover obvious special cases of NaN and Infinity.
        if (binExp == (Float64Consts.EXP_BIT_MASK shr EXP_SHIFT).toInt()) {
            outputSpecialCase(if (fractBits == 0L) {
                if (isNegative) B2AC_NEGATIVE_INFINITY else B2AC_POSITIVE_INFINITY
            } else {
                B2AC_NOT_A_NUMBER
            })
            return
        }
        // Finish unpacking.
        // Normalize denormalized numbers.
        // Insert assumed high-order bit for normalized numbers.
        // Subtract exponent bias.
        val nSignificantBits: Int
        if (binExp == 0) {
            if (fractBits == 0L) {
                // not a denorm, just a 0!
                outputSpecialCase(if (isNegative) B2AC_NEGATIVE_ZERO else B2AC_POSITIVE_ZERO)
                return
            }
            val leadingZeros: Int = fractBits.countLeadingZeroBits()
            val shift = leadingZeros - (63 - EXP_SHIFT)
            fractBits = fractBits shl shift
            binExp = 1 - shift
            nSignificantBits = 64 - leadingZeros // recall binExp is  - shift count.
        } else {
            fractBits = fractBits or FRACT_HOB
            nSignificantBits = EXP_SHIFT + 1
        }
        binExp -= Float64Consts.EXP_BIAS

        reset(isNegative)
        dtoa(binExp, fractBits, nSignificantBits)
    }

    internal inline fun prepare(
        f: Float,
        outputSpecialCase: (CharArray) -> Unit
    ) {
        val fBits: Int = f.toRawBits()
        val isNegative = (fBits and Float32Consts.SIGN_BIT_MASK) != 0
        var fractBits = fBits and Float32Consts.SIGNIF_BIT_MASK
        var binExp = (fBits and Float32Consts.EXP_BIT_MASK) shr SINGLE_EXP_SHIFT
        // Discover obvious special cases of NaN and Infinity.
        if (binExp == (Float32Consts.EXP_BIT_MASK shr SINGLE_EXP_SHIFT)) {
            outputSpecialCase(if (fractBits.toLong() == 0L) {
                if (isNegative) B2AC_NEGATIVE_INFINITY else B2AC_POSITIVE_INFINITY
            } else {
                B2AC_NOT_A_NUMBER
            })
            return
        }
        // Finish unpacking.
        // Normalize denormalized numbers.
        // Insert assumed high-order bit for normalized numbers.
        // Subtract exponent bias.
        val nSignificantBits: Int
        if (binExp == 0) {
            if (fractBits == 0) {
                // not a denorm, just a 0!
                outputSpecialCase(if (isNegative) B2AC_NEGATIVE_ZERO else B2AC_POSITIVE_ZERO)
                return
            }
            val leadingZeros: Int = fractBits.countLeadingZeroBits()
            val shift = leadingZeros - (31 - SINGLE_EXP_SHIFT)
            fractBits = fractBits shl shift
            binExp = 1 - shift
            nSignificantBits = 32 - leadingZeros // recall binExp is  - shift count.
        } else {
            fractBits = fractBits or SINGLE_FRACT_HOB
            nSignificantBits = SINGLE_EXP_SHIFT + 1
        }
        binExp -= Float32Consts.EXP_BIAS

        reset(isNegative)
        dtoa(binExp, (fractBits.toLong()) shl (EXP_SHIFT - SINGLE_EXP_SHIFT), nSignificantBits)
    }

    /**
     * This is the easy subcase --
     * all the significant bits, after scaling, are held in [lvalue].
     * negSign and decExponent tell us what processing and scaling
     * has already been done. Exceptional cases have already been
     * stripped out.
     * In particular:
     * [lvalue] is a finite number (not Inf, nor NaN)
     * [lvalue] > 0L (not zero, nor negative).
     *
     * The only reason that we develop the digits here, rather than
     * calling on [Long.toString] is that we can do it a little faster,
     * and besides want to treat trailing 0s specially. If [Long.toString]
     * changes, we should re-evaluate this strategy!
     */
    private fun developLongDigits(lvalue: Long, insignificantDigits: Int) {
        var decExp = 0
        var lval = lvalue
        if (insignificantDigits != 0) {
            // Discard non-significant low-order bits, while rounding,
            // up to insignificant value.
            // 10^i == 5^i * 2^i;
            val pow10: Long = FDBigInteger.LONG_5_POW[insignificantDigits] shl insignificantDigits
            val residue = lval % pow10
            lval /= pow10
            decExp += insignificantDigits
            if (residue >= (pow10 shr 1)) {
                // round up based on the low-order bits we're discarding
                lval++
            }
        }
        var digitno = digits.size - 1
        var c: Int
        if (lval <= Int.MAX_VALUE) {
            // even easier subcase!
            // can do int arithmetic rather than long!
            var ivalue = lval.toInt()
            c = ivalue % 10
            ivalue /= 10
            while (c == 0) {
                decExp++
                c = ivalue % 10
                ivalue /= 10
            }
            while (ivalue != 0) {
                digits[digitno--] = (c + '0'.code).toChar()
                decExp++
                c = ivalue % 10
                ivalue /= 10
            }
            digits[digitno] = (c + '0'.code).toChar()
        } else {
            // same algorithm as above (same bugs, too)
            // but using long arithmetic.
            c = (lval % 10L).toInt()
            lval /= 10L
            while (c == 0) {
                decExp++
                c = (lval % 10L).toInt()
                lval /= 10L
            }
            while (lval != 0L) {
                digits[digitno--] = (c + '0'.code).toChar()
                decExp++
                c = (lval % 10L).toInt()
                lval /= 10
            }
            digits[digitno] = (c + '0'.code).toChar()
        }
        this.decimalExponent = decExp + 1
        this.firstDigitIndex = digitno
        this.nDigits = digits.size - digitno
    }

    fun dtoa(binExp: Int, fractBits: Long, nSignificantBits: Int) {
        var frBits = fractBits
        // Examine number. Determine if it is an easy case,
        // which we can do pretty trivially using float/long conversion,
        // or whether we must do real work.
        val tailZeros: Int = frBits.countTrailingZeroBits()

        // number of significant bits of fractBits;
        val nFractBits = EXP_SHIFT + 1 - tailZeros

        // reset flags to default values as dtoa() does not always set these
        // flags and a prior call to dtoa() might have set them to incorrect
        // values with respect to the current state.
        decimalDigitsRoundedUp = false
        exactDecimalConversion = false

        // number of significant bits to the right of the point.
        val nTinyBits = max(0.0, (nFractBits - binExp - 1).toDouble()).toInt()
        if (binExp in MIN_SMALL_BIN_EXP..MAX_SMALL_BIN_EXP) {
            // Look more closely at the number to decide if,
            // with scaling by 10^nTinyBits, the result will fit in
            // a long.
            if ((nTinyBits < FDBigInteger.LONG_5_POW.size) && ((nFractBits + Float32Consts.N_5_BITS[nTinyBits]) < 64)) {
                // We can do this:
                // take the fraction bits, which are normalized.
                // (a) nTinyBits == 0: Shift left or right appropriately
                //     to align the binary point at the extreme right, i.e.
                //     where a long int point is expected to be. The integer
                //     result is easily converted to a string.
                // (b) nTinyBits > 0: Shift right by EXP_SHIFT-nFractBits,
                //     which effectively converts to long and scales by
                //     2^nTinyBits. Then multiply by 5^nTinyBits to
                //     complete the scaling. We know this won't overflow
                //     because we just counted the number of bits necessary
                //     in the result. The integer you get from this can
                //     then be converted to a string pretty easily.
                if (nTinyBits == 0) {
                    val insignificant = if (binExp <= nSignificantBits) 0 else {
                        insignificantDigitsForPow2(binExp - nSignificantBits - 1)
                    }
                    frBits = if (binExp >= EXP_SHIFT) {
                        frBits shl (binExp - EXP_SHIFT)
                    } else {
                        frBits ushr (EXP_SHIFT - binExp)
                    }
                    developLongDigits(frBits, insignificant)
                    return
                }
            }
        }
        // This is the hard case. We are going to compute large positive
        // integers B and S and integer decExp, s.t.
        //      d = ( B / S )// 10^decExp
        //      1 <= B / S < 10
        // Obvious choices are:
        //      decExp = floor( log10(d) )
        //      B      = d// 2^nTinyBits// 10^max( 0, -decExp )
        //      S      = 10^max( 0, decExp)// 2^nTinyBits
        // (noting that nTinyBits has already been forced to non-negative)
        // I am also going to compute a large positive integer
        //      M      = (1/2^nSignificantBits)// 2^nTinyBits// 10^max( 0, -decExp )
        // i.e. M is (1/2) of the ULP of d, scaled like B.
        // When we iterate through dividing B/S and picking off the
        // quotient bits, we will know when to stop when the remainder
        // is <= M.
        //
        // We keep track of powers of 2 and powers of 5.
        var decExp = estimateDecExp(frBits, binExp)
        var B2: Int
        var S2: Int
        var M2: Int // powers of 2
        val M5: Int // powers of 5

        val B5 = max(0.0, -decExp.toDouble()).toInt() // powers of 2 and powers of 5, respectively, in B
        B2 = B5 + nTinyBits + binExp

        val S5 = max(0.0, decExp.toDouble()).toInt() // powers of 2 and powers of 5, respectively, in S
        S2 = S5 + nTinyBits

        M5 = B5
        M2 = B2 - nSignificantBits

        // the long integer fractBits contains the (nFractBits) interesting
        // bits from the mantissa of d ( hidden 1 added if necessary) followed
        // by (EXP_SHIFT+1-nFractBits) zeros. In the interest of compactness,
        // I will shift out those zeros before turning fractBits into a
        // FDBigInteger. The resulting whole number will be
        //      d * 2^(nFractBits-1-binExp).
        frBits = frBits ushr tailZeros
        B2 -= nFractBits - 1
        val common2factor = min(B2.toDouble(), S2.toDouble()).toInt()
        B2 -= common2factor
        S2 -= common2factor
        M2 -= common2factor

        // HACK!! For exact powers of two, the next smallest number
        // is only half as far away as we think (because the meaning of
        // ULP changes at power-of-two bounds) for this reason, we
        // hack M2. Hope this works.
        if (nFractBits == 1) M2 -= 1

        if (M2 < 0) {
            // since we cannot scale M down far enough, we must scale the other values up.
            B2 -= M2
            S2 -= M2
            M2 = 0
        }
        // Construct, Scale, iterate.
        var ndigit: Int
        var low: Boolean
        var high: Boolean
        val lowDigitDifference: Long
        var q: Int

        // Detect the special cases where all the numbers we are about
        // to compute will fit in int or long integers.
        // In these cases, we will avoid doing FDBigInteger arithmetic.
        // We use the same algorithms, except that we "normalize"
        // our FDBigIntegers before iterating. This is to make division easier,
        // as it makes our fist guess (quotient of high-order words)
        // more accurate!
        //
        // Some day, we'll write a stopping test that takes
        // account of the asymmetry of the spacing of floating-point
        // numbers below perfect powers of 2
        // 26 Sept 96 is not that day.
        // So we use a symmetric test.
        //
        // binary digits needed to represent B, approx.
        val Bbits = nFractBits + B2 + (if ((B5 < Float32Consts.N_5_BITS.size)) Float32Consts.N_5_BITS[B5] else (B5 * 3))

        // binary digits needed to represent 10*S, approx.
        val tenSbits = S2 + 1 + (if (((S5 + 1) < Float32Consts.N_5_BITS.size)) Float32Consts.N_5_BITS[S5 + 1] else ((S5 + 1) * 3))
        if (Bbits < 64 && tenSbits < 64) {
            if (Bbits < 32 && tenSbits < 32) {
                // wa-hoo! They're all ints!
                var b: Int = (frBits.toInt() * FDBigInteger.SMALL_5_POW[B5]) shl B2
                val s: Int = FDBigInteger.SMALL_5_POW[S5] shl S2
                var m: Int = FDBigInteger.SMALL_5_POW[M5] shl M2
                val tens = s * 10

                // Unroll the first iteration. If our decExp estimate
                // was too high, our first quotient will be zero. In this
                // case, we discard it and decrement decExp.
                ndigit = 0
                q = b / s
                b = 10 * (b % s)
                m *= 10
                low = (b < m)
                high = (b + m > tens)
                if ((q == 0) && !high) {
                    // oops. Usually ignore leading zero.
                    decExp--
                } else {
                    digits[ndigit++] = ('0'.code + q).toChar()
                }

                // HACK! Java spec sez that we always have at least
                // one digit after the . in either F- or E-form output.
                // Thus, we will need more than one digit if we're using E-form
                if (decExp < -3 || decExp >= 8) {
                    low = false
                    high = low
                }
                while (!low && !high) {
                    q = b / s
                    b = 10 * (b % s)
                    m *= 10
                    if (m > 0L) {
                        low = (b < m)
                        high = (b + m > tens)
                    } else {
                        // HACK! m might overflow!
                        // in this case, it is certainly > b, which won't.
                        // And b+m > tens, too, since that has overflowed either!
                        low = true
                        high = true
                    }
                    digits[ndigit++] = ('0'.code + q).toChar()
                }
                lowDigitDifference = ((b shl 1) - tens).toLong()
                exactDecimalConversion = (b == 0)
            } else {
                // still good! they're all longs!
                var b: Long = (frBits * FDBigInteger.LONG_5_POW[B5]) shl B2
                val s: Long = FDBigInteger.LONG_5_POW[S5] shl S2
                var m: Long = FDBigInteger.LONG_5_POW[M5] shl M2
                val tens = s * 10L
                // Unroll the first iteration. If our decExp estimate
                // was too high, our first quotient will be zero. In this
                // case, we discard it and decrement decExp.
                ndigit = 0
                q = (b / s).toInt()
                b = 10L * (b % s)
                m *= 10L
                low = (b < m)
                high = (b + m > tens)
                if ((q == 0) && !high) {
                    // oops. Usually ignore leading zero.
                    decExp--
                } else {
                    digits[ndigit++] = ('0'.code + q).toChar()
                }
                // HACK! Java spec sez that we always have at least
                // one digit after the . in either F- or E-form output.
                // Thus, we will need more than one digit if we're using E-form
                if (decExp < -3 || decExp >= 8) {
                    low = false
                    high = low
                }
                while (!low && !high) {
                    q = (b / s).toInt()
                    b = 10 * (b % s)
                    m *= 10
                    if (m > 0L) {
                        low = (b < m)
                        high = (b + m > tens)
                    } else {
                        // HACK! m might overflow!
                        // in this case, it is certainly > b, which won't.
                        // And b+m > tens, too, since that has overflowed either!
                        low = true
                        high = true
                    }
                    digits[ndigit++] = ('0'.code + q).toChar()
                }
                lowDigitDifference = (b shl 1) - tens
                exactDecimalConversion = (b == 0L)
            }
        } else {
            // We really must do FDBigInteger arithmetic.
            // Fist, construct our FDBigInteger initial values.
            var Sval = FDBigInteger.valueOfPow52(S5, S2)
            val shiftBias: Int = Sval.getNormalizationBias()
            Sval = Sval.leftShift(shiftBias) // normalize so that division works better

            var Bval = FDBigInteger.valueOfMulPow52(frBits, B5, B2 + shiftBias)
            var Mval = FDBigInteger.valueOfPow52(M5 + 1, M2 + shiftBias + 1)

            val tenSval = FDBigInteger.valueOfPow52(S5 + 1, S2 + shiftBias + 1) //Sval.mult( 10 );

            // Unroll the first iteration. If our decExp estimate
            // was too high, our first quotient will be zero. In this
            // case, we discard it and decrement decExp.
            ndigit = 0
            q = Bval.quoRemIteration(Sval)
            low = (Bval.cmp(Mval) < 0)
            high = tenSval.addAndCmp(Bval, Mval) <= 0

            if ((q == 0) && !high) {
                // oops. Usually ignore leading zero.
                decExp--
            } else {
                digits[ndigit++] = ('0'.code + q).toChar()
            }

            // HACK! Java spec sez that we always have at least
            // one digit after the . in either F- or E-form output.
            // Thus, we will need more than one digit if we're using
            // E-form
            if (decExp < -3 || decExp >= 8) {
                low = false
                high = low
            }
            while (!low && !high) {
                q = Bval.quoRemIteration(Sval)
                Mval = Mval.multBy10() //Mval = Mval.mult( 10 );
                low = (Bval.cmp(Mval) < 0)
                high = tenSval.addAndCmp(Bval, Mval) <= 0
                digits[ndigit++] = ('0'.code + q).toChar()
            }
            if (high && low) {
                Bval = Bval.leftShift(1)
                lowDigitDifference = Bval.cmp(tenSval).toLong()
            } else {
                lowDigitDifference = 0L // this here only for flow analysis!
            }
            exactDecimalConversion = (Bval.cmp(FDBigInteger.ZERO) == 0)
        }
        this.decimalExponent = decExp + 1
        this.firstDigitIndex = 0
        this.nDigits = ndigit

        if (!high) return
        // Last digit gets rounded based on stopping condition.
        if (low) {
            if (lowDigitDifference == 0L) {
                // it's a tie!
                // choose based on which digits we like.
                if ((digits[firstDigitIndex + nDigits - 1].code and 1) != 0) {
                    roundup()
                }
            } else if (lowDigitDifference > 0) {
                roundup()
            }
        } else {
            roundup()
        }
    }

    // Add one to the least significant digit.
    // In the unlikely event there is a carry-out, deal with it.
    // Assert that this will only happen where there
    // is only one digit, e.g. (float)1e-44 seems to do it.
    private fun roundup() {
        var i = (firstDigitIndex + nDigits - 1)
        var q = digits[i].code
        if (q == '9'.code) {
            while (q == '9'.code && i > firstDigitIndex) {
                digits[i] = '0'
                q = digits[--i].code
            }
            if (q == '9'.code) {
                // carryout! High-order 1, rest 0s, larger exp.
                decimalExponent += 1
                digits[firstDigitIndex] = '1'
                return
            }
            // else fall through.
        }
        digits[i] = (q + 1).toChar()
        decimalDigitsRoundedUp = true
    }

    private fun getChars(output: MutableBuffer, offset: Int = 0): Int {
        var i = offset
        getChars { char -> output[i++] = char }
        return i - offset
    }

    private inline fun getChars(putChar: (Int) -> Unit) {
        if (isNegative) putChar('-'.code)

        if (decimalExponent in 1..7) {
            // print digits.digits
            var charLength = min(nDigits.toDouble(), decimalExponent.toDouble()).toInt()
            for (i in 0 until charLength) putChar(digits[firstDigitIndex + i].code)
            if (charLength < decimalExponent) {
                charLength = decimalExponent - charLength
                repeat(charLength) { putChar('0'.code) }
                putChar('.'.code)
                putChar('0'.code)
            } else {
                putChar('.'.code)
                if (charLength < nDigits) {
                    for (i in 0 until (nDigits - charLength)) putChar(digits[firstDigitIndex + charLength + i].code)
                } else {
                    putChar('0'.code)
                }
            }
        } else if (decimalExponent in -2..0) {
            putChar('0'.code)
            putChar('.'.code)
            repeat(-decimalExponent) { putChar('0'.code) }
            for (i in 0 until nDigits) putChar(digits[firstDigitIndex + i].code)
        } else {
            putChar(digits[firstDigitIndex].code)
            putChar('.'.code)
            if (nDigits > 1) {
                for (i in 0 until (nDigits - 1)) putChar(digits[firstDigitIndex + 1 + i].code)
            } else {
                putChar('0'.code)
            }

            putChar('E'.code)
            var e: Int = if (decimalExponent <= 0) {
                putChar('-'.code)
                -decimalExponent + 1
            } else {
                decimalExponent - 1
            }

            // decExponent has 1, 2, or 3, digits
            if (e <= 9) {
                putChar(e + '0'.code)
            } else if (e <= 99) {
                putChar(e / 10 + '0'.code)
                putChar(e % 10 + '0'.code)
            } else {
                putChar(e / 100 + '0'.code)
                e %= 100
                putChar(e / 10 + '0'.code)
                putChar(e % 10 + '0'.code)
            }
        }
    }
}