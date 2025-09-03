package io.kodec

import java.io.BufferedReader
import java.lang.Long.numberOfTrailingZeros
import java.math.BigDecimal
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertFailsWith

class DoubleToDecimalTest: BasicChecker() {
    private val iterations = 100_000

    /*
     * Convert v to String and check whether it meets the specification.
     */
    private fun testDec(v: Double) {
        DoubleToDecimalChecker(v).check()
    }

    /*
     * Test around v, up to z values below and above v.
     * Don't care when v is at the extremes,
     * as any value returned by longBitsToDouble() is valid.
     */
    private fun testAround(v: Double, z: Int) {
        val bits: Long = v.toRawBits()
        for (i in -z .. z) {
            testDec(Double.fromBits(bits + i))
        }
    }

    @Test
    fun meta() {
        test { }

        assertFailsWith<AssertionError> {
            test {
                addError(null)
            }
        }
    }

    @Test
    fun testExtremeValues() = test {
        testDec(Double.NEGATIVE_INFINITY)
        testAround(-Double.MAX_VALUE, DoubleToDecimalChecker.Z)
        testAround(-java.lang.Double.MIN_NORMAL, DoubleToDecimalChecker.Z)
        testAround(-Double.MIN_VALUE, DoubleToDecimalChecker.Z)
        testDec(-0.0)
        testDec(0.0)
        testAround(Double.MIN_VALUE, DoubleToDecimalChecker.Z)
        testAround(java.lang.Double.MIN_NORMAL, DoubleToDecimalChecker.Z)
        testAround(Double.MAX_VALUE, DoubleToDecimalChecker.Z)
        testDec(Double.POSITIVE_INFINITY)
        testDec(Double.NaN)

        /*
         * Quiet NaNs have the most significant bit of the mantissa as 1,
         * while signaling NaNs have it as 0.
         * Exercise 4 combinations of quiet/signaling NaNs and
         * "positive/negative" NaNs
         */
        testDec(Double.fromBits(0x7FF8000000000001L))
        testDec(Double.fromBits(0x7FF0000000000001L))
        testDec(Double.fromBits(-0x7ffffffffffffL))
        testDec(Double.fromBits(-0xfffffffffffffL))

        /*
         * All values treated specially by Schubfach
         */
        for (c in 1 ..< DoubleToDecimalChecker.C_TINY) {
            testDec(c * Double.MIN_VALUE)
        }
    }

    /*
     * Some values close to powers of 10 are incorrectly rendered by older JDKs.
     * The rendering is either too long or it is not the closest decimal.
     */
    @Test
    fun testPowersOf10() = test {
        for (e in DoubleToDecimalChecker.E_MIN .. DoubleToDecimalChecker.E_MAX) {
            testAround(java.lang.Double.parseDouble("1e$e"), DoubleToDecimalChecker.Z)
        }
    }

    /*
     * Many values close to powers of 2 are incorrectly rendered by older JDKs.
     * The rendering is either too long or it is not the closest decimal.
     */
    @Test
    fun testPowersOf2() = test {
        run {
            var v = Double.MIN_VALUE
            while (v <= Double.MAX_VALUE) {
                testAround(v, DoubleToDecimalChecker.Z)
                v *= 2.0
            }
        }
    }

    /*
     * There are tons of doubles that are rendered incorrectly by older JDKs.
     * While the renderings correctly round back to the original value,
     * they are longer than needed or are not the closest decimal to the double.
     * Here are just a very few examples.
     */
    private val anomalies = arrayOf<String?>(
        /* Older JDKs render these with 18 digits! */"2.82879384806159E17", "1.387364135037754E18",
        "1.45800632428665E17",  /* Older JDKs render these longer than needed */

        "1.6E-322", "6.3E-322",
        "7.3879E20", "2.0E23", "7.0E22", "9.2E22",
        "9.5E21", "3.1E22", "5.63E21", "8.41E21",  /* Older JDKs do not render these as the closest */

        "9.9E-324", "9.9E-323",
        "1.9400994884341945E25", "3.6131332396758635E25",
        "2.5138990223946153E25",
    )

    @Test
    fun testSomeAnomalies() = test {
        for (dec in anomalies) {
            testDec(java.lang.Double.parseDouble(dec))
        }
    }

    /*
     * Values are from
     * Paxson V, "A Program for Testing IEEE Decimal-Binary Conversion"
     * tables 3 and 4
     */
    private val paxsonSignificands = doubleArrayOf(
        8511030020275656.0,
        5201988407066741.0,
        6406892948269899.0,
        8431154198732492.0,
        6475049196144587.0,
        8274307542972842.0,
        5381065484265332.0,
        6761728585499734.0,
        7976538478610756.0,
        5982403858958067.0,
        5536995190630837.0,
        7225450889282194.0,
        7225450889282194.0,
        8703372741147379.0,
        8944262675275217.0,
        7459803696087692.0,
        6080469016670379.0,
        8385515147034757.0,
        7514216811389786.0,
        8397297803260511.0,
        6733459239310543.0,
        8091450587292794.0,

        6567258882077402.0,
        6712731423444934.0,
        6712731423444934.0,
        5298405411573037.0,
        5137311167659507.0,
        6722280709661868.0,
        5344436398034927.0,
        8369123604277281.0,
        8995822108487663.0,
        8942832835564782.0,
        8942832835564782.0,
        8942832835564782.0,
        6965949469487146.0,
        6965949469487146.0,
        6965949469487146.0,
        7487252720986826.0,
        5592117679628511.0,
        8887055249355788.0,
        6994187472632449.0,
        8797576579012143.0,
        7363326733505337.0,
        8549497411294502.0,
    )

    private val paxsonExponents = intArrayOf(
        -342,
        -824,
        237,
        72,
        99,
        726,
        -456,
        -57,
        376,
        377,
        93,
        710,
        709,
        117,
        -1,
        -707,
        -381,
        721,
        -828,
        -345,
        202,
        -473,

        952,
        535,
        534,
        -957,
        -144,
        363,
        -169,
        -853,
        -780,
        -383,
        -384,
        -385,
        -249,
        -250,
        -251,
        548,
        164,
        665,
        690,
        588,
        272,
        -448,
    )

    @Test
    fun testPaxson() = test {
        for (i in paxsonSignificands.indices) {
            testDec(StrictMath.scalb(paxsonSignificands[i], paxsonExponents[i]))
        }
    }

    @Test
    fun testHardValues() = test {
        // These numbers were generated on purpose as hard test cases
        // and were obtained from the continued fraction of 2^q*10^(-k),
        // with k as in the Schubfach paper.
        val reader: BufferedReader = DoubleToDecimalChecker::class.java
            .getResourceAsStream("/hard_doubles.txt")?.bufferedReader()
            ?: error("test resource file not found")

        reader.useLines { lines ->
            for (line in lines) {
                if (line.isNotEmpty()) testDec(line.toDouble())
            }
        }
    }

    /*
     * Tests all integers of the form yx_xxx_000_000_000_000_000, y != 0.
     * These are all exact doubles.
     */
    @Test
    fun testLongs() = test {
        for (i in 10000 ..< 100000) {
            testDec(i * 1e15)
        }
    }

    /*
     * Tests all integers up to 1_000_000.
     * These are all exact doubles and exercise a fast path.
     */
    @Test
    fun testInts() = test {
        for (i in 0 .. 1000000) {
            testDec(i.toDouble())
        }
    }

    /*
     * 0.1, 0.2, ..., 999.9 and around
     */
    @Test
    fun testDeci() = test {
        for (i in 1 ..< 10000) {
            testAround(i / 1e1, 10)
        }
    }

    /*
     * 0.01, 0.02, ..., 99.99 and around
     */
    @Test
    fun testCenti() = test {
        for (i in 1 ..< 10000) {
            testAround(i / 1e2, 10)
        }
    }

    /*
     * 0.001, 0.002, ..., 9.999 and around
     */
    @Test
    fun testMilli() = test {
        for (i in 1 ..< 10000) {
            testAround(i / 1e3, 10)
        }
    }

    /*
     * Random doubles over the whole range
     */
    @Test
    fun testRandom() = test {
        repeat(iterations) {
            testDec(Double.fromBits(Random.nextLong()))
        }
    }

    /*
     * Random doubles over the integer range [0, 2^52).
     * These are all exact doubles and exercise the fast path (except 0).
     */
    @Test
    fun testRandomUnit() = test {
        repeat(iterations) {
            testDec((Random.nextLong() and (1L shl (DoubleToDecimalChecker.P - 1))).toDouble())
        }
    }

    /*
     * Random doubles over the range [0, 10^15) as "multiples" of 1e-3
     */
    @Test
    fun testRandomMilli() = test {
        repeat(iterations) {
            testDec(Random.nextLong() % 1000000000000000000L / 1e3)
        }
    }

    /*
     * Random doubles over the range [0, 10^15) as "multiples" of 1e-6
     */
    @Test
    fun testRandomMicro() = test {
        repeat(iterations) {
            testDec((Random.nextLong() and 0x7FFFFFFFFFFFFFFFL) / 1e6)
        }
    }

    /*
     * Values suggested by Guy Steele
     */
    @Test
    fun testRandomShortDecimals() = test {
        val e: Int = Random.nextInt(DoubleToDecimalChecker.E_MAX - DoubleToDecimalChecker.E_MIN + 1) + DoubleToDecimalChecker.E_MIN
        var pow10 = 1
        while (pow10 < 10000) {
            /* randomly generate an int in [pow10, 10 pow10) */
            testAround(java.lang.Double.parseDouble((Random.nextInt(9 * pow10) + pow10).toString() + "e" + e), DoubleToDecimalChecker.Z)
            pow10 *= 10
        }
    }

    @Test
    fun testConstants() = test {
        with(DoubleToDecimalChecker) {
            addOnFail(P == DoubleToDecimal.P, "P")
            addOnFail(W == DoubleToDecimal.W, "W")

            addOnFail(Q_MIN == DoubleToDecimal.Q_MIN, "Q_MIN")
            addOnFail(Q_MAX == DoubleToDecimal.Q_MAX, "Q_MAX")
            addOnFail(C_MIN == DoubleToDecimal.C_MIN, "C_MIN")
            addOnFail(C_MAX == DoubleToDecimal.C_MAX, "C_MAX")
            addOnFail(C_MIN.toDouble().toLong() == C_MIN, "C_MIN")
            addOnFail(C_MAX.toDouble().toLong() == C_MAX, "C_MAX")

            addOnFail(E_MIN == DoubleToDecimal.E_MIN, "E_MIN")
            addOnFail(E_MAX == DoubleToDecimal.E_MAX, "E_MAX")
            addOnFail(E_THR_Z == DoubleToDecimal.E_THR_Z, "E_THR_Z")
            addOnFail(E_THR_I == DoubleToDecimal.E_THR_I, "E_THR_I")
            addOnFail(K_MIN == DoubleToDecimal.K_MIN, "K_MIN")
            addOnFail(K_MAX == DoubleToDecimal.K_MAX, "K_MAX")

            addOnFail(C_TINY.toLong() == DoubleToDecimal.C_TINY, "C_TINY")
            addOnFail(H == DoubleToDecimal.H, "H")

            addOnFail(DoubleToDecimalChecker.MIN_VALUE.compareTo(BigDecimal(Double.MIN_VALUE)) == 0, "MIN_VALUE")
            addOnFail(
                DoubleToDecimalChecker.MIN_NORMAL.compareTo(BigDecimal(java.lang.Double.MIN_NORMAL)) == 0,
                "MIN_NORMAL"
            )
            addOnFail(DoubleToDecimalChecker.MAX_VALUE.compareTo(BigDecimal(Double.MAX_VALUE)) == 0, "MAX_VALUE")
        }
    }
}

private class DoubleToDecimalChecker(private val v: Double) : ToDecimalChecker(NumberToString.toString(v), BasicChecker()) {
    override fun eMin(): Int = E_MIN

    override fun eMax(): Int = E_MAX

    override fun h(): Int = H

    override fun maxStringLength(): Int = H + 7

    override fun toBigDecimal(): BigDecimal = BigDecimal(v)

    override fun recovers(bd: BigDecimal): Boolean = bd.toDouble() == v

    override fun recovers(s: String): Boolean = java.lang.Double.parseDouble(s) == v

    override fun hexString(): String = java.lang.Double.toHexString(v) + "D"

    override val isNegativeInfinity: Boolean get() = v == Double.NEGATIVE_INFINITY

    override val isPositiveInfinity: Boolean get() = v == Double.POSITIVE_INFINITY

    override val isMinusZero: Boolean get() = v.toRawBits() == 1L shl 63

    override val isPlusZero: Boolean get() = v.toRawBits() == 0x0000000000000000L

    override val isNaN: Boolean get() = v.isNaN()

    companion object {
        val P: Int = numberOfTrailingZeros((3.0).toRawBits()) + 2
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
