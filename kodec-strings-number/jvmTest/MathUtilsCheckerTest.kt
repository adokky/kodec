package io.kodec

import io.kodec.MathUtilsChecker.MARGIN
import java.math.BigInteger
import kotlin.test.Test

class MathUtilsCheckerTest : BasicChecker() {
    private fun gReason(e: Int): String = "g($e) is incorrect"

    private fun testG(e: Int) {
        val g1: Long = MathUtils.g1(e)
        val g0: Long = MathUtils.g0(e)
        // 2^62 <= g1 < 2^63, 0 < g0 < 2^63
        addOnFail((g1 ushr -2) == 1L && g0 > 0, gReason(e))

        val g: BigInteger = BigInteger.valueOf(g1).shiftLeft(63).or(BigInteger.valueOf(g0))
        // double check that 2^125 <= g < 2^126
        addOnFail(g.signum() > 0 && g.bitLength() == 126, gReason(e))

        addOnFail(MathUtilsChecker.g(e).compareTo(g) == 0, gReason(e))
    }

    @Test
    fun testG() = test {
        for (e in MathUtilsChecker.GE_MIN .. MathUtilsChecker.GE_MAX) {
            testG(e)
        }
    }

    private fun flog10threeQuartersPow2Reason(q: Int): String = "flog10threeQuartersPow2($q) is incorrect"

    @Test
    fun testFlog10threeQuartersPow2() = test {
        for (q in DoubleToDecimal.Q_MIN - MARGIN .. DoubleToDecimal.Q_MAX + MARGIN) {
            addOnFail(
                MathUtilsChecker.flog10threeQuartersPow2(q) == MathUtils.flog10threeQuartersPow2(q),
                flog10threeQuartersPow2Reason(q)
            )
        }
    }

    private fun flog10pow2Reason(q: Int): String = "flog10pow2($q) is incorrect"

    @Test
    fun testFlog10pow2() = test {
        for (q in DoubleToDecimal.Q_MIN - MARGIN .. DoubleToDecimal.Q_MAX + MARGIN) {
            addOnFail(
                MathUtilsChecker.flog10pow2(q) == MathUtils.flog10pow2(q),
                flog10pow2Reason(q)
            )
        }
    }

    private fun flog2pow10Reason(e: Int): String = "flog2pow10($e) is incorrect"

    @Test
    fun testFlog2pow10() = test {
        for (e in -DoubleToDecimal.K_MAX - MARGIN .. -DoubleToDecimal.K_MIN + MARGIN) {
            addOnFail(
                MathUtilsChecker.flog2pow10(e) == MathUtils.flog2pow10(e),
                flog2pow10Reason(e)
            )
        }
    }

    @Test
    fun testDecimalConstants() = test {
        addOnFail(MathUtilsChecker.GE_MIN == MathUtils.GE_MIN, "GE_MIN")
        addOnFail(MathUtilsChecker.GE_MAX == MathUtils.GE_MAX, "GE_MAX")
    }

    private fun pow10Reason(e: Int): String = "pow10($e) is incorrect"

    @Test
    fun testPow10() = test {
        addOnFail(MathUtilsChecker.N == MathUtils.N, "N")
        try {
            MathUtilsChecker.unsignedPowExact(10L, MathUtilsChecker.N + 1) // expected to throw
            addOnFail(false, "N")
        } catch (`_`: RuntimeException) {
        }
        for (e in 0 .. MathUtilsChecker.N) {
            addOnFail(MathUtilsChecker.unsignedPowExact(10L, e) == MathUtils.pow10(e), pow10Reason(e))
        }
    }
}