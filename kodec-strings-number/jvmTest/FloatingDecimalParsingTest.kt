package io.kodec

import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.abs
import kotlin.math.nextUp
import kotlin.test.Test
import kotlin.test.assertEquals


class FloatingDecimalParsingTest {
    /*
     * The tests rely on the different conversion implementations
     * in FloatDecimal and BigDecimal.
     */
    @Test
    fun testParseDouble() {
        val rnd = ThreadLocalRandom.current()

        repeat(NUM_RANDOM_TESTS) {
            val d = doubleArrayOf(
                rnd.nextLong().toDouble(),
                rnd.nextGaussian(),
                rnd.nextDouble() * Double.MAX_VALUE,
            )
            for (v in d) {
                val dec: String = v.toString()
                assertEquals(BigDecimal(dec).toDouble(), FloatingDecimalParsing.parseDouble(dec))

                val bd = BigDecimal(v)
                val full: String = bd.toString()
                if (full.length <= FloatingDecimalParsing.MAX_DIGITS) {
                    assertEquals(bd.toDouble(), FloatingDecimalParsing.parseDouble(full))
                }
            }
        }
    }

    @Test
    fun testParseFloat() {
        val rnd = ThreadLocalRandom.current()

        repeat(NUM_RANDOM_TESTS) {
            val f = floatArrayOf(
                rnd.nextLong().toFloat(),
                rnd.nextGaussian().toFloat(),
                rnd.nextFloat() * Float.MAX_VALUE
            )
            for (v in f) {
                val dec: String = v.toString()
                assertEquals(BigDecimal(dec).toFloat(), FloatingDecimalParsing.parseFloat(dec))

                val bd = BigDecimal(v.toDouble())
                val full: String = bd.toString()
                if (full.length <= FloatingDecimalParsing.MAX_DIGITS) {
                    assertEquals(bd.toFloat(), FloatingDecimalParsing.parseFloat(full))
                }
            }
        }
    }

    private val HALF: BigDecimal? = BigDecimal.valueOf(0.5)

    private fun fail(v: String?, n: Double) {
        throw AssertionError("Double.parseDouble failed. String:$v Result:$n")
    }

    private fun check(v: String) {
        val n = v.toDouble()
        val isNegativeN = n < 0 || n == 0.0 && 1 / n < 0
        val na = abs(n)
        var s = v.trim { it <= ' ' }.lowercase(Locale.getDefault())
        when (s[s.length - 1]) {
            'd', 'f' -> s = s.dropLast(1)
        }
        var isNegative = false
        if (s[0] == '+') {
            s = s.substring(1)
        } else if (s[0] == '-') {
            s = s.substring(1)
            isNegative = true
        }
        if (s == "nan") {
            if (!java.lang.Double.isNaN(n)) fail(v, n)
            return
        }
        if (java.lang.Double.isNaN(n)) {
            fail(v, n)
        }
        if (isNegativeN != isNegative) fail(v, n)
        if (s == "infinity") {
            if (na != Double.POSITIVE_INFINITY) fail(v, n)
            return
        }
        var bd: BigDecimal
        if (s.startsWith("0x")) {
            s = s.substring(2)
            val indP = s.indexOf('p')
            var exp = s.substring(indP + 1).toLong()
            val indD = s.indexOf('.')
            val significand: String?
            if (indD >= 0) {
                significand = s.take(indD) + s.substring(indD + 1, indP)
                exp -= (4 * (indP - indD - 1)).toLong()
            } else {
                significand = s.take(indP)
            }
            bd = BigDecimal(BigInteger(significand, 16))
            if (exp >= 0) {
                bd = bd.multiply(BigDecimal.valueOf(2).pow(exp.toInt()))
            } else {
                bd = bd.divide(BigDecimal.valueOf(2).pow(-exp.toInt()))
            }
        } else {
            bd = BigDecimal(s)
        }
        val l: BigDecimal?
        val u: BigDecimal?
        if (java.lang.Double.isInfinite(na)) {
            l = BigDecimal(Double.MAX_VALUE).add(
                BigDecimal(Math.ulp(Double.MAX_VALUE)).multiply(HALF)
            )
            u = null
        } else {
            l = BigDecimal(na).subtract(BigDecimal(Math.ulp((-na).nextUp())).multiply(HALF))
            u = BigDecimal(na).add(BigDecimal(Math.ulp(n)).multiply(HALF))
        }
        val cmpL = bd.compareTo(l)
        val cmpU = if (u != null) bd.compareTo(u) else -1
        if ((java.lang.Double.doubleToLongBits(n) and 1L) != 0L) {
            if (cmpL <= 0 || cmpU >= 0) fail(v, n)
        } else {
            if (cmpL < 0 || cmpU > 0) fail(v, n)
        }
    }

    private fun check(`val`: String, expected: Double) {
        val n = `val`.toDouble()
        if (n != expected) fail(`val`, n)
        check(`val`)
    }

    @Test
    fun rudimentaryTest() {
        check(Double.MIN_VALUE.toString(), Double.MIN_VALUE)
        check(Double.MAX_VALUE.toString(), Double.MAX_VALUE)

        check("10", 10.0)
        check("10.0", 10.0)
        check("10.01", 10.01)

        check("-10", -10.0)
        check("-10.00", -10.0)
        check("-10.01", -10.01)
    }

    /**
     * For each subnormal power of two, test at boundaries of
     * region that should convert to that value.
     */
    @Test
    fun testSubnormalPowers() {
        var failed = false
        val TWO = BigDecimal.valueOf(2)
        // An ulp is the same for all subnormal values
        val ulp_BD = BigDecimal(Double.MIN_VALUE)

        // Test subnormal powers of two (except Double.MIN_VALUE)
        for (i in -1073..-1022) {
            val d = Math.scalb(1.0, i)

            /*
             * The region [d - ulp/2, d + ulp/2] should round to d.
             */
            val d_BD = BigDecimal(d)

            val lowerBound = d_BD.subtract(ulp_BD.divide(TWO))
            val upperBound = d_BD.add(ulp_BD.divide(TWO))

            val convertedLowerBound = lowerBound.toString().toDouble()
            val convertedUpperBound = upperBound.toString().toDouble()
            if (convertedLowerBound != d) {
                failed = true
                System.err.printf(
                    "2^%d lowerBound converts as %a %s%n",
                    i, convertedLowerBound, lowerBound
                )
            }
            if (convertedUpperBound != d) {
                failed = true
                System.out.printf(
                    "2^%d upperBound converts as %a %s%n",
                    i, convertedUpperBound, upperBound
                )
            }
        }
        /*
         * Double.MIN_VALUE
         * The region ]0.5*Double.MIN_VALUE, 1.5*Double.MIN_VALUE[ should round to Double.MIN_VALUE .
         */
        val minValue = BigDecimal(Double.MIN_VALUE)
        if (minValue.multiply(BigDecimal(0.5)).toString().toDouble() != 0.0) {
            failed = true
            System.err.printf("0.5*MIN_VALUE doesn't convert 0%n")
        }
        if (minValue.multiply(BigDecimal(0.50000000001)).toString().toDouble() != Double.MIN_VALUE) {
            failed = true
            System.err.printf("0.50000000001*MIN_VALUE doesn't convert to MIN_VALUE%n")
        }
        if (minValue.multiply(BigDecimal(1.49999999999)).toString().toDouble() != Double.MIN_VALUE) {
            failed = true
            System.err.printf("1.49999999999*MIN_VALUE doesn't convert to MIN_VALUE%n")
        }
        if (minValue.multiply(BigDecimal(1.5)).toString().toDouble() != 2 * Double.MIN_VALUE) {
            failed = true
            System.err.printf("1.5*MIN_VALUE doesn't convert to 2*MIN_VALUE%n")
        }

        if (failed) throw AssertionError("Inconsistent conversion")
    }

    /**
     * For each power of two, test at boundaries of
     * region that should convert to that value.
     */
    @Test
    fun testPowers() {
        for (i in -1074..1023) {
            val d = Math.scalb(1.0, i)
            val d_BD = BigDecimal(d)

            val lowerBound = d_BD.subtract(BigDecimal(Math.ulp((-d).nextUp())).multiply(HALF))
            val upperBound = d_BD.add(BigDecimal(Math.ulp(d)).multiply(HALF))

            check(lowerBound.toString())
            check(upperBound.toString())
        }
        check(BigDecimal(Double.MAX_VALUE).add(BigDecimal(Math.ulp(Double.MAX_VALUE)).multiply(HALF)).toString())
    }

    @Test
    fun testStrictness() {
        val expected = 6.63123685E-316
        //        final double expected = 0x0.0000008000001p-1022;
        var failed = false
        var conversion: Double
        var sum = 0.0 // Prevent conversion from being optimized away

        //2^-1047 + 2^-1075 rounds to 2^-1047
        val decimal =
            "6.631236871469758276785396630275967243399099947355303144249971758736286630139265439618068200788048744105960420552601852889715006376325666595539603330361800519107591783233358492337208057849499360899425128640718856616503093444922854759159988160304439909868291973931426625698663157749836252274523485312442358651207051292453083278116143932569727918709786004497872322193856150225415211997283078496319412124640111777216148110752815101775295719811974338451936095907419622417538473679495148632480391435931767981122396703443803335529756003353209830071832230689201383015598792184172909927924176339315507402234836120730914783168400715462440053817592702766213559042115986763819482654128770595766806872783349146967171293949598850675682115696218943412532098591327667236328125E-316"

        for (i in 0..12000) {
            conversion = decimal.toDouble()
            sum += conversion
            if (conversion != expected) {
                failed = true
                System.out.printf(
                    "Iteration %d converts as %a%n",
                    i, conversion
                )
            }
        }

        if (failed) {
            println("Sum = $sum")
            throw AssertionError("Inconsistent conversion")
        }
    }

    private companion object {
        const val NUM_RANDOM_TESTS = 1_000_000
    }
}
