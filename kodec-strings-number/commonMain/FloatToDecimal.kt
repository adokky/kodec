package io.kodec

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
 * This class exposes a method to render a `float` as a string.
 */
internal object FloatToDecimal : ToDecimal() {
    const val P: Int = 24

    /* Exponent width in bits. */
    const val W: Int = (Float.SIZE_BITS - 1) - (P - 1)

    /* Minimum value of the exponent: -(2^(W-1)) - P + 3. */
    const val Q_MIN: Int = (-1 shl (W - 1)) - P + 3

    /* Maximum value of the exponent: 2^(W-1) - P. */
    const val Q_MAX: Int = (1 shl (W - 1)) - P

    /* Minimum value of the significand of a normal value: 2^(P-1). */
    const val C_MIN: Int = 1 shl (P - 1)

    /* Maximum value of the significand of a normal value: 2^P - 1. */
    const val C_MAX: Int = (1 shl P) - 1

    /* E_MIN = max{e : 10^(e-1) <= MIN_VALUE}. */
    const val E_MIN: Int = -44

    /* E_MAX = max{e : 10^(e-1) <= MAX_VALUE}. */
    const val E_MAX: Int = 39

    /*
     * Let THR_Z = ulp(0.0) / 2 = MIN_VALUE / 2 = 2^(Q_MIN-1).
     * THR_Z is the zero threshold.
     * x is rounded to 0 by roundTiesToEven iff |x| <= THR_Z.
     *
     * E_THR_Z = max{e : 10^e <= THR_Z}.
     */
    const val E_THR_Z: Int = -46

    /*
     * Let THR_I = MAX_VALUE + ulp(MAX_VALUE) / 2 = (2 C_MAX + 1) 2^(Q_MAX-1).
     * THR_I is the infinity threshold.
     * x is rounded to infinity by roundTiesToEven iff |x| >= THR_I.
     *
     * E_THR_I = min{e : THR_I <= 10^(e-1)}.
     */
    const val E_THR_I: Int = 40

    /* K_MIN = max{k : 10^k <= 2^Q_MIN}. */
    const val K_MIN: Int = -45

    /* K_MAX = max{k : 10^k <= 2^Q_MAX}. */
    const val K_MAX: Int = 31

    /*
     * Threshold to detect tiny values, as in section 8.2.1 of [1].
     *      C_TINY = ceil(2^(-Q_MIN) 10^(K_MIN+1))
     */
    const val C_TINY: Int = 8

    /*
     * H is as in section 8.1 of [1].
     *      H = max{e : 10^(e-2) <= 2^P}
     */
    const val H: Int = 9

    /* Mask to extract the biased exponent. */
    private const val BQ_MASK = (1 shl W) - 1

    /* Mask to extract the fraction bits. */
    private const val T_MASK = (1 shl (P - 1)) - 1

    /* Used in rop(). */
    private const val MASK_32 = (1L shl 32) - 1

    /*
     * Room for the longer of the forms
     *     -ddddd.dddd         H + 2 characters
     *     -0.00ddddddddd      H + 5 characters
     *     -d.ddddddddE-ee     H + 6 characters
     * where there are H digits d
     */
    const val MAX_CHARS: Int = H + 6

    /**
     * Appends the rendering of the [value] to [dst].
     *
     * @param dst the String byte array to append to
     * @param index the index into [dst]
     * @return string size
     */
    fun putDecimal(dst: MutableBuffer, index: Int, value: Float): Int {
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
    fun toString(value: Float, stringBuilder: StringBuilder?): String {
        val buffer = ArrayBuffer(MAX_CHARS)
        val pair: Int = toDecimal(buffer, 0, value)
        return when (val type = pair and 0xFF00) {
            NON_SPECIAL -> {
                val size = pair and 0xFF
                buffer.array.toAsciiString(size, stringBuilder)
            }
            else -> special(type)
        }
    }

    /**
     * Returns:
     *     Combine type and size, the first byte is size, the second byte is type
     *
     *     PLUS_ZERO       iff v is 0.0
     *     MINUS_ZERO      iff v is -0.0
     *     PLUS_INF        iff v is POSITIVE_INFINITY
     *     MINUS_INF       iff v is NEGATIVE_INFINITY
     *     NAN             iff v is NaN
     */
    private fun toDecimal(dst: MutableBuffer, index: Int, v: Float): Int {
        /*
         * For full details see references [2] and [1].
         *
         * For finite v != 0, determine integers c and q such that
         *     |v| = c 2^q    and
         *     Q_MIN <= q <= Q_MAX    and
         *         either    2^(P-1) <= c < 2^P                 (normal)
         *         or        0 < c < 2^(P-1)  and  q = Q_MIN    (subnormal)
         */
        var index = index
        val bits: Int = v.toRawBits()
        val t = bits and T_MASK
        val bq = (bits ushr P - 1) and BQ_MASK
        if (bq < BQ_MASK) {
            val start = index
            if (bits < 0) index = putChar(dst, index, '-')
            return when {
                bq != 0 -> { // normal value
                    val mq: Int = -Q_MIN + 1 - bq // here mq = -q
                    val c: Int = C_MIN or t
                    /* The fast path discussed in section 8.3 of [1] */
                    if ((0 < mq) and (mq < P)) {
                        val f = c shr mq
                        if (f shl mq == c) return toChars(dst, index, f, 0) - start
                    }
                    toDecimal(dst, index, -mq, c, 0) - start
                }
                else -> when {
                    t != 0 -> { // subnormal value
                        when {
                            t < C_TINY -> toDecimal(dst, index, Q_MIN, 10 * t, -1)
                            else -> toDecimal(dst, index, Q_MIN, t, 0)
                        } - start
                    }
                    else -> if (bits == 0) PLUS_ZERO else MINUS_ZERO
                }
            }
        }
        if (t != 0) return NAN
        return if (bits > 0) PLUS_INF else MINUS_INF
    }

    private fun toDecimal(dst: MutableBuffer, index: Int, q: Int, c: Int, dk: Int): Int {
        /*
         * The skeleton corresponds to figure 7 of [1].
         * The efficient computations are those summarized in figure 9.
         * Also check the appendix.
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
        val out = c and 0x1
        val cb = (c shl 2).toLong()
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
                k = MathUtils.flog10pow2(q)
            }
            else -> { // irregular spacing
                cbl = cb - 1
                k = MathUtils.flog10threeQuartersPow2(q)
            }
        }
        val h: Int = q + MathUtils.flog2pow10(-k) + 33

        /* g is as in the appendix */
        val g: Long = MathUtils.g1(-k) + 1

        val vb: Int = rop(g, cb shl h)
        val vbl: Int = rop(g, cbl shl h)
        val vbr: Int = rop(g, cbr shl h)

        val s = vb shr 2
        if (s >= 100) {
            /*
             * For n = 9, m = 1 the table in section 10 of [1] shows
             *     s' = floor(s / 10) = floor(s 1_717_986_919 / 2^34)
             *
             * sp10 = 10 s'
             * tp10 = 10 t'
             * upin    iff    u' = sp10 10^k in Rv
             * wpin    iff    w' = tp10 10^k in Rv
             * See section 9.3 of [1].
             */
            val sp10 = 10 * (s * 1717986919L ushr 34).toInt()
            val tp10 = sp10 + 10
            val upin = vbl + out <= sp10 shl 2
            val wpin = (tp10 shl 2) + out <= vbr
            if (upin != wpin) {
                /* Exactly one of u' or w' lies in Rv */
                return toChars(dst, index, if (upin) sp10 else tp10, k)
            }
        }

        /*
         * 10 <= s < 100    or    s >= 100  and  u', w' not in Rv
         * uin    iff    u = s 10^k in Rv
         * win    iff    w = t 10^k in Rv
         * See section 9.3 of [1].
         */
        val t = s + 1
        val uin = vbl + out <= s shl 2
        val win = (t shl 2) + out <= vbr
        if (uin != win) {
            /* Exactly one of u or w lies in Rv */
            return toChars(dst, index, if (uin) s else t, k + dk)
        }
        /*
         * Both u and w lie in Rv: determine the one closest to v.
         * See section 9.3 of [1].
         */
        val cmp = vb - (s + t shl 1)
        val away = cmp > 0 || cmp == 0 && (s and 0x1) != 0
        return toChars(dst, index, if (away) t else s, k + dk)
    }

    /*
     * Formats the decimal f 10^e.
     */
    private fun toChars(dst: MutableBuffer, index: Int, f: Int, e: Int): Int {
        /*
         * For details not discussed here see section 10 of [1].
         *
         * Determine len such that
         *     10^(len-1) <= f < 10^len
         */
        var f = f
        var e = e
        var len: Int = MathUtils.flog10pow2(Int.SIZE_BITS - f.countLeadingZeroBits())
        if (f >= MathUtils.pow10(len)) len += 1

        /*
         * Let fp and ep be the original f and e, respectively.
         * Transform f and e to ensure
         *     10^(H-1) <= f < 10^H
         *     fp 10^ep = f 10^(e-H) = 0.f 10^e
         */
        f *= MathUtils.pow10(H - len).toInt()
        e += len

        /*
         * The toChars?() methods perform left-to-right digits extraction
         * using ints, provided that the arguments are limited to 8 digits.
         * Therefore, split the H = 9 digits of f into:
         *     h = the most significant digit of f
         *     l = the last 8, least significant digits of f
         *
         * For n = 9, m = 8 the table in section 10 of [1] shows
         *     floor(f / 10^8) = floor(1_441_151_881 f / 2^57)
         */
        val h = (f * 1441151881L ushr 57).toInt()
        val l = f - 100000000 * h

        return when(e) {
            in 1 .. 7 -> toChars1(dst, index, h, l, e)
            in -2 .. 0 -> toChars2(dst, index, h, l, e)
            else -> toChars3(dst, index, h, l, e)
        }
    }

    private fun toChars1(dst: MutableBuffer, index: Int, h: Int, l: Int, e: Int): Int {
        /*
         * 0 < e <= 7: plain format without leading zeroes.
         * Left-to-right digits extraction:
         * algorithm 1 in [3], with b = 10, k = 8, n = 28.
         */
        var index = index
        index = putDigit(dst, index, h)
        var y: Int = y(l)
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
        return removeTrailingZeroes(dst, index)
    }

    private fun toChars2(dst: MutableBuffer, index: Int, h: Int, l: Int, e: Int): Int {
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
        index = put8Digits(dst, index, l)
        return removeTrailingZeroes(dst, index)
    }

    private fun toChars3(dst: MutableBuffer, index: Int, h: Int, l: Int, e: Int): Int {
        /* -3 >= e | e > 7: computerized scientific notation */
        var index = index
        index = putDigit(dst, index, h)
        index = putChar(dst, index, '.')
        index = put8Digits(dst, index, l)
        index = removeTrailingZeroes(dst, index)
        return exponent(dst, index, e - 1)
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
        /*
         * For n = 2, m = 1 the table in section 10 of [1] shows
         *     floor(e / 10) = floor(103 e / 2^10)
         */
        val d = e * 103 ushr 10
        index = putDigit(dst, index, d)
        return putDigit(dst, index, e - 10 * d)
    }

    /*
     * Computes rop(cp g 2^(-95))
     * See appendix and figure 11 of [1].
     */
    private fun rop(g: Long, cp: Long): Int {
        val x1: Long = multiplyHigh(g, cp)
        val vbp = x1 ushr 31
        return (vbp or ((x1 and MASK_32) + MASK_32 ushr 32)).toInt()
    }
}