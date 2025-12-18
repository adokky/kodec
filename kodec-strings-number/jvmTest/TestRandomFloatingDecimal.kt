package io.kodec

import karamel.utils.enrichMessageOf
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import java.util.stream.Stream
import java.util.stream.Stream.generate
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals

class TestRandomFloatingDecimal {
    @Test
    fun testRandomDecForFloat() = generate { randomDec(false) }.limit(samples.toLong()).test { args ->
        val expected = args.decimal.toFloat()
        val actual: Float = FloatingDecimalParsing.parseFloat(args.s)
        assertEquals(expected, actual)
    }

    @Test
    fun testRandomDecForDouble() = generate { randomDec(true) }.limit(samples.toLong()).test { args ->
        val expected = args.decimal.toDouble()
        val actual: Double = FloatingDecimalParsing.parseDouble(args.s)
        assertEquals(expected, actual)
    }

    @JvmRecord
    private data class Args(val s: String, val decimal: BigDecimal)

    private fun Stream<Args>.test(body: (Args) -> Unit) {
        forEach { args ->
            enrichMessageOf<Throwable>({ args.toString() }) {
                body(args)
            }
        }
    }

    companion object {
        /*
         * This class relies on the correctness of
         *      BigInteger string parsing, both decimal and hexadecimal
         *      BigDecimal floatValue() and doubleValue() conversions
         * and on the fact that the implementation of the BigDecimal conversions is
         * independent of the implementation in FloatingDecimal.
         * Hence, the expected values are those computed by BigDecimal,
         * while the actual values are those returned by FloatingDecimal.
         */
        private val RANDOM: Random = Random()
        private var samples = 0 // random samples per test

        private const val SAMPLES_PROP = "samples"

        init {
            val prop = System.getProperty(SAMPLES_PROP, "10000") // 10_000
            try {
                samples = prop.toInt()
                if (samples <= 0) {
                    throw NumberFormatException()
                }
            } catch (`_`: NumberFormatException) {
                throw IllegalArgumentException("-D$SAMPLES_PROP=$prop must specify a valid positive decimal integer.")
            }
        }

        private fun randomDec(forDouble: Boolean): Args {
            val sb = StringBuilder()
            val signLen: Int = appendRandomSign(sb)
            val leadingZeros: Int = RANDOM.nextInt(4)
            appendZeros(sb, leadingZeros)
            val digits: Int = RANDOM.nextInt(if (forDouble) 24 else 12) + 1
            appendRandomDecDigits(sb, digits)
            val trailingZeros: Int = RANDOM.nextInt(4)
            appendZeros(sb, trailingZeros)
            var bd = BigDecimal(
                BigInteger(
                    sb.substring(
                        0,
                        signLen + leadingZeros + digits + trailingZeros
                    ),
                    10
                )
            )

            var p = 0
            if (RANDOM.nextInt(8) != 0) {  // 87.5% chance of point presence
                val pointPos: Int = RANDOM.nextInt(leadingZeros + digits + trailingZeros + 1)
                sb.insert(signLen + pointPos, '.')
                p = -(leadingZeros + digits + trailingZeros - pointPos)
            }
            var e = 0
            if (RANDOM.nextInt(4) != 0) {  // 75% chance of explicit exponent
                val emax = if (forDouble) 325 else 46
                e = RANDOM.nextInt(-emax, emax)
                appendExponent(sb, e)
            }
            if (e + p >= 0) {
                bd = bd.multiply(BigDecimal.TEN.pow(e + p))
            } else {
                bd = bd.divide(BigDecimal.TEN.pow(-(e + p)))
            }
            return Args(sb.toString(), bd)
        }

        private fun appendRandomSign(sb: StringBuilder): Int = when (RANDOM.nextInt(4)) {
            0 -> { sb.append('-'); 1 }
            1 -> { sb.append('+'); 1 }
            else -> 0
        }

        private fun appendExponent(sb: StringBuilder, e: Int) {
            sb.append(if (RANDOM.nextBoolean()) 'e' else 'E')
            when {
                e < 0 -> sb.append('-')
                e == 0 -> appendRandomSign(sb)
                RANDOM.nextBoolean() -> sb.append('+')
            }
            appendZeros(sb, RANDOM.nextInt(2))
            sb.append(abs(e))
        }

        private fun appendZeros(sb: StringBuilder, count: Int) {
            repeat(count) {
                sb.append('0')
            }
        }

        private fun appendRandomDecDigits(sb: StringBuilder, count: Int) {
            var count = count
            sb.append(randomDecDigit(1))
            while (count > 1) {
                sb.append(randomDecDigit(0))
                --count
            }
        }

        private fun randomDecDigit(min: Int): Char {
            val c = Character.forDigit(RANDOM.nextInt(min, 10), 10).code
            return c.toChar()
        }
    }
}
