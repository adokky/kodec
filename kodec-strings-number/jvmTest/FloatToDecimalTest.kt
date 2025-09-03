package io.kodec

import io.kodec.FloatToDecimalChecker.Companion.Z
import java.lang.Integer.numberOfTrailingZeros
import java.math.BigDecimal
import kotlin.random.Random
import kotlin.test.Ignore
import kotlin.test.Test

class FloatToDecimalTest: BasicChecker() {
    private fun testDec(v: Float) {
        FloatToDecimalChecker(v, this).check()
    }

    /*
     * Test around v, up to z values below and above v.
     * Don't care when v is at the extremes,
     * as any value returned by Float.fromBits() is valid.
     */
    private fun testAround(v: Float, z: Int) {
        val bits: Int = v.toRawBits()
        for (i in -z .. z) {
            testDec(Float.fromBits(bits + i))
        }
    }

    /*
     * MIN_NORMAL is incorrectly rendered by older JDKs.
     */
    @Test
    fun testExtremeValues() = test {
        testDec(Float.NEGATIVE_INFINITY)
        testAround(-Float.MAX_VALUE, Z)
        testAround(-java.lang.Float.MIN_NORMAL, Z)
        testAround(-Float.MIN_VALUE, Z)
        testDec(-0.0f)
        testDec(0.0f)
        testAround(Float.MIN_VALUE, Z)
        testAround(java.lang.Float.MIN_NORMAL, Z)
        testAround(Float.MAX_VALUE, Z)
        testDec(Float.POSITIVE_INFINITY)
        testDec(Float.NaN)

        /*
         * Quiet NaNs have the most significant bit of the mantissa as 1,
         * while signaling NaNs have it as 0.
         * Exercise 4 combinations of quiet/signaling NaNs and
         * "positive/negative" NaNs.
         */
        testDec(Float.fromBits(0x7FC00001))
        testDec(Float.fromBits(0x7F800001))
        testDec(Float.fromBits(-0x3fffff))
        testDec(Float.fromBits(-0x7fffff))

        /*
         * All values treated specially by Schubfach
         */
        for (c in 1 ..< FloatToDecimalChecker.C_TINY) {
            testDec(c * Float.MIN_VALUE)
        }
    }

    /*
     * Some values close to powers of 10 are incorrectly rendered by older JDKs.
     * The rendering is either too long or it is not the closest decimal.
     */
    @Test
    fun testPowersOf10() = test {
        for (e in FloatToDecimalChecker.E_MIN .. FloatToDecimalChecker.E_MAX) {
            testAround(java.lang.Float.parseFloat("1e$e"), Z)
        }
    }

    /*
     * Many powers of 2 are incorrectly rendered by older JDKs.
     * The rendering is either too long or it is not the closest decimal.
     */
    @Test
    fun testPowersOf2() = test {
        run {
            var v = Float.MIN_VALUE
            while (v <= Float.MAX_VALUE) {
                testAround(v, Z)
                v *= 2f
            }
        }
    }

    /*
     * There are tons of floats that are rendered incorrectly by older JDKs.
     * While the renderings correctly round back to the original value,
     * they are longer than needed or are not the closest decimal to the float.
     * Here are just a very few examples.
     */
    private val anomalies = arrayOf<String?>(
        /* Older JDKs render these longer than needed */"1.1754944E-38", "2.2E-44",
        "1.0E16", "2.0E16", "3.0E16", "5.0E16", "3.0E17",
        "3.2E18", "3.7E18", "3.7E16", "3.72E17", "2.432902E18",  /* Older JDKs do not render this as the closest */

        "9.9E-44",
    )

    @Test
    fun testSomeAnomalies() = test {
        for (dec in anomalies) {
            testDec(java.lang.Float.parseFloat(dec))
        }
    }

    /*
     * Values are from
     * Paxson V, "A Program for Testing IEEE Decimal-Binary Conversion"
     * tables 16 and 17
     */
    private val paxsonSignificands = floatArrayOf(
        12676506f,
        15445013f,
        13734123f,
        12428269f,
        12676506f,
        15334037f,
        11518287f,
        12584953f,
        15961084f,
        14915817f,
        10845484f,
        16431059f,

        16093626f,
        9983778f,
        12745034f,
        12706553f,
        11005028f,
        15059547f,
        16015691f,
        8667859f,
        14855922f,
        14855922f,
        10144164f,
        13248074f,
    )

    private val paxsonExponents = intArrayOf(
        -102,
        -103,
        86,
        -138,
        -130,
        -146,
        -41,
        -145,
        -125,
        -146,
        -102,
        -61,

        69,
        25,
        104,
        72,
        45,
        71,
        -99,
        56,
        -82,
        -83,
        -110,
        95,
    )

    @Test
    fun testPaxson() = test {
        for (i in paxsonSignificands.indices) {
            testDec(StrictMath.scalb(paxsonSignificands[i], paxsonExponents[i]))
        }
    }

    /*
     * Tests all positive integers below 2^23.
     * These are all exact floats and exercise the fast path.
     */
    @Test
    fun testInts() = test {
        for (i in 1 ..< (1 shl FloatToDecimalChecker.P - 1)) {
            testDec(i.toFloat())
        }
    }

    /*
     * 0.1, 0.2, ..., 999.9 and around
     */
    @Test
    fun testDeci() = test {
        for (i in 1 ..< 10000) {
            testAround(i / 1e1f, 10)
        }
    }

    /*
     * 0.01, 0.02, ..., 99.99 and around
     */
    @Test
    fun testCenti() = test {
        for (i in 1 ..< 10000) {
            testAround(i / 1e2f, 10)
        }
    }

    /*
     * 0.001, 0.002, ..., 9.999 and around
     */
    @Test
    fun testMilli() = test {
        for (i in 1 ..< 10000) {
            testAround(i / 1e3f, 10)
        }
    }

    /*
     * Random floats over the whole range.
     */
    @Test
    fun testRandom() = test {
        repeat(100_000) {
            testDec(Float.fromBits(Random.nextInt()))
        }
    }

    /*
     * All, really all, 2^32 possible floats. Takes between 90 and 120 minutes.
     */
    @Ignore
    @Test
    fun testAll() = test {
        /* Avoid wrapping around Integer.MAX_VALUE */
        var bits: Int = Integer.MIN_VALUE
        while (bits < Integer.MAX_VALUE) {
            testDec(Float.fromBits(bits))
            ++bits
        }
        testDec(Float.fromBits(bits))
    }

    /*
     * All positive 2^31 floats.
     */
    @Ignore
    @Test
    fun testPositive() = test {
        /* Avoid wrapping around Integer.MAX_VALUE */
        var bits = 0
        while (bits < Integer.MAX_VALUE) {
            testDec(Float.fromBits(bits))
            ++bits
        }
        testDec(Float.fromBits(bits))
    }

    /*
     * Values suggested by Guy Steele
     */
    @Test
    fun testRandomShortDecimals() = test {
        val e: Int = Random.nextInt(FloatToDecimalChecker.E_MAX - FloatToDecimalChecker.E_MIN + 1) + FloatToDecimalChecker.E_MIN
        var pow10 = 1
        while (pow10 < 10000) {
            /* randomly generate an int in [pow10, 10 pow10) */
            testAround(java.lang.Float.parseFloat((Random.nextInt(9 * pow10) + pow10).toString() + "e" + e), Z)
            pow10 *= 10
        }
    }

    @Test
    fun constants() = test {
        with(FloatToDecimalChecker) {
            addOnFail(P == FloatToDecimal.P, "P")
            addOnFail(W == FloatToDecimal.W, "W")

            addOnFail(Q_MIN == FloatToDecimal.Q_MIN, "Q_MIN")
            addOnFail(Q_MAX == FloatToDecimal.Q_MAX, "Q_MAX")
            addOnFail(C_MIN == FloatToDecimal.C_MIN.toLong(), "C_MIN")
            addOnFail(C_MAX == FloatToDecimal.C_MAX.toLong(), "C_MAX")
            addOnFail(C_MIN.toFloat().toInt().toLong() == C_MIN, "C_MIN")
            addOnFail(C_MAX.toFloat().toInt().toLong() == C_MAX, "C_MAX")

            addOnFail(E_MIN == FloatToDecimal.E_MIN, "E_MIN")
            addOnFail(E_MAX == FloatToDecimal.E_MAX, "E_MAX")
            addOnFail(E_THR_Z == FloatToDecimal.E_THR_Z, "E_THR_Z")
            addOnFail(E_THR_I == FloatToDecimal.E_THR_I, "E_THR_I")
            addOnFail(K_MIN == FloatToDecimal.K_MIN, "K_MIN")
            addOnFail(K_MAX == FloatToDecimal.K_MAX, "K_MAX")

            addOnFail(C_TINY == FloatToDecimal.C_TINY, "C_TINY")
            addOnFail(H == FloatToDecimal.H, "H")

            addOnFail(MIN_VALUE.compareTo(BigDecimal(Float.MIN_VALUE.toDouble())) == 0, "MIN_VALUE")
            addOnFail(MIN_NORMAL.compareTo(BigDecimal(java.lang.Float.MIN_NORMAL.toDouble())) == 0, "MIN_NORMAL")
            addOnFail(MAX_VALUE.compareTo(BigDecimal(Float.MAX_VALUE.toDouble())) == 0, "MAX_VALUE")
        }
    }
}

private class FloatToDecimalChecker(private val v: Float, errors: BasicChecker) : ToDecimalChecker(NumberToString.toString(v), errors) {
    override fun eMin(): Int = E_MIN

    override fun eMax(): Int = E_MAX

    override fun h(): Int = H

    override fun maxStringLength(): Int = H + 6

    override fun toBigDecimal(): BigDecimal = BigDecimal(v.toDouble())

    override fun recovers(bd: BigDecimal): Boolean = bd.toFloat() == v

    override fun recovers(s: String): Boolean = java.lang.Float.parseFloat(s) == v

    override fun hexString(): String = java.lang.Float.toHexString(v) + "F"

    override val isNegativeInfinity: Boolean
        get() = v == Float.NEGATIVE_INFINITY

    override val isPositiveInfinity: Boolean
        get() = v == Float.POSITIVE_INFINITY

    override val isMinusZero: Boolean
        get() = v.toRawBits() == 1 shl 31

    override val isPlusZero: Boolean
        get() = v.toRawBits() == 0x00000000

    override val isNaN: Boolean
        get() = v.isNaN()

    companion object {
        val P: Int = numberOfTrailingZeros((3.0f).toRawBits()) + 2
        val W: Int = w(P)

        val Q_MIN: Int = q_min(P)
        val Q_MAX: Int = q_max(P)
        val C_MIN: Long = c_min(P)
        val C_MAX: Long = c_max(P)

        val E_MIN: Int = e_min(P)
        val E_MAX: Int = e_max(P)
        val E_THR_Z: Int = e_thr_z(P)
        val E_THR_I: Int = e_thr_i(P)
        val K_MIN: Int = k_min(P)
        val K_MAX: Int = k_max(P)

        val C_TINY: Int = c_tiny(P)
        val H: Int = h(P)

        val MIN_VALUE: BigDecimal = min_value(P)
        val MIN_NORMAL: BigDecimal = min_normal(P)
        val MAX_VALUE: BigDecimal = max_value(P)

        const val Z = 1024
    }
}
