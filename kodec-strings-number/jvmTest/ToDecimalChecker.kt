package io.kodec

import io.kodec.MathUtilsChecker.pow10
import io.kodec.MathUtilsChecker.ceil
import io.kodec.MathUtilsChecker.clog10
import io.kodec.MathUtilsChecker.flog10
import io.kodec.MathUtilsChecker.flog10pow2
import io.kodec.MathUtilsChecker.pow2
import java.io.StringReader
import java.math.BigDecimal

/**
 * Relies on straightforward use of (expensive) BigDecimal arithmetic.
 * Not optimized for performance.
 *
 * @property s The string to check
 * @property errors container for storing errors
 */
abstract class ToDecimalChecker(private val s: String, private val errors: BasicChecker) : DelegatingChecker(errors) {
    /* The decimal parsed from s is dv = (sgn c) 10^q*/
    private var sgn = 0
    private var q = 0
    private var c: Long = 0

    /* The number of digits in c: 10^(l-1) <= c < 10^l */
    private var l = 0

    private fun conversionError(reason: String?): Boolean = addError(
        "toString(${hexString()}) returns incorrect \"$s\" ($reason)"
    )

    /*
     * Returns whether s syntactically meets the expected output of
     * toString(). It is restricted to finite nonzero outputs.
     */
    private fun failsOnParse(): Boolean {
        if (s.length > maxStringLength()) return conversionError("too long")

        StringReader(s).use { r ->
            /* 1 character look-ahead */
            var ch: Int = r.read()

            if (ch != '-'.code && !isDigit(ch)) {
                return conversionError("does not start with '-' or digit")
            }

            var m = 0
            if (ch == '-'.code) {
                ++m
                ch = r.read()
            }
            sgn = if (m > 0) -1 else 1

            var i = m
            while (ch == '0'.code) {
                ++i
                ch = r.read()
            }
            if (i - m > 1) {
                return conversionError("more than 1 leading '0'")
            }

            var p = i
            while (isDigit(ch)) {
                c = 10 * c + (ch - '0'.code)
                ++p
                ch = r.read()
            }
            if (p == m) {
                return conversionError("no integer part")
            }
            if (i in (m + 1) ..< p) {
                return conversionError("non-zero integer part with leading '0'")
            }

            var fz = p
            if (ch == '.'.code) {
                ++fz
                ch = r.read()
            }
            if (fz == p) {
                return conversionError("no decimal point")
            }

            var f = fz
            while (ch == '0'.code) {
                c *= 10
                ++f
                ch = r.read()
            }

            var x = f
            while (isDigit(ch)) {
                c = 10 * c + (ch - '0'.code)
                ++x
                ch = r.read()
            }
            if (x == fz) {
                return conversionError("no fraction")
            }
            l = if (p > i) x - i - 1 else x - f
            if (l > h()) {
                return conversionError("significand with more than " + h() + " digits")
            }
            if (x - fz > 1 && c % 10 == 0L) {
                return conversionError("fraction has more than 1 digit and ends with '0'")
            }

            if (ch == 'e'.code) {
                return conversionError("exponent indicator is 'e'")
            }
            if (ch != 'E'.code) {
                /* Plain notation, no exponent */
                if (p - m > 7) {
                    return conversionError("integer part with more than 7 digits")
                }
                if (i > m && f - fz > 2) {
                    return conversionError("pure fraction with more than 2 leading '0'")
                }
            } else {
                if (p - i != 1) {
                    return conversionError("integer part doesn't have exactly 1 non-zero digit")
                }

                ch = r.read()
                if (ch != '-'.code && !isDigit(ch)) {
                    return conversionError("exponent doesn't start with '-' or digit")
                }

                var e = x + 1
                if (ch == '-'.code) {
                    ++e
                    ch = r.read()
                }

                if (ch == '0'.code) {
                    return conversionError("exponent with leading '0'")
                }

                var z = e
                while (isDigit(ch)) {
                    q = 10 * q + (ch - '0'.code)
                    ++z
                    ch = r.read()
                }
                if (z == e) {
                    return conversionError("no exponent")
                }
                if (z - e > 3) {
                    return conversionError("exponent is out-of-range")
                }

                if (e > x + 1) {
                    q = -q
                }
                if (-3 <= q && q < 7) {
                    return conversionError("exponent lies in [-3, 7)")
                }
            }
            if (ch >= 0) {
                return conversionError("extraneous characters after decimal")
            }
            q += fz - x
        }
        return false
    }

    private fun addOnFail(expected: String?): Boolean = addOnFail(s == expected, "expected \"$expected\"")

    fun check(): Boolean {
        if (s.isEmpty()) {
            return conversionError("empty")
        }
        if (this.isNaN) {
            return addOnFail("NaN")
        }
        if (this.isNegativeInfinity) {
            return addOnFail("-Infinity")
        }
        if (this.isPositiveInfinity) {
            return addOnFail("Infinity")
        }
        if (this.isMinusZero) {
            return addOnFail("-0.0")
        }
        if (this.isPlusZero) {
            return addOnFail("0.0")
        }
        if (failsOnParse()) {
            return true
        }

        /* The exponent is bounded */
        if (eMin() > q + l || q + l > eMax()) {
            return conversionError("exponent is out-of-range")
        }

        /* s must recover v */
        try {
            if (!recovers(s)) {
                return conversionError("does not convert to the floating-point value")
            }
        } catch (ex: NumberFormatException) {
            return conversionError("unexpected exception (${ex.message})!!!")
        }

        if (l < 2) {
            c *= 10
            q -= 1
            l += 1
        }

        /* Get rid of trailing zeroes, still ensuring at least 2 digits */
        while (l > 2 && c % 10 == 0L) {
            c /= 10
            q += 1
            l -= 1
        }

        /* dv = (sgn * c) 10^q */
        if (l > 2) {
            /* Try with a number shorter than dv of lesser magnitude... */
            val dvd: BigDecimal = BigDecimal.valueOf(sgn * (c / 10), -(q + 1))
            if (recovers(dvd)) {
                return conversionError("\"$dvd\" is shorter")
            }
            /* ... and with a number shorter than dv of greater magnitude */
            val dvu: BigDecimal = BigDecimal.valueOf(sgn * (c / 10 + 1), -(q + 1))
            if (recovers(dvu)) {
                return conversionError("\"$dvu\" is shorter")
            }
        }

        /*
         * Check with the predecessor dvp (lesser magnitude)
         * and successor dvs (greater magnitude) of dv.
         * If |dv| < |v| dvp is not checked.
         * If |dv| > |v| dvs is not checked.
         */
        val v: BigDecimal = toBigDecimal()
        val dv: BigDecimal? = BigDecimal.valueOf(sgn * c, -q)
        val deltav: BigDecimal = v.subtract(dv)
        if (sgn * deltav.signum() < 0) {
            /* |dv| > |v|, check dvp */
            val dvp: BigDecimal =
                if (c == 10L)
                    BigDecimal.valueOf(sgn * 99L, -(q - 1))
                else
                    BigDecimal.valueOf(sgn * (c - 1), -q)
            if (recovers(dvp)) {
                val deltavp: BigDecimal = dvp.subtract(v)
                if (sgn * deltavp.signum() >= 0) {
                    return conversionError("\"$dvp\" is closer")
                }
                val cmp = sgn * deltav.compareTo(deltavp)
                if (cmp < 0) {
                    return conversionError("\"$dvp\" is closer")
                }
                if (cmp == 0 && (c and 0x1L) != 0L) {
                    return conversionError("\"$dvp\" is as close but has even significand")
                }
            }
        } else if (sgn * deltav.signum() > 0) {
            /* |dv| < |v|, check dvs */
            val dvs: BigDecimal = BigDecimal.valueOf(sgn * (c + 1), -q)
            if (recovers(dvs)) {
                val deltavs: BigDecimal = dvs.subtract(v)
                if (sgn * deltavs.signum() <= 0) {
                    return conversionError("\"$dvs\" is closer")
                }
                val cmp = sgn * deltav.compareTo(deltavs)
                if (cmp > 0) {
                    return conversionError("\"$dvs\" is closer")
                }
                if (cmp == 0 && (c and 0x1L) != 0L) {
                    return conversionError("\"$dvs\" is as close but has even significand")
                }
            }
        }
        return false
    }

    abstract fun eMin(): Int

    abstract fun eMax(): Int

    abstract fun h(): Int

    abstract fun maxStringLength(): Int

    abstract fun toBigDecimal(): BigDecimal

    abstract fun recovers(bd: BigDecimal): Boolean

    abstract fun recovers(s: String): Boolean

    abstract fun hexString(): String

    abstract val isNegativeInfinity: Boolean

    abstract val isPositiveInfinity: Boolean

    abstract val isMinusZero: Boolean

    abstract val isPlusZero: Boolean

    abstract val isNaN: Boolean

    companion object {
        private fun isDigit(ch: Int): Boolean = '0'.code <= ch && ch <= '9'.code

        fun size(p: Int): Int = 1 shl -Integer.numberOfLeadingZeros(p)

        fun w(p: Int): Int = (size(p) - 1) - (p - 1)

        fun q_min(p: Int): Int = (-1 shl (w(p) - 1)) - p + 3

        fun q_max(p: Int): Int = (1 shl (w(p) - 1)) - p

        fun c_min(p: Int): Long = 1L shl (p - 1)

        fun c_max(p: Int): Long = (1L shl p) - 1

        /* max{e : 10^(e-1) <= v */
        fun e(v: BigDecimal): Int = flog10(v) + 1

        fun e_min(p: Int): Int = e(min_value(p))

        fun e_max(p: Int): Int = e(max_value(p))

        fun e_thr_z(p: Int): Int {
            val THR_Z: BigDecimal = pow2(q_min(p) - 1)
            return flog10(THR_Z)
        }

        fun e_thr_i(p: Int): Int {
            val THR_I = BigDecimal.valueOf(2 * c_max(p) + 1)
                .multiply(pow2(q_max(p) - 1))
            return clog10(THR_I) + 1
        }

        fun k_min(p: Int): Int = flog10pow2(q_min(p))

        fun k_max(p: Int): Int = flog10pow2(q_max(p))

        /* C_TINY = ceil(2^(-Q_MIN) 10^(K_MIN+1)) */
        fun c_tiny(p: Int): Int {
            return ceil(pow2(-q_min(p))
                .multiply(pow10(k_min(p) + 1)))
                .intValueExact()
        }

        fun h(p: Int): Int = flog10pow2(p) + 2

        fun min_value(p: Int): BigDecimal = pow2(q_min(p))

        fun min_normal(p: Int): BigDecimal = BigDecimal.valueOf(c_min(p)).multiply(pow2(q_min(p)))

        fun max_value(p: Int): BigDecimal = BigDecimal.valueOf(c_max(p)).multiply(pow2(q_max(p)))
    }
}
