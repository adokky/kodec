package io.kodec

import io.kodec.DoubleToDecimal.E_THR_I
import io.kodec.DoubleToDecimal.E_THR_Z
import io.kodec.DoubleToDecimal.K_MAX
import io.kodec.DoubleToDecimal.K_MIN
import java.math.BigDecimal
import java.math.BigInteger
import java.math.BigInteger.ONE
import java.math.RoundingMode
import kotlin.math.log10

object MathUtilsChecker : BasicChecker() {
    private val THREE_QUARTER: BigDecimal = BigDecimal("0.75")

    // decimal constants
    val N = n()
    val GE_MIN = ge_min()
    val GE_MAX = ge_max()

    const val MARGIN = 10

    fun pow2(q: Int): BigDecimal = when {
        q >= 0 -> BigDecimal(ONE.shiftLeft(q))
        else -> BigDecimal.ONE.divide(BigDecimal(ONE.shiftLeft(-q)))
    }

    fun pow10(e: Int): BigDecimal = BigDecimal.valueOf(1L, -e)

    fun floor(v: BigDecimal): BigInteger = v.setScale(0, RoundingMode.FLOOR).unscaledValue()

    fun ceil(v: BigDecimal): BigInteger = v.setScale(0, RoundingMode.CEILING).unscaledValue()

    /* floor(log2(v)) */
    fun flog2(v: BigDecimal): Int {
        /*
         * Let v = f 10^e.
         * Then log2(v) = log2(f) + e / log10(2).
         *
         * The initial flog2 is an estimate of l = floor(log2(v)), that is,
         * 2^l <= v < 2^(l+1).
         * Given the initial estimate flog2, search l meeting the above
         * inequalities.
         */
        var flog2: Int = ((v.unscaledValue().bitLength() - 1)
                + kotlin.math.floor(-v.scale() / log10(2.0)).toInt())
        while (pow2(flog2) <= v) {
            // empty body
            ++flog2
        }
        while (v < pow2(flog2)) {
            // empty body
            --flog2
        }
        return flog2
    }

    /* floor(log10(v)) */
    fun flog10(v: BigDecimal): Int = v.precision() - v.scale() - 1

    /* ceil(log10(v)) */
    fun clog10(v: BigDecimal): Int = flog10(v.subtract(v.ulp())) + 1

    /* floor(log10(2^q)) */
    fun flog10pow2(q: Int): Int = flog10(pow2(q))

    fun flog10threeQuartersPow2(q: Int): Int = flog10(THREE_QUARTER.multiply(pow2(q)))

    fun flog2pow10(e: Int): Int = flog2(pow10(e))

    private fun n(): Int = flog10pow2(java.lang.Long.SIZE)

    fun ge_max(): Int = Integer.max(-K_MIN, E_THR_I - 2)

    fun ge_min(): Int = Integer.min(-K_MAX, E_THR_Z - (N - 1))

    private fun r(e: Int): Int = flog2pow10(e) - 125

    fun g(e: Int): BigInteger = floor(pow10(e).multiply(pow2(-r(e)))).add(ONE)

    /**
     * Returns the product of the unsigned arguments,
     * throwing an exception if the result overflows an unsigned `long`.
     *
     * @param x the first unsigned value
     * @param y the second unsigned value
     * @return the result
     * @throws ArithmeticException if the result overflows an unsigned long
     * @since 25
     */
    fun unsignedMultiplyExact(x: Long, y: Long): Long {
        val l = x * y
        val h: Long = unsignedMultiplyHigh(x, y)
        if (h == 0L) {
            return l
        }
        throw java.lang.ArithmeticException("unsigned long overflow")
    }

    /**
     * Returns as a `long` the most significant 64 bits of the unsigned
     * 128-bit product of two unsigned 64-bit factors.
     *
     * @param x the first value
     * @param y the second value
     * @return the result
     * @see .multiplyHigh
     *
     * @since 18
     */
    fun unsignedMultiplyHigh(x: Long, y: Long): Long {
        // Compute via multiplyHigh() to leverage the intrinsic
        var result = Math.multiplyHigh(x, y)
        result += (y and (x shr 63)) // equivalent to `if (x < 0) result += y;`
        result += (x and (y shr 63)) // equivalent to `if (y < 0) result += x;`
        return result
    }

    /**
     * Returns unsigned `x` raised to the power of `n`,
     * throwing an exception if the result overflows an unsigned `long`.
     * When `n` is 0, the returned value is 1.
     *
     * @param x the unsigned base.
     * @param n the exponent.
     * @return `x` raised to the power of `n`.
     * @throws ArithmeticException when `n` is negative,
     * or when the result overflows an unsigned long.
     * @since 25
     */
    fun unsignedPowExact(x: Long, n: Int): Long {
        var x = x
        var n = n
        if (n < 0) {
            throw java.lang.ArithmeticException("negative exponent")
        }
        if (n == 0) {
            return 1
        }
        /*
         * To keep the code as simple as possible, there are intentionally
         * no fast paths, except for |x| <= 1.
         * The reason is that the number of loop iterations below can be kept
         * very small when |x| > 1, but not necessarily when |x| <= 1.
         */
        if (x == 0L || x == 1L) {
            return x
        }

        /*
         * Let x0 and n0 > 0 be the entry values of x and n, resp.
         * The useful loop invariants are:
         *      p * x^n = x0^n0
         *      |p| < |x|
         *
         * Since |x0| >= 2 here, and since |x0|^(2^6) >= 2^Long.SIZE, the squaring
         * of x in the loop overflows at latest during the 6th iteration,
         * so by then the method throws.
         * Thus, the loop executes at most 5 successful iterations, and fails
         * not later than at the 6th.
         *
         * But n is right-shifted at each iteration.
         * If the method returns, there are thus floor(log2(n0)) iterations.
         */
        var p: Long = 1
        while (n > 1) {
            if ((n and 1) != 0) {
                /*
                 * The invariant |p| < |x| holds, so we have |p*x| < |x*x|.
                 * That is, if p*x overflows, so does x*x below, which is
                 * always executed.
                 * In other words, a plain * can be used here, since we are
                 * piggybacking on the squaring of x to throw.
                 */
                p *= x
            }
            x = unsignedMultiplyExact(x, x)
            n = n ushr 1
        }
        return unsignedMultiplyExact(p, x)
    }
}

