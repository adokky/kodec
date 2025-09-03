package io.kodec

import io.kodec.MathUtils.flog10pow2
import io.kodec.MathUtils.flog10threeQuartersPow2
import io.kodec.MathUtils.flog2pow10
import io.kodec.MathUtils.g0
import io.kodec.MathUtils.g1
import io.kodec.MathUtils.pow10
import io.kodec.buffers.ArrayBuffer
import io.kodec.buffers.MutableBuffer
import karamel.utils.assertionsEnabled

/*
 * For full details about this code see the following references:
 *
 * [1] Giulietti, "The Schubfach way to render doubles",
 *     https://drive.google.com/file/d/1gp5xv4CAa78SVgCeWfGqqI4FfYYYuNFb
 *
 * [2] IEEE Computer Society, "IEEE Standard for Floating-Point Arithmetic"
 *
 * [3] Bouvier & Zimmermann, "Division-Free Binary-to-Decimal Conversion"
 *
 * Divisions are avoided altogether for the benefit of those architectures
 * that do not provide specific machine instructions or where they are slow.
 * This is discussed in section 10 of [1].
 */

/**
 * This class exposes a method to render a `double` as a string.
 */
internal object DoubleToDecimal : ToDecimal() {
    /**
     * The number of bits in the significand of a `double` value.
     * This is the parameter N in section 4.2.3 of "The Java Language Specification".
     */
    const val PRECISION: Int = 53

    /* Precision */
    const val P: Int = PRECISION

    /* Exponent width in bits. */
    const val W: Int = (Double.SIZE_BITS - 1) - (P - 1)

    /* Minimum value of the exponent: -(2^(W-1)) - P + 3. */
    const val Q_MIN: Int = (-1 shl (W - 1)) - P + 3

    /* Maximum value of the exponent: 2^(W-1) - P. */
    const val Q_MAX: Int = (1 shl (W - 1)) - P

    /* Minimum value of the significand of a normal value: 2^(P-1). */
    const val C_MIN: Long = 1L shl (P - 1)

    /* Maximum value of the significand of a normal value: 2^P - 1. */
    const val C_MAX: Long = (1L shl P) - 1

    /* E_MIN = max{e : 10^(e-1) <= MIN_VALUE}. */
    const val E_MIN: Int = -323

    /* E_MAX = max{e : 10^(e-1) <= MAX_VALUE}. */
    const val E_MAX: Int = 309

    /*
     * Let THR_Z = ulp(0.0) / 2 = MIN_VALUE / 2 = 2^(Q_MIN-1).
     * THR_Z is the zero threshold.
     * Real x rounds to 0 by roundTiesToEven iff |x| <= THR_Z.
     *
     * E_THR_Z = max{e : 10^e <= THR_Z}.
     */
    const val E_THR_Z: Int = -324

    /*
     * Let THR_I = MAX_VALUE + ulp(MAX_VALUE) / 2 = (2 C_MAX + 1) 2^(Q_MAX-1).
     * THR_I is the infinity threshold.
     * Real x rounds to infinity by roundTiesToEven iff |x| >= THR_I.
     *
     * E_THR_I = min{e : THR_I <= 10^(e-1)}.
     */
    const val E_THR_I: Int = 310

    /* K_MIN = max{k : 10^k <= 2^Q_MIN}. */
    const val K_MIN: Int = -324

    /* K_MAX = max{k : 10^k <= 2^Q_MAX}. */
    const val K_MAX: Int = 292

    /*
     * Threshold to detect tiny values, as in section 8.2.1 of [1].
     *      C_TINY = ceil(2^(-Q_MIN) 10^(K_MIN+1))
     */
    const val C_TINY: Long = 3

    /*
    * H is as in section 8.1 of [1].
     *      H = max{e : 10^(e-2) <= 2^P}
     */
    const val H: Int = 17

    /* Mask to extract the biased exponent. */
    private const val BQ_MASK = (1 shl W) - 1

    /* Mask to extract the fraction bits. */
    private const val T_MASK = (1L shl (P - 1)) - 1

    /* Used in rop(). */
    private const val MASK_63 = (1L shl 63) - 1

    /*
     * Room for the longer of the forms
     *     -ddddd.dddddddddddd         H + 2 characters
     *     -0.00ddddddddddddddddd      H + 5 characters
     *     -d.ddddddddddddddddE-eee    H + 7 characters
     * where there are H digits d
     */
    const val MAX_CHARS: Int = H + 7

    /**
     * Appends the rendering of the [value] to [dst].
     *
     * @param dst the string byte array to append to
     * @param index the index into str
     * @return string size
     */
    fun putDecimal(dst: MutableBuffer, index: Int, value: Double): Int {
        if (assertionsEnabled) require(0 <= index && index <= dst.size - MAX_CHARS) {
            "Trusted caller missed bounds check"
        }

        val pair = toDecimal(dst, index, value)
        return when (val type = pair and 0xFF00) {
            NON_SPECIAL -> pair and 0xFF
            else -> putSpecial(dst, index, type)
        }
    }

    /**
     * Returns a string representation of [value]. 
     * All characters mentioned below are ASCII characters.
     */
    fun toString(value: Double, stringBuilder: StringBuilder?): String {
        val str = ArrayBuffer(MAX_CHARS)
        val pair: Int = toDecimal(str, 0, value)
        return when (val type = pair and 0xFF00) {
            NON_SPECIAL -> {
                val size = pair and 0xFF
                str.array.toAsciiString(size, stringBuilder)
            }
            else -> special(type)
        }
    }

    /*
     * Returns size in the lower byte, type in the high byte, where type is
     *     PLUS_ZERO       iff v is 0.0
     *     MINUS_ZERO      iff v is -0.0
     *     PLUS_INF        iff v is POSITIVE_INFINITY
     *     MINUS_INF       iff v is NEGATIVE_INFINITY
     *     NAN             iff v is NaN
     *     otherwise NON_SPECIAL
     */
    private fun toDecimal(dst: MutableBuffer, index: Int, v: Double): Int {
        /*
         * For full details see references [2] and [1].
         *
         * For finite v != 0, determine integers c and q such that
         *     |v| = c 2^q    and
         *     Q_MIN <= q <= Q_MAX    and
         *         either    2^(P-1) <= c < 2^P                 (normal)
         *         or        0 < c < 2^(P-1)  and  q = Q_MIN    (subnormal)
         */
        val bits: Long = v.toRawBits()
        val t = bits and T_MASK
        val bq = (bits ushr P - 1).toInt() and BQ_MASK
        if (bq >= BQ_MASK) return when {
            t != 0L -> NAN
            else -> if (bits > 0) PLUS_INF else MINUS_INF
        }
        val start = index
        var index = index
        if (bits < 0) {
            /*
             * fd != null implies str == null and bits >= 0
             * Thus, when fd != null, control never reaches here.
             */
            index = putChar(dst, index, '-')
        }
        if (bq != 0) {
            /* normal value. Here mq = -q */
            val mq: Int = -Q_MIN + 1 - bq
            val c: Long = C_MIN or t
            /* The fast path discussed in section 8.3 of [1] */
            if ((0 < mq) and (mq < P)) {
                val f = c shr mq
                if (f shl mq == c) {
                    return toChars(dst, index, f, 0, exact = true, away = false) - start
                }
            }
            return toDecimal(dst, index, -mq, c, 0) - start
        }
        return when {
            t != 0L -> { // subnormal value
                when {
                    t < C_TINY -> toDecimal(dst, index, Q_MIN, 10 * t, -1)
                    else -> toDecimal(dst, index, Q_MIN, t, 0)
                } - start
            }
            else -> if (bits == 0L) PLUS_ZERO else MINUS_ZERO
        }
    }

    private fun toDecimal(dst: MutableBuffer, index: Int, q: Int, c: Long, dk: Int): Int {
        /*
         * The skeleton corresponds to figure 7 of [1].
         * The efficient computations are those summarized in figure 9.
         *
         * Here's a correspondence between Java names and names in [1],
         * expressed as approximate LaTeX source code and informally.
         * Other names are identical.
         * cb:     \bar{c}     "c-bar"
         * cbr:    \bar{c}_r   "c-bar-r"
         * cbl:    \bar{c}_l   "c-bar-l"
         *
         * vb:     \bar{v}     "v-bar"
         * vbr:    \bar{v}_r   "v-bar-r"
         * vbl:    \bar{v}_l   "v-bar-l"
         *
         * rop:    r_o'        "r-o-prime"
         */
        val out = c.toInt() and 0x1
        val cb = c shl 2
        val cbr = cb + 2
        val cbl: Long
        val k: Int
        /*
         * flog10pow2(e) = floor(log_10(2^e))
         * flog10threeQuartersPow2(e) = floor(log_10(3/4 2^e))
         * flog2pow10(e) = floor(log_2(10^e))
         */
        when {
            (c != C_MIN) or (q == Q_MIN) -> { // regular spacing
                cbl = cb - 2
                k = flog10pow2(q)
            }
            else -> { // irregular spacing
                cbl = cb - 1
                k = flog10threeQuartersPow2(q)
            }
        }
        val h: Int = q + flog2pow10(-k) + 2

        /* g1 and g0 are as in section 9.8.3 of [1], so g = g1 2^63 + g0 */
        val g1: Long = g1(-k)
        val g0: Long = g0(-k)

        val vb: Long = rop(g1, g0, cb shl h)
        val vbl: Long = rop(g1, g0, cbl shl h)
        val vbr: Long = rop(g1, g0, cbr shl h)

        val s = vb shr 2
        if (s >= 100) {
            /*
             * For n = 17, m = 1 the table in section 10 of [1] shows
             *     s' = floor(s / 10) = floor(s 115_292_150_460_684_698 / 2^60)
             *        = floor(s 115_292_150_460_684_698 2^4 / 2^64)
             *
             * sp10 = 10 s'
             * tp10 = 10 t'
             * upin    iff    u' = sp10 10^k in Rv
             * wpin    iff    w' = tp10 10^k in Rv
             * See section 9.3 of [1].
             *
             * Also,
             * d_v = v      iff     4 sp10 = vb
             */
            val sp10: Long = 10 * multiplyHigh(s, 115292150460684698L shl 4)
            val tp10 = sp10 + 10
            val upin = vbl + out <= sp10 shl 2
            val wpin = (tp10 shl 2) + out <= vbr
            if (upin != wpin) {
                /* Exactly one of u' or w' lies in Rv */
                return toChars(dst, index, if (upin) sp10 else tp10, k, sp10 shl 2 == vb, wpin)
            }
        }

        /*
         * 10 <= s < 100    or    s >= 100  and  u', w' not in Rv
         * uin    iff    u = s 10^k in Rv
         * win    iff    w = t 10^k in Rv
         * See section 9.3 of [1].
         *
         * Also,
         * d_v = v      iff     4 s = vb
         */
        val t = s + 1
        val uin = vbl + out <= s shl 2
        val win = (t shl 2) + out <= vbr
        if (uin != win) {
            /* Exactly one of u or w lies in Rv */
            return toChars(dst, index, if (uin) s else t, k + dk, s shl 2 == vb, win)
        }
        /*
         * Both u and w lie in Rv: determine the one closest to v.
         * See section 9.3 of [1].
         */
        val cmp = vb - (s + t shl 1)
        val away = cmp > 0 || cmp == 0L && (s and 0x1L) != 0L
        return toChars(dst, index, if (away) t else s, k + dk, s shl 2 == vb, away)
    }

    /*
     * Formats the decimal f 10^e.
     */
    private fun toChars(
        dst: MutableBuffer, index: Int, f: Long, e: Int,
        exact: Boolean, away: Boolean
    ): Int {
        /*
         * For details not discussed here see section 10 of [1].
         *
         * Determine len such that
         *     10^(len-1) <= f < 10^len
         */
        var f = f
        var e = e
        var len: Int = flog10pow2(Long.SIZE_BITS - f.countLeadingZeroBits())
        if (f >= pow10(len)) len += 1

        /*
         * Let fp and ep be the original f and e, respectively.
         * Transform f and e to ensure
         *     10^(H-1) <= f < 10^H
         *     fp 10^ep = f 10^(e-H) = 0.f 10^e
         */
        f *= pow10(H - len)
        e += len

        /*
         * The toChars?() methods perform left-to-right digits extraction
         * using ints, provided that the arguments are limited to 8 digits.
         * Therefore, split the H = 17 digits of f into:
         *     h = the most significant digit of f
         *     m = the next 8 most significant digits of f
         *     l = the last 8, least significant digits of f
         *
         * For n = 17, m = 8 the table in section 10 of [1] shows
         *     floor(f / 10^8) = floor(193_428_131_138_340_668 f / 2^84) =
         *     floor(floor(193_428_131_138_340_668 f / 2^64) / 2^20)
         * and for n = 9, m = 8
         *     floor(hm / 10^8) = floor(1_441_151_881 hm / 2^57)
         */
        val hm: Long = multiplyHigh(f, 193428131138340668L) ushr 20
        val l = (f - 100000000L * hm).toInt()
        val h = (hm * 1441151881L ushr 57).toInt()
        val m = (hm - 100000000 * h).toInt()

        return when(e) {
            in 1 .. 7 -> toChars1(dst, index, h, m, l, e)
            in -2 .. 0 -> toChars2(dst, index, h, m, l, e)
            else -> toChars3(dst, index, h, m, l, e)
        }
    }

    private fun toChars1(dst: MutableBuffer, index: Int, h: Int, m: Int, l: Int, e: Int): Int {
        /*
         * 0 < e <= 7: plain format without leading zeroes.
         * Left-to-right digits extraction:
         * algorithm 1 in [3], with b = 10, k = 8, n = 28.
         */
        var index = index
        index = putDigit(dst, index, h)
        var y: Int = y(m)
        var t: Int
        var i = 1
        while (i < e) {
            t = 10 * y
            index = putDigit(dst, index, t ushr 28)
            y = t and MASK_28
            ++i
        }
        index = putChar(dst, index, '.')
        while (i <= 8) {
            t = 10 * y
            index = putDigit(dst, index, t ushr 28)
            y = t and MASK_28
            ++i
        }
        return lowDigits(dst, index, l)
    }

    private fun toChars2(dst: MutableBuffer, index: Int, h: Int, m: Int, l: Int, e: Int): Int {
        /* -3 < e <= 0: plain format with leading zeroes */
        var index = index
        var e = e
        index = putDigit(dst, index, 0)
        index = putChar(dst, index, '.')
        while (e < 0) {
            index = putDigit(dst, index, 0)
            ++e
        }
        index = putDigit(dst, index, h)
        index = put8Digits(dst, index, m)
        return lowDigits(dst, index, l)
    }

    private fun toChars3(dst: MutableBuffer, index: Int, h: Int, m: Int, l: Int, e: Int): Int {
        /* -3 >= e | e > 7: computerized scientific notation */
        var index = index
        index = putDigit(dst, index, h)
        index = putChar(dst, index, '.')
        index = put8Digits(dst, index, m)
        index = lowDigits(dst, index, l)
        return exponent(dst, index, e - 1)
    }

    private fun lowDigits(dst: MutableBuffer, index: Int, l: Int): Int {
        var index = index
        if (l != 0) {
            index = put8Digits(dst, index, l)
        }
        return removeTrailingZeroes(dst, index)
    }

    private fun exponent(dst: MutableBuffer, index: Int, e: Int): Int {
        var index = index
        var e = e
        index = putChar(dst, index, 'E')
        if (e < 0) {
            index = putChar(dst, index, '-')
            e = -e
        }
        if (e < 10) return putDigit(dst, index, e)
        var d: Int
        if (e >= 100) {
            /*
             * For n = 3, m = 2 the table in section 10 of [1] shows
             *     floor(e / 100) = floor(1_311 e / 2^17)
             */
            d = e * 1311 ushr 17
            index = putDigit(dst, index, d)
            e -= 100 * d
        }
        /*
         * For n = 2, m = 1 the table in section 10 of [1] shows
         *     floor(e / 10) = floor(103 e / 2^10)
         */
        d = e * 103 ushr 10
        index = putDigit(dst, index, d)
        return putDigit(dst, index, e - 10 * d)
    }

    /*
     * Computes rop(cp g 2^(-127)), where g = g1 2^63 + g0
     * See section 9.9 and figure 8 of [1].
     */
    private fun rop(g1: Long, g0: Long, cp: Long): Long {
        val x1: Long = multiplyHigh(g0, cp)
        val y0 = g1 * cp
        val y1: Long = multiplyHigh(g1, cp)
        val z = (y0 ushr 1) + x1
        val vbp = y1 + (z ushr 63)
        return vbp or ((z and MASK_63) + MASK_63 ushr 63)
    }
}