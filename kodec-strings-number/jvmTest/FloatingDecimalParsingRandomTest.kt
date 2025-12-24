package io.kodec

import io.kodec.buffers.asBuffer
import karamel.utils.enrichMessageOf
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class FloatingDecimalParsingRandomTest {
    @Test
    fun floats() {
        repeat(ITERATIONS) {
            val args = randomDec(forDouble = false)
            val expected: Float = args.decimal.toFloat()
            enrichMessageOf<Throwable>({ args.s }) {
                val actual: Float = FloatingDecimalParsing.parseFloat(args.s)
                assertEquals(expected, actual)
            }
        }
    }

    @Test
    fun doubles() {
        assertEquals(14752235318700E023, "14752235318700E023".encodeToByteArray().asBuffer().parseDouble().doubleValue())
        assertEquals(5.130262921845E178, "05130262921845000E+163".encodeToByteArray().asBuffer().parseDouble().doubleValue())
        repeat(ITERATIONS) {
            val args = randomDec(forDouble = true)
            val expected: Double = args.decimal.toDouble()
            enrichMessageOf<Throwable>({ "input: ${args.s}" }) {
                val actual: Double = FloatingDecimalParsing.parseDouble(args.s)
                assertEquals(expected, actual)
            }
        }
    }

    @JvmRecord
    private data class Args(val s: String, val decimal: BigDecimal)

    private companion object {
        /*
         * This class relies on the correctness of
         *      BigInteger string parsing, both decimal and hexadecimal
         *      BigDecimal floatValue() and doubleValue() conversions
         * and on the fact that the implementation of the BigDecimal conversions is
         * independent of the implementation in FloatingDecimal.
         * Hence, the expected values are those computed by BigDecimal,
         * while the actual values are those returned by FloatingDecimal.
         */
        val RANDOM = Random(4343)

        const val ITERATIONS = 1_000_000

        fun randomDec(forDouble: Boolean): Args {
            val sb = StringBuilder()
            val signLen = appendRandomSign(sb)
            val leadingZeros: Int = RANDOM.nextInt(4)
            appendZeros(sb, leadingZeros)
            val digits: Int = RANDOM.nextInt(if (forDouble) 24 else 12) + 1
            appendRandomDecDigits(sb, digits)
            val trailingZeros: Int = RANDOM.nextInt(4)
            appendZeros(sb, trailingZeros)
            var bd = BigDecimal(
                BigInteger(
                    sb.substring(0, signLen + leadingZeros + digits + trailingZeros),
                    10
                )
            )

            var p = 0
            if (RANDOM.nextInt(8) != 0) {  // 87.5% chance of point presence
                val pointPos: Int = RANDOM.nextInt(leadingZeros + digits + trailingZeros + 1)
                sb.insert(0 + signLen + pointPos, '.')
                p = -(leadingZeros + digits + trailingZeros - pointPos)
            }
            var e = 0
            if (RANDOM.nextInt(4) != 0) {  // 75% chance of explicit exponent
                val emax = if (forDouble) 325 else 46
                e = RANDOM.nextInt(-emax, emax)
                appendExponent(sb, e)
            }
//            appendRandomSuffix(sb)
            if (e + p >= 0) {
                bd = bd.multiply(BigDecimal.TEN.pow(e + p))
            } else {
                bd = bd.divide(BigDecimal.TEN.pow(-(e + p)))
            }
            return Args(sb.toString(), bd)
        }

        fun appendRandomSign(sb: StringBuilder): Int =
            when (RANDOM.nextInt(4)) {
                0 -> { sb.append('-'); 1 }
                1 -> { sb.append('+'); 1 }
                else -> 0
            }

        fun appendExponent(sb: StringBuilder, e: Int) {
            sb.append(if (RANDOM.nextBoolean()) 'e' else 'E')
            when {
                e < 0 -> sb.append('-')
                e == 0 -> appendRandomSign(sb)
                RANDOM.nextBoolean() -> sb.append('+')
            }
            appendZeros(sb, RANDOM.nextInt(2))
            sb.append(abs(e))
        }

        fun appendZeros(sb: StringBuilder, count: Int) {
            repeat(count) { sb.append('0') }
        }

        fun appendRandomDecDigits(sb: StringBuilder, count: Int) {
            var count = count
            sb.append(randomDecDigit(1))
            while (count > 1) {
                sb.append(randomDecDigit(0))
                --count
            }
        }

        fun randomDecDigit(min: Int): Char = Character.forDigit(RANDOM.nextInt(min, 10), 10).code.toChar()
    }
}
