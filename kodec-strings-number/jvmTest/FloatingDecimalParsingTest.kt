package io.kodec

import java.math.BigDecimal
import java.util.concurrent.ThreadLocalRandom
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

    private companion object {
        const val NUM_RANDOM_TESTS = 100000
    }
}
