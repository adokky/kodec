package io.kodec

import io.kodec.FDBigInteger.Companion.valueOfMulPow52
import io.kodec.FloatingDecimalParsing.MAX_DIGITS
import io.kodec.MathUtils.flog2pow10
import io.kodec.MathUtils.g1
import io.kodec.MathUtils.pow10
import io.kodec.buffers.Buffer

/**
 * A class for converting between ASCII and decimal representations of a single
 * or double precision floating point number
 *
 * The mathematical value x of an instance is
 *      ±<0.d_1...d_n> 10^e
 * where d_i = d[i-1] - '0' (0 < i ≤ n) is the i-th digit.
 * It is assumed that d_1 > 0.
 * isNegative denotes the - sign.
 */
internal class ASCIIToBinaryBuffer: StringToFpConverter {
    private val d: ByteArray = ByteArray(MAX_DIGITS)
    private var isNegative: Boolean = false
    private var e: Int = 0
    private var n: Int = 0

    private val lhs = FDBigInteger()

    /* Assumes n ≤ 19 and returns a decimal prefix of f as an unsigned long. */
    private fun toLong(n: Int): Long {
        var f: Long = 0
        for (i in 0..<n) {
            f = 10 * f + (d[i] - '0'.code.toByte())
        }
        return f
    }

    override fun doubleValue(): Double {
        /*
         * As described above, the magnitude of the mathematical value is
         *      x = <0.d_1...d_n> 10^e = <d_1...d_n> 10^(e-n) = f 10^ep
         * where f = <d_1...d_n> and ep = e - n are integers.
         *
         * Let r_e denote the roundTiesToEven rounding.
         * This method returns ±r_e(x).
         */

        /* Filter out extremely small or extremely large x. */

        if (e <= DoubleToDecimal.E_THR_Z) {
            /* Test cases: "0.9e-324", "3e-500" */
            return signed(0.0)
        }
        if (e >= DoubleToDecimal.E_THR_I) {
            /* Test cases: "0.1e310", "4e500" */
            return signed(Double.POSITIVE_INFINITY)
        }

        /*
         * Attempt some fast paths before resorting to higher precision.
         * Here, let P = Double.PRECISION = 53.
         *
         * Below, fl is an unsigned long, thus we require n ≤ 19 because
         * 10^19 < 2^64 < 10^20.
         */
        val n = this.n
        val ep = e - n
        var v: Double
        val m: Int = minOf(n, MathUtils.N)
        var fl = toLong(m) // unsigned
        if (n <= MathUtils.N && 0 <= ep && e <= MathUtils.N) {
            /*
             * Here, n ≤ 19, hence f = fl < 10^19.
             * Since e = n + ep and 0 ≤ ep ∧ n + ep ≤ 19 we see that
             * x = f 10^ep < 10^n 10^ep = 10^(n+ep) ≤ 10^19.
             * Thus, x = fl 10^ep fits in an unsigned long as well.
             * If its most significant bit is 0, the long is non-negative.
             * Otherwise, fl ≥ 2^63, so there's room for P precision bits,
             * +1 rounding bit, +1 sticky bit.
             * In both cases, correct rounding is achieved as below.
             * All integer x < 10^19 are covered here.
             */
            /*
             * Test cases:
             *      for fl < 2^63: "1", "2.34000e2", "9.223e18";
             *      for fl ≥ 2^63: "9.876e18", "9223372036854776833" (this
             *          is 2^63 + 2^10 + 1, rounding up due to sticky bit),
             *          "9223372036854776832" (this is 2^63 + 2^10, halfway
             *          value rounding down to even);
             */
            fl *= pow10(ep) // 0 ≤ ep < 19
            v = if (fl >= 0) fl.toDouble() else 2.0 * (fl ushr 1 or (fl and 1L))
            return signed(v)
        }

        if (n <= FLOG_10_MAX_LONG && -MAX_SMALL_TEN <= ep) {
            v = fl.toDouble()
            /*
             * Here, -22 ≤ ep.
             * Further, fl < 10^18, so fl is an exact double iff
             * (long) v == fl holds.
             * If fl is not an exact double, resort to higher precision.
             */
            val isExact = v.toLong() == fl
            if (isExact && ep <= MAX_SMALL_TEN) {
                /*
                 * Here, -22 ≤ ep ≤ 22, so 10^|ep| is an exact double.
                 * The product or quotient below operate on exact doubles,
                 * so the result is correctly rounded.
                 */
                /*
                 * Test cases:
                 *      for ep < 0: "1.23", "0.000234";
                 *      for ep > 0: "3.45e23", "576460752303423616e20" (the
                 *          significand is 2^59 + 2^7, an exact double);
                 */
                v = if (ep >= 0) v * SMALL_10_POW[ep] else v / SMALL_10_POW[-ep]
                return signed(v)
            }

            /*
             * Here, fl < 10^18 is not an exact double, or ep > 22.
             * If fl is not an exact double, resort to higher precision.
             */
            if (isExact) {  // v and fl are mathematically equal.
                /*
                 * Here, ep > 22.
                 * We have f = fl = v.
                 * Note that 2^P = 9007199254740992 has 16 digits.
                 * If f does not start with 9 let ef = 16 - n, otherwise
                 * let ef = 15 - n.
                 * If ef < 0 then resort to higher precision.
                 * Otherwise, if f does not start with 9 we have n ≤ 16,
                 * so f 10^ef < 9 10^(n-1) 10^ef = 9 10^15 < 2^P.
                 * If f starts with 9 we have n ≤ 15, hence f 10^ef <
                 * 10^n 10^ef = 10^15 < 2^P.
                 *
                 * Hence, when ef ≥ 0 and ep - ef ≤ 22 we know that
                 * fl 10^ep = (fl 10^ef) 10^(ep-ef), with fl, (fl 10^ef),
                 * and 10^(ep-ef) all exact doubles.
                 */
                val ef = (if (d[0] < '9'.code.toByte()) MAX_DEC_DIGITS + 1 else MAX_DEC_DIGITS) - n
                if (ef >= 0 && ep - ef <= MAX_SMALL_TEN) {
                    /*
                     * Test cases:
                     *      f does not start with 9: "1e37", "8999e34";
                     *      f starts with 9: "0.9999e36", "0.9876e37";
                     */
                    /* Rely on left-to-right evaluation. */
                    @Suppress("ReplaceWithOperatorAssignment")
                    v = v * SMALL_10_POW[ef] * SMALL_10_POW[ep - ef]
                    return signed(v)
                }
            }
        }

        /*
         * Here, the above fast paths have failed to return.
         * Force ll, lh in [10^(N-1), 10^N] to have more high order bits.
         */
        var ll = fl // unsigned
        val lh: Long // unsigned
        if (n <= MathUtils.N) {  // ll = f
            ll *= pow10(MathUtils.N - n)
            lh = ll
        } else {  // ll is an N digits long prefix of f
            lh = ll + 1
        }
        val el = e - MathUtils.N
        /*
         * We now have
         *      x = f 10^ep
         *      ll 10^el ≤ x ≤ lh 10^el
         *      2^59 < 10^(N-1) ≤ ll ≤ lh ≤ 10^N < 2^64
         *
         * Rather than rounding x directly, which requires full precision
         * arithmetic, approximate x as follows.
         * Let integers g and r such that (see comments in MathUtils)
         *      (g - 1) 2^r ≤ 10^el < g 2^r
         * and split g into the lower 63 bits g0 and the higher bits g1:
         *      g = g1 2^63 + g0
         * where
         *      2^62 < g1 + 1 < 2^63, 0 < g0 < 2^63
         * We have
         *      g - 1 = g1 2^63 + g0 - 1 ≥ g1 2^63
         *      g = g1 2^63 + g0 < g1 2^63 + 2^63 = (g1 + 1) 2^63
         * Let
         *      nl = ll g1          nh = lh (g1 + 1)
         * These lead to
         *      nl 2^(r+63) ≤ x < nh 2^(r+63)
         * Let
         *      v = r_e(nl 2^(r+63))        vh = r_e(nh 2^(r+63))
         * If v = vh then r_e(x) = v.
         *
         * We also have
         *      2^121 = 2^59 2^62 < nl < nh < 2^64 2^63 = 2^127
         * Therefore, each of nl and nh fits in two longs.
         * Split them into the lower 64 bits and the higher bits.
         *      nl = nl1 2^64 + nl0     2^57 ≤ nl1 < 2^63
         *      nh = nh1 2^64 + nh0     2^57 ≤ nh1 < 2^63
         * Let bl and bh be the bitlength of nl1 and nh1, resp.
         * Both bl and bh lie in the interval [58, 63], and all of nl1, nh1,
         * nl, and nh are in the normal range of double.
         * As nl ≤ nh ≤ nl + 2 ll, and as ll < 2^64, then either bh = bl,
         * or more rarely bh = bl + 1.
         *
         * As mentioned above, if v = vh then r_e(x) = v.
         * Rather than rounding nl 2^(r+63), nh 2^(r+63) boundaries directly,
         * first round nl and nh to obtain doubles wl and wh, resp.
         *      wl = r_e(nl)        wh = r_e(nh)
         * Note that both wl and wh are normal doubles.
         *
         * Assume wl = wh.
         * There's a good chance that v = scalb(wl, r + 63) holds.
         * In fact, if x ≥ MIN_NORMAL then it can be (tediously) shown that
         * v = scalb(wl, r + 63) holds, even when v overflows.
         * If x < MIN_NORMAL, and since wl is normal and v ≤ MIN_NORMAL,
         * the precision might be lowered, so scalb(wl, r + 63) might incur
         * two rounding errors and could slightly differ from v.
         *
         * It is costly to precisely determine whether x ≥ MIN_NORMAL.
         * However, bl + r > MIN_EXPONENT - 127 implies x ≥ MIN_NORMAL,
         * and bh + r ≤ MIN_EXPONENT - 127 entails x < MIN_NORMAL.
         * Finally, when bl + r ≤ MIN_EXPONENT - 127 < bh + r we see that
         * bl + r = MIN_EXPONENT - 127 and bh = bl + 1 must hold.
         *
         * As noted, nh ≤ nl + 2 ll.
         * This means
         *      nh1 ≤ nh 2^(-64) ≤ (nl + 2 ll) 2^(-64) < (nl1 + 1) + 2
         * and thus
         *      nh1 ≤ nl1 + 2
         */
        val rp = flog2pow10(el) + 2 // r + 127
        val g1 = g1(el)
        val nl1: Long = unsignedMultiplyHigh(ll, g1)
        val nl0 = ll * g1
        val nh1: Long = unsignedMultiplyHigh(lh, g1 + 1)
        val nh0 = lh * (g1 + 1)
        val bl: Int = Long.SIZE_BITS - nl1.countLeadingZeroBits()
        if (bl + rp > Float64Consts.MIN_EXPONENT) {  // implies x is normal
            /*
             * To round nl we need its most significant P bits, the rounding
             * bit immediately to the right, and an indication (sticky bit)
             * of whether there are "1" bits following the rounding bit.
             * The sticky bit can be placed anywhere after the rounding bit.
             * Since bl ≥ 58, the P = 53 bits, the rounding bit, and space
             * for the sticky bit are all located in nl1.
             *
             * When nl0 = 0, the indication of whether there are "1" bits
             * to the right of the rounding bit is already contained in nl1.
             * Rounding nl to wl is the same as rounding nl1 to ul and then
             * multiplying this by 2^64.
             * that is, given wl = r_e(nl), ul = r_e(nl1), we get
             * wl = scalb(ul, 64).
             * The same holds for nh, wh, nh1, and uh.
             * So, if ul = uh then wl = wh, thus v = scalb(ul, r + 127).
             *
             * When nl1 ≠ 0, there are indeed "1" bits to the right of the
             * rounding bit.
             * We force the rightmost bit of nl1 to 1, obtaining nl1'.
             * Then, again, rounding nl to wl is the same as rounding nl1'
             * to ul and multiplying this by 2^64.
             * Analogously for nh, wh, nh1, and uh.
             * Again, if ul = uh then wl = wh, thus v = scalb(ul, r + 127).
             *
             * Since nh1 ≤ nl1 + 2, then either uh = ul or uh = nextUp(ul).
             * This means that when ul ≠ uh then
             *      v ≤ r_e(x) ≤ nextUp(v)
             */
            val ul = (nl1 or (if (nl0 != 0L) 1 else 0).toLong()).toDouble()
            val uh = (nh1 or (if (nh0 != 0L) 1 else 0).toLong()).toDouble()
            v = scalb(ul, rp)
            if (ul == uh || v == Double.POSITIVE_INFINITY) {
                /*
                 * Test cases:
                 *      for ll = lh ∧ ul = uh: "1.2e-200", "2.3e100";
                 *      for ll ≠ lh ∧ ul = uh: "1.2000000000000000003e-200",
                 *          "2.3000000000000000004e100";
                 *      for ll = lh ∧ v = ∞: "5.249320425370670463e308";
                 *      for ll ≠ lh ∧ v = ∞: "5.2493204253706704633e308";
                 */
                return signed(v)
            }
        } else {
            val bh: Int = Long.SIZE_BITS - nh1.countLeadingZeroBits()
            if (bh + rp <= Float64Consts.MIN_EXPONENT) {  // implies x is subnormal
                /*
                 * We need to reduce the precision to avoid double rounding
                 * issues.
                 * Shifting to the right while keeping room for the rounding
                 * and the sticky bit is one way to go.
                 * Other than that, the reasoning is similar to the above case.
                 */
                val sh = DoubleToDecimal.Q_MIN - rp // shift distance
                val sbMask = -1L ushr 1 - sh

                val nl1p = nl1 ushr sh
                var rb = nl1 ushr sh - 1
                var sb = (if ((nl1 and sbMask or nl0) != 0L) 1 else 0).toLong()
                var corr = rb and (sb or nl1p) and 1L
                val ul = (nl1p + corr).toDouble()

                val nh1p = nh1 ushr sh
                rb = nh1 ushr sh - 1
                sb = (if ((nh1 and sbMask or nh0) != 0L) 1 else 0).toLong()
                corr = rb and (sb or nh1p) and 1L
                val uh = (nh1p + corr).toDouble()
                v = scalb(ul, rp + sh)
                if (ul == uh) {
                    /*
                     * Test cases:
                     *      for ll = lh: "1.2e-320";
                     *      for ll ≠ lh: "1.2000000000000000003e-320";
                     */
                    return signed(v)
                }
            } else {
                /*
                 * Here, bl + r ≤ MIN_EXPONENT - 127 < bh + r.
                 * As mentioned before, this means bh = bl + 1 and
                 * rp = MIN_EXPONENT - bl.
                 * As nh1 ≤ nl1 + 2, nl1 ≥ 2^57, bh = bl + 1 happens only if
                 * the most significant P + 2 bits in nl1 are all "1" bits,
                 * so wl = r_e(nl) = r_e(nh) = wh = 2^(bl+64), and
                 * thus v = vh = 2^(bl+127) = 2^MIN_EXPONENT = MIN_NORMAL.
                 */
                /*
                 * Test cases:
                 *      for ll = lh: "2.225073858507201383e-308"
                 *      for ll ≠ lh: "2.2250738585072013831e-308"
                 */
                return signed(Float64Consts.MIN_NORMAL)
            }
        }

        /*
         * Measurements show that the failure rate of the above fast paths
         * on the outcomes of Double.toString() on uniformly distributed
         * double bit patterns is around 0.04%.
         *
         * Here, v ≤ r_e(x) ≤ nextUp(v), with v = c 2^q (c, q are as in
         * IEEE-754 2019).
         *
         * Let vr = v + ulp(v)/2 = (c + 1/2) 2^q, the number halfway between
         * v and nextUp(v).
         * With cr = (2 c + 1), qr = q - 1 we get vr = cr 2^qr.
         */
        val bits: Long = v.toRawBits()
        val be = ((bits and Float64Consts.EXP_BIT_MASK) ushr Float64Consts.SIGNIFICAND_WIDTH - 1).toInt()
        val qr: Int = (be - (Float64Consts.EXP_BIAS + Float64Consts.SIGNIFICAND_WIDTH - 1) - (if (be != 0) 1 else 0))
        val cr = 2 * (bits and Float64Consts.SIGNIF_BIT_MASK or (if (be != 0) DoubleToDecimal.C_MIN else 0)) + 1

        /*
         * The test vr ⋚ x is equivalent to cr 2^qr ⋚ f 10^ep.
         * This is in turn equivalent to one of 4 cases, where all exponents
         * are non-negative:
         *      ep ≥ 0 ∧ ep ≥ qr:                     cr ⋚ f 5^ep 2^(ep-qr)
         *      ep ≥ 0 ∧ ep < qr:           cr 2^(qr-ep) ⋚ f 5^ep
         *      ep < 0 ∧ ep ≥ qr:             cr 5^(-ep) ⋚ f 2^(ep-qr)
         *      ep < 0 ∧ ep < qr:   cr 5^(-ep) 2^(qr-ep) ⋚ f
         */
        val lhs = valueOfMulPow52(cr, maxOf(-ep, 0), maxOf(qr - ep, 0), lhs.makeMutable())
        val rhs = FDBigInteger(fl, d, m, n).multByPow52(maxOf(ep, 0), maxOf(ep - qr, 0))
        val cmp = lhs.cmp(rhs)
        v = Double.fromBits(
            when {
                cmp < 0 -> bits + 1
                cmp > 0 -> bits
                else -> bits + (bits and 1L)
            }
        )
        return signed(v)
    }

    override fun floatValue(): Float {
        /* For details not covered here, see comments in doubleValue(). */
        if (e <= E_THR_Z[BINARY_32_IX]) return signed(0.0f)
        if (e >= E_THR_I[BINARY_32_IX]) return signed(Float.POSITIVE_INFINITY)
        val n = this.n
        val ep = e - n
        var v: Float
        val m: Int = minOf(n, MathUtils.N)
        var fl = toLong(m)
        if (n <= MathUtils.N && 0 <= ep && e <= MathUtils.N) {
            fl *= pow10(ep) // 0 ≤ ep < 19
            v = if (fl >= 0) fl.toFloat() else 2.0f * (fl ushr 1 or (fl and 1L))
            return signed(v)
        }
        if (n <= FLOG_10_MAX_LONG && -SINGLE_MAX_SMALL_TEN <= ep) {
            v = fl.toFloat()
            val isExact = v.toLong() == fl
            if (isExact && ep <= SINGLE_MAX_SMALL_TEN) {
                v = if (ep >= 0) v * SINGLE_SMALL_10_POW[ep] else v / SINGLE_SMALL_10_POW[-ep]
                return signed(v)
            }
            /*
             * The similar case in doubleValue() where fl is exact and
             * ep is somewhat larger than MAX_SMALL_TEN is already covered
             * above for float.
             */
        }
        var ll = fl
        val lh: Long
        if (n <= MathUtils.N) {
            ll *= pow10(MathUtils.N - n)
            lh = ll
        } else {
            lh = ll + 1
        }
        val el = e - MathUtils.N
        val rp = flog2pow10(el) + 2
        val g1 = g1(el)
        val nl1: Long = unsignedMultiplyHigh(ll, g1)
        val nl0 = ll * g1
        val nh1: Long = unsignedMultiplyHigh(lh, g1 + 1)
        val nh0 = lh * (g1 + 1)
        val bl: Int = Long.SIZE_BITS - nl1.countLeadingZeroBits()
        if (bl + rp > Float32Consts.MIN_EXPONENT) {
            val ul = (nl1 or (if (nl0 != 0L) 1 else 0).toLong()).toFloat()
            val uh = (nh1 or (if (nh0 != 0L) 1 else 0).toLong()).toFloat()
            v = scalb(ul, rp)
            if (ul == uh || v == Float.POSITIVE_INFINITY) return signed(v)
        } else {
            val bh: Int = Long.SIZE_BITS - nh1.countLeadingZeroBits()
            if (bh + rp <= Float32Consts.MIN_EXPONENT) {
                val sh = FloatToDecimal.Q_MIN - rp
                val sbMask = -1L ushr 1 - sh

                val nl1p = nl1 ushr sh
                var rb = nl1 ushr sh - 1
                var sb = (if ((nl1 and sbMask or nl0) != 0L) 1 else 0).toLong()
                var corr = rb and (sb or nl1p) and 1L
                val ul = (nl1p + corr).toFloat()

                val nh1p = nh1 ushr sh
                rb = nh1 ushr sh - 1
                sb = (if ((nh1 and sbMask or nh0) != 0L) 1 else 0).toLong()
                corr = rb and (sb or nh1p) and 1L
                val uh = (nh1p + corr).toFloat()
                v = scalb(ul, rp + sh)
                if (ul == uh) return signed(v)
            } else {
                return signed(Float32Consts.MIN_NORMAL)
            }
        }
        val bits: Int = v.toRawBits()
        val be = (bits and Float32Consts.EXP_BIT_MASK) ushr Float32Consts.SIGNIFICAND_WIDTH - 1
        val qr: Int = (be - (Float32Consts.EXP_BIAS + Float32Consts.SIGNIFICAND_WIDTH - 1) - (if (be != 0) 1 else 0))
        val cr = 2 * (bits and Float32Consts.SIGNIF_BIT_MASK or (if (be != 0) FloatToDecimal.C_MIN else 0)) + 1
        val lhs = valueOfMulPow52(cr.toLong(), maxOf(-ep, 0), maxOf(qr - ep, 0), lhs.makeMutable())
        val rhs = FDBigInteger(fl, d, m, n).multByPow52(maxOf(ep, 0), maxOf(ep - qr, 0))
        val cmp = lhs.cmp(rhs)
        v = Float.fromBits(
            when {
                cmp < 0 -> bits + 1
                cmp > 0 -> bits
                else -> bits + (bits and 1)
            }
        )
        return signed(v)
    }

    private fun signed(v: Double): Double = if (isNegative) -v else v

    private fun signed(v: Float): Float = if (isNegative) -v else v

    companion object {
        private const val MAX_DEC_DIGITS = 15 // max{n : 10^n ≤ 2^P}
        private const val FLOG_10_MAX_LONG = 18 // max{i : 10^i ≤ Long.MAX_VALUE}

        /* All the powers of 10 that can be represented exactly in double. */
        private val SMALL_10_POW = doubleArrayOf(
            1e0, 1e1, 1e2, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8, 1e9,
            1e10, 1e11, 1e12, 1e13, 1e14, 1e15, 1e16, 1e17, 1e18, 1e19,
            1e20, 1e21, 1e22,
        )

        /* All the powers of 10 that can be represented exactly in float. */
        private val SINGLE_SMALL_10_POW = floatArrayOf(
            1e0f, 1e1f, 1e2f, 1e3f, 1e4f, 1e5f, 1e6f, 1e7f, 1e8f, 1e9f,
            1e10f,
        )

        private val MAX_SMALL_TEN = SMALL_10_POW.size - 1
        private val SINGLE_MAX_SMALL_TEN = SINGLE_SMALL_10_POW.size - 1

        // private static final int BINARY_128_IX = 3;
        // private static final int BINARY_256_IX = 4;
        private val P = intArrayOf(
            11,  // 11
            FloatToDecimal.P,  // 24
            DoubleToDecimal.P,  // 53
            // 113,
            // 237,
        )

        /* Minimum exponent in the c 2^q representation. */
        private val Q_MIN = intArrayOf(
            -24,  // Float16ToDecimal.Q_MIN,  // -24
            FloatToDecimal.Q_MIN,  // -149
            DoubleToDecimal.Q_MIN,  // -1_074
            // -16_494,
            // -262_378,
        )

        /*
         * For each binary floating-point format, let
         *      THR_Z = ulp(0.0) / 2 = MIN_VALUE / 2
         * THR_Z is the zero threshold.
         * Real x rounds to 0 by roundTiesToEven iff |x| ≤ THR_Z.
         *
         * E_THR_Z = max{e : 10^e ≤ THR_Z}.
         */
        private val E_THR_Z = intArrayOf(
            -8,  // -8
            FloatToDecimal.E_THR_Z,  // -46
            DoubleToDecimal.E_THR_Z,  // -324
            // -4_966,
            // -78_985,
        )

        /*
         * For each binary floating-point format, let
         *      THR_I = MAX_VALUE + ulp(MAX_VALUE) / 2
         * THR_I is the infinity threshold.
         * Real x rounds to infinity by roundTiesToEven iff |x| ≥ THR_I.
         *
         * E_THR_I = min{e : THR_I ≤ 10^(e-1)}.
         */
        private val E_THR_I = intArrayOf(
            6,  // 6
            FloatToDecimal.E_THR_I,  // 40
            DoubleToDecimal.E_THR_I,  // 310
            // 4_934,
            // 78_915,
        )

        const val BINARY_32_IX = 1
        const val BINARY_64_IX = 2

        /**
         * @param `in` the non-null input
         * @param ix one of the `BINARY_<S>_IX` constants, where `<S>` is one of 32, 64
         * @throws NumberFormatException if the input is malformed
         */
        fun parse(
            buffer: ASCIIToBinaryBuffer,
            ix: Int,
            input: Buffer,
            offset: Int,
            end: Int,
            onFormatError: DecodingErrorHandler<String>
        ): StringToFpConverter {
            run main@ {
                /*
                 * The scanning proper does not allocate any object,
                 * nor does it perform any costly computation.
                 * This means that all scanning errors are detected without consuming
                 * any heap, before actually throwing.
                 *
                 * Once scanning is complete, the method determines the length
                 * of a prefix of the significand that is sufficient for correct
                 * rounding according to roundTiesToEven.
                 * The actual value of the prefix length might not be optimal,
                 * but is always a safe choice.
                 *
                 * For hexadecimal input, the prefix is processed by this method directly,
                 * without allocating objects before creating the returned instance.
                 *
                 * For decimal input, the prefix is copied to the returned instance,
                 * along with the other information needed for the conversion.
                 * For comparison, the prefix length is at most
                 *       23 for BINARY_16_IX (Float16, once integrated in java.base)
                 *      114 for BINARY_32_IX (float)
                 *      769 for BINARY_64_IX (double)
                 * but is much shorter in common cases.
                 */
                val len = (end - offset)
                if (len <= 0 || len > MAX_DIGITS) return@main

                var i = offset

                /* Scan opt significand sign. */
                var ssign = ' '.code // ' ' iff sign is implicit
                var ch: Int = input[i] // running char
                if (ch == '-'.code || ch == '+'.code) {  // i < len
                    ssign = ch
                    ++i
                }

                /* Determine whether we are facing a symbolic value or hex notation. */
                if (i < end) {
                    ch = input[i]
                    if (ch == 'I'.code) {
                        if (!checkIsInfinity(input, start = i + 1, end = end)) return@main
                        return if (ssign != '-'.code) StringToFpConverter.POSITIVE_INFINITY else StringToFpConverter.NEGATIVE_INFINITY
                    }
                    if (ch == 'N'.code) {
                        if (i != offset || !checkIsNaN(input, start = i + 1, end = end)) return@main
                        return StringToFpConverter.NAN // ignore sign
                    }
                }

                var pt = 0 // index after point, 0 iff absent
                val start = i // index of start of the significand, excluding opt sign

                /* Skip opt leading zeros, including an opt point. */
                while (i < end && ((input[i].also { ch = it }) == '0'.code || ch == '.'.code)) {
                    ++i
                    if (ch == '.'.code) {
                        if (pt != 0) return@main // multiple points
                        pt = i
                    }
                }
                val lz = i // index after leading group of zeros or point

                /*
                 * Scan all remaining chars of the significand, including an opt point.
                 * Also locate the index after the end of the trailing group of non-zeros
                 * inside this range of the input.
                 */
                var tnz = 0 // index after trailing group of non-zeros, 0 iff absent
                while (i < end && (isDigit(input[i].also { ch = it }) || ch == '.'.code)) {
                    i++
                    when {
                        ch == '.'.code -> {
                            if (pt != 0) return@main // multiple points
                            pt = i
                        }
                        ch != '0'.code -> tnz = i
                    }
                }
                // must have at least one digit
                if (i - start <= (if (pt != 0) 1 else 0)) return@main
                val stop = i // index after the significand

                /* Scan exponent part, optional for dec, mandatory for hex. */
                var ep: Long = 0 // exponent, implicitly 0
                if ((i < end) && (toLowerCase(input[i]) == 'e'.code)) {
                    ++i

                    /* Scan opt exponent sign. */
                    var esign = ' '.code // esign == ' ' iff the sign is implicit
                    if (i < end && ((input[i].also { ch = it }) == '-'.code || ch == '+'.code)) {
                        esign = ch
                        ++i
                    }

                    /* Scan the exponent digits. Accumulate in ep, clamping at 10^10. */
                    while (i < end && isDigit(input[i].also { ch = it })) {  // ep is decimal
                        ++i
                        ep = appendDecDigit(ep, ch)
                    }
                    // at least 3 chars after significand OR 2 chars, one is digit
                    if (!(i - stop >= 3 || i - stop == 2 && esign == ' '.code)) return@main
                    if (esign == '-'.code) ep = -ep
                }

                if (i != end) return@main

                /* By now, the input is syntactically correct. */
                if (tnz == 0) {  // all zero digits, so ignore ep and point
                    return if (ssign != '-'.code) StringToFpConverter.POSITIVE_ZERO else StringToFpConverter.NEGATIVE_ZERO
                }

                /*
                 * Virtually adjust the point position to be just after
                 * the last non-zero digit by adjusting the exponent accordingly
                 * (without modifying the physical pt, as it is used later on).
                 *
                 * Determine the count of digits, excluding leading and trailing zeros.
                 *
                 * These are the possible situations:
                 *         |lz               |tnz     |stop
                 * 00000000123456000000234567000000000
                 *
                 *  |pt     |lz               |tnz     |stop
                 * .00000000123456000000234567000000000
                 *
                 *    |pt   |lz               |tnz     |stop
                 * 00.000000123456000000234567000000000
                 *
                 *          |pt=lz            |tnz     |stop
                 * 00000000.123456000000234567000000000
                 *
                 *         |lz  |pt           |tnz     |stop
                 * 000000001234.56000000234567000000000
                 *
                 *         |lz      |pt       |tnz     |stop
                 * 0000000012345600.0000234567000000000
                 *
                 *         |lz          |pt   |tnz     |stop
                 * 00000000123456000000.234567000000000
                 *
                 *         |lz            |pt |tnz     |stop
                 * 0000000012345600000023.4567000000000
                 *
                 *         |lz                |pt=tnz  |stop
                 * 00000000123456000000234567.000000000
                 *
                 *         |lz               |tnz  |pt |stop
                 * 0000000012345600000023456700000.0000
                 *
                 *         |lz               |tnz      |pt=stop
                 * 00000000123456000000234567000000000.
                 *
                 * In decimal, moving the point by one position means correcting ep by 1.
                 * In hexadecimal, it means correcting ep by 4.
                 */
                var n = tnz - lz // number of significant digits, 1st approximation
                if (pt == 0) {
                    ep += stop - tnz
                } else {
                    ep += pt - tnz
                    if (pt > tnz) {  // '.' was counted as a position, adjust ep
                        ep -= 1L
                    } else if (lz < pt) {  // lz < pt ≤ tnz
                        n -= 1
                    }
                }

                /*
                 * For decimal inputs, we copy an appropriate prefix of the input and
                 * rely on another method to do the (sometimes intensive) math conversion.
                 *
                 * Define e = n + ep, which leads to
                 *      x = 0.d_1 ... d_n 10^e, 10^(e-1) ≤ x < 10^e
                 * If e ≤ E_THR_Z then x rounds to zero.
                 * Similarly, if e ≥ E_THR_I then x rounds to infinity.
                 * We return immediately in these cases.
                 * Otherwise, e fits in an int, aptly named e as well.
                 */
                val e: Int = (ep + n).coerceIn(E_THR_Z[ix].toLong(), E_THR_I[ix].toLong()).toInt()
                when (e) {
                    // the true mathematical e ≤ E_THR_Z
                    E_THR_Z[ix] -> return if (ssign != '-'.code) StringToFpConverter.POSITIVE_ZERO else StringToFpConverter.NEGATIVE_ZERO
                    // the true mathematical e ≥ E_THR_I
                    E_THR_I[ix] -> return if (ssign != '-'.code) StringToFpConverter.POSITIVE_INFINITY else StringToFpConverter.NEGATIVE_INFINITY
                }

                /*
                 * For further considerations, x also needs to be seen as
                 *      x = beta 2^q
                 * with real beta and integer q meeting
                 *      q ≥ Q_MIN
                 * and
                 *      either  2^(P-1) ≤ beta < 2^P
                 *      or      0 < beta < 2^(P-1) and q = Q_MIN
                 * The (unique) solution is
                 *      q = max(floor(log2(x)) - (P-1), Q_MIN), beta = x 2^(-q)
                 * It's usually costly to determine q as here.
                 * However, estimates to q are cheaper and quick to compute.
                 *
                 * Indeed, it's a matter of some simple maths to show that, by defining
                 *      ql = max(floor((e-1) log2(10)) - (P-1), Q_MIN)
                 *      qh = max(floor(e log2(10)) - (P-1), Q_MIN)
                 * then the following hold
                 *      ql ≤ q ≤ qh, and qh - ql ≤ 4
                 * Since by now e is relatively small, we can leverage flog2pow10().
                 *
                 * Consider the half-open interval [ 2^(P-1+q), 2^(P+q) ).
                 * It contains all floating-point values of the form
                 *      c 2^q, c integer, 2^(P-1) ≤ c < 2^P (normal values)
                 * When q = Q_MIN also consider the interval half-open [0, 2^(P-1+q) ),
                 * which contains all floating-point values of the form
                 *      c 2^q, c integer, 0 ≤ c < 2^(P-1) (subnormal values and zero)
                 * For these c values, all numbers of the form
                 *      (c + 1/2) 2^q
                 * also belong to the intervals.
                 * ThStringese are the boundaries of the rounding intervals and are key for
                 * correct rounding.
                 *
                 * First assume ql > 0, so q > 0.
                 * All rounding boundaries (c + 1/2) 2^q are integers.
                 *
                 * Hence, to correctly round x, it's enough to retain its integer part,
                 * +1 non-zero sticky digit iff the fractional part is non-zero.
                 * (Well, the sticky digit is only needed when the integer part
                 * coincides with a boundary, but that's hard to detect at this stage.
                 * Adding the sticky digit is always safe.)
                 * If n > e we pass the digits <d_1...d_e 8> (8 is as good as any other
                 * non-zero sticky digit) and the exponent e to the conversion routine.
                 * If n ≤ e we pass all the digits <d_1...d_n> (no sticky digit,
                 * as the fractional part is empty) and the exponent e to the converter.
                 *
                 * Now assume qh ≤ 0, so q ≤ 0.
                 * The boundaries (c + 1/2) 2^q = (2c + 1) 2^(q-1) have a fractional part
                 * of 1 - q digits: some (or zero) leading zeros, the rightmost is 5.
                 * A correct rounding needs to retain the integer part of x (if any),
                 * 1 - q digits of the fractional part, +1 non-zero sticky digit iff
                 * the rest of the fractional part beyond the 1 - q digits is non-zero.
                 * (Again, the sticky digit is only needed when the digit in f at the
                 * same position as the last 5 of the rounding boundary is 5 as well.
                 * But let's keep it simple for now.)
                 * However, q is unknown, so use the conservative ql instead.
                 * More precisely, if n > e + 1 - ql we pass the leftmost e + 1 - ql
                 * digits of f, sticky 8 (the "most even" digit), and e.
                 * Otherwise, n ≤ e + 1 - ql.
                 * We pass all n digits of f, no sticky digit, and e to the converter.
                 *
                 * Otherwise, ql ≤ 0 < qh, so -4 < q ≤ 4.
                 * Again, since q is not known exactly, we proceed as in the previous
                 * case, with ql as a safe replacement for q.
                 */
                val ql: Int = maxOf(flog2pow10(e - 1) - (P[ix] - 1), Q_MIN[ix])
                val np: Int = e + maxOf(2 - ql, 1)
                val nDigits: Int = if (n >= np) {
                    copyDigits(input, buffer.d, np - 1, lz)
                    buffer.d[np - 1] = '8'.code.toByte() // append the "most even" non-zero sticky digit
                    np
                } else {
                    copyDigits(input, buffer.d, n, lz)
                    n
                }
                buffer.isNegative = ssign == '-'.code
                buffer.e = e
                buffer.n = nDigits
                return buffer
            }

            onFormatError("malformed number")
            return StringToFpConverter.NAN
        }

        private fun toLowerCase(ch: Int): Int = ch or 32

        private fun copyDigits(source: Buffer, d: ByteArray, len: Int, i: Int) {
            var i = i
            var j = 0
            while (j < len) {
                val ch = source[i++]
                if (ch != '.'.code) d[j++] = ch.toByte()
            }
        }

        /* Arithmetically "appends the dec digit" ch to v ≥ 0, clamping at 10^10. */
        private fun appendDecDigit(v: Long, ch: Int): Long = if (v < 10000000000L / 10) 10 * v + (ch - '0'.code) else 10000000000L

        /* Whether ch is a digit char '0-9', 'A-F', or 'a-f', depending on isDec. */
        private fun isDigit(ch: Int): Boolean = '0'.code <= ch && ch <= '9'.code

        private fun checkIsInfinity(s: Buffer, start: Int, end: Int): Boolean {
            val inf = "nfinity"
            if (end - start != inf.length) return false
            for (i in start until end) {
                if (s[i] != inf[i - start].code) return false
            }
            return true
        }

        private fun checkIsNaN(s: Buffer, start: Int, end: Int): Boolean =
            end - start == 2 && s[start] == 'a'.code && s[start + 1] == 'N'.code
    }
}

