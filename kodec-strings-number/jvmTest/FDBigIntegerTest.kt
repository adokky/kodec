package io.kodec

import java.lang.Long.toHexString
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import kotlin.test.Test

class FDBigIntegerTest {
    private companion object {
        const val MAX_P5 = 413
        const val MAX_P2 = 65

        private const val LONG_SIGN_MASK = (1L shl 63)

        private val FIVE = BigInteger.valueOf(5)

        private val MUTABLE_ZERO: FDBigInteger =
            FDBigInteger.valueOfPow52(0, 0).leftInplaceSub(FDBigInteger.valueOfPow52(0, 0))
        private val IMMUTABLE_ZERO: FDBigInteger =
            FDBigInteger.valueOfPow52(0, 0).leftInplaceSub(FDBigInteger.valueOfPow52(0, 0))
        private val IMMUTABLE_MILLION: FDBigInteger = genMillion1()
        private val IMMUTABLE_BILLION: FDBigInteger = genBillion1()
        private val IMMUTABLE_TEN18: FDBigInteger = genTen18()

        init {
            IMMUTABLE_ZERO.isImmutable = true
            IMMUTABLE_MILLION.isImmutable = true
            IMMUTABLE_BILLION.isImmutable = true
            IMMUTABLE_TEN18.isImmutable = true
        }

        // data.length == 1, nWords == 1, offset == 0
        private fun genMillion1(): FDBigInteger = FDBigInteger.valueOfPow52(6, 0).leftShift(6)

        // data.length == 2, nWords == 1, offset == 0
        private fun genMillion2(): FDBigInteger = FDBigInteger.valueOfMulPow52(1000000L, 0, 0)

        // data.length == 1, nWords == 1, offset == 0
        private fun genBillion1(): FDBigInteger = FDBigInteger.valueOfPow52(9, 0).leftShift(9)

        // data.length == 2, nWords == 2, offset == 0
        private fun genTen18(): FDBigInteger = FDBigInteger.valueOfPow52(18, 0).leftShift(18)
    }

    private fun mutable(hex: String?, offset: Int): FDBigInteger {
        val chars: ByteArray = BigInteger(hex, 16).toString().toByteArray(StandardCharsets.US_ASCII)
        return FDBigInteger(0, chars, 0, chars.size).multByPow52(0, offset * 32)
    }

    private fun biPow52(p5: Int, p2: Int): BigInteger = FIVE.pow(p5).shiftLeft(p2)

    private fun check(expected: BigInteger, actual: FDBigInteger, message: String?) {
        if (expected != actual.toBigInteger()) {
            throw Exception(message.toString() + " result " + actual.toHexString() + " expected " + expected.toString(16))
        }
    }

    private fun testValueOfPow52(p5: Int, p2: Int) {
        check(
            biPow52(p5, p2), FDBigInteger.valueOfPow52(p5, p2),
            "valueOfPow52($p5,$p2)"
        )
    }

    @Test
    fun testValueOfPow52() {
        for (p5 in 0 .. MAX_P5) {
            for (p2 in 0 .. MAX_P2) {
                testValueOfPow52(p5, p2)
            }
        }
    }

    private fun testValueOfMulPow52(value: Long, p5: Int, p2: Int) {
        var bi = BigInteger.valueOf(value and LONG_SIGN_MASK.inv())
        if (value < 0) {
            bi = bi.setBit(63)
        }
        check(
            biPow52(p5, p2).multiply(bi), FDBigInteger.valueOfMulPow52(value, p5, p2),
            "valueOfMulPow52(" + toHexString(value) + "." + p5 + "," + p2 + ")"
        )
    }

    private fun testValueOfMulPow52(value: Long, p5: Int) {
        testValueOfMulPow52(value, p5, 0)
        testValueOfMulPow52(value, p5, 1)
        testValueOfMulPow52(value, p5, 30)
        testValueOfMulPow52(value, p5, 31)
        testValueOfMulPow52(value, p5, 33)
        testValueOfMulPow52(value, p5, 63)
    }

    @Test
    fun testValueOfMulPow52() {
        for (p5 in 0 .. MAX_P5) {
            testValueOfMulPow52(0xFFFFFFFFL, p5)
            testValueOfMulPow52(0x123456789AL, p5)
            testValueOfMulPow52(0x7FFFFFFFFFFFFFFFL, p5)
            testValueOfMulPow52(-0xabcdfL, p5)
        }
    }

    private fun testLeftShift(t: FDBigInteger, shift: Int, isImmutable: Boolean) {
        val bt = t.toBigInteger()
        val r: FDBigInteger = t.leftShift(shift)
        if ((bt.signum() == 0 || shift == 0 || !isImmutable) && r != t) {
            throw Exception("leftShift doesn't reuse its argument")
        }
        if (isImmutable) {
            check(bt, t, "leftShift corrupts its argument")
        }
        check(bt.shiftLeft(shift), r, "leftShift returns wrong result")
    }

    @Test
    fun testLeftShift() {
        testLeftShift(IMMUTABLE_ZERO, 0, true)
        testLeftShift(IMMUTABLE_ZERO, 10, true)
        testLeftShift(MUTABLE_ZERO, 0, false)
        testLeftShift(MUTABLE_ZERO, 10, false)

        testLeftShift(IMMUTABLE_MILLION, 0, true)
        testLeftShift(IMMUTABLE_MILLION, 1, true)
        testLeftShift(IMMUTABLE_MILLION, 12, true)
        testLeftShift(IMMUTABLE_MILLION, 13, true)
        testLeftShift(IMMUTABLE_MILLION, 32, true)
        testLeftShift(IMMUTABLE_MILLION, 33, true)
        testLeftShift(IMMUTABLE_MILLION, 44, true)
        testLeftShift(IMMUTABLE_MILLION, 45, true)

        testLeftShift(genMillion1(), 0, false)
        testLeftShift(genMillion1(), 1, false)
        testLeftShift(genMillion1(), 12, false)
        testLeftShift(genMillion1(), 13, false)
        testLeftShift(genMillion1(), 25, false)
        testLeftShift(genMillion1(), 26, false)
        testLeftShift(genMillion1(), 32, false)
        testLeftShift(genMillion1(), 33, false)
        testLeftShift(genMillion1(), 44, false)
        testLeftShift(genMillion1(), 45, false)

        testLeftShift(genMillion2(), 0, false)
        testLeftShift(genMillion2(), 1, false)
        testLeftShift(genMillion2(), 12, false)
        testLeftShift(genMillion2(), 13, false)
        testLeftShift(genMillion2(), 25, false)
        testLeftShift(genMillion2(), 26, false)
        testLeftShift(genMillion2(), 32, false)
        testLeftShift(genMillion2(), 33, false)
        testLeftShift(genMillion2(), 44, false)
        testLeftShift(genMillion2(), 45, false)
    }

    @Suppress("SameParameterValue")
    private fun testQuoRemIteration(t: FDBigInteger, s: FDBigInteger) {
        val bt = t.toBigInteger()
        val bs = s.toBigInteger()
        val q: Int = t.quoRemIteration(s)
        val qr: Array<BigInteger> = bt.divideAndRemainder(bs)
        if (!BigInteger.valueOf(q.toLong()).equals(qr[0])) {
            throw Exception("quoRemIteration returns incorrect quo")
        }
        check(qr[1].multiply(BigInteger.TEN), t, "quoRemIteration returns incorrect rem")
    }

    @Test
    fun testQuoRemIteration() {
        // IMMUTABLE_TEN18 == 0de0b6b3a7640000
        // q = 0
        testQuoRemIteration(mutable("00000001", 0), IMMUTABLE_TEN18)
        testQuoRemIteration(mutable("00000001", 1), IMMUTABLE_TEN18)
        testQuoRemIteration(mutable("0de0b6b2", 1), IMMUTABLE_TEN18)
        // q = 1 -> q = 0
        testQuoRemIteration(mutable("0de0b6b3", 1), IMMUTABLE_TEN18)
        testQuoRemIteration(mutable("0de0b6b3a763FFFF", 0), IMMUTABLE_TEN18)
        // q = 1
        testQuoRemIteration(mutable("0de0b6b3a7640000", 0), IMMUTABLE_TEN18)
        testQuoRemIteration(mutable("0de0b6b3FFFFFFFF", 0), IMMUTABLE_TEN18)
        testQuoRemIteration(mutable("8ac72304", 1), IMMUTABLE_TEN18)
        testQuoRemIteration(mutable("0de0b6b400000000", 0), IMMUTABLE_TEN18)
        testQuoRemIteration(mutable("8ac72305", 1), IMMUTABLE_TEN18)
        // q = 18
        testQuoRemIteration(mutable("FFFFFFFF", 1), IMMUTABLE_TEN18)
    }

    private fun testCmp(t: FDBigInteger, o: FDBigInteger) {
        val bt = t.toBigInteger()
        val bo = o.toBigInteger()
        val cmp: Int = t.cmp(o)
        val bcmp = bt.compareTo(bo)
        if (bcmp != cmp) {
            throw Exception("cmp returns $cmp expected $bcmp")
        }
        check(bt, t, "cmp corrupts this")
        check(bo, o, "cmp corrupts other")
        if (o.cmp(t) != -cmp) {
            throw Exception("asymmetrical cmp")
        }
        check(bt, t, "cmp corrupts this")
        check(bo, o, "cmp corrupts other")
    }

    @Test
    fun testCmp() {
        testCmp(mutable("FFFFFFFF", 0), mutable("100000000", 0))
        testCmp(mutable("FFFFFFFF", 0), mutable("1", 1))
        testCmp(mutable("5", 0), mutable("6", 0))
        testCmp(mutable("5", 0), mutable("5", 0))
        testCmp(mutable("5000000001", 0), mutable("500000001", 0))
        testCmp(mutable("5000000001", 0), mutable("6", 1))
        testCmp(mutable("5000000001", 0), mutable("5", 1))
        testCmp(mutable("5000000000", 0), mutable("5", 1))
    }

    @Suppress("SameParameterValue")
    private fun testCmpPow52(t: FDBigInteger, p5: Int, p2: Int) {
        val o = FDBigInteger.valueOfPow52(p5, p2)
        val bt = t.toBigInteger()
        val bo: BigInteger = biPow52(p5, p2)
        val cmp: Int = t.cmp(o)
        val bcmp = bt.compareTo(bo)
        if (bcmp != cmp) {
            throw Exception("cmpPow52 returns $cmp expected $bcmp")
        }
        check(bt, t, "cmpPow52 corrupts this")
        check(bo, o, "cmpPow5 corrupts other")
    }

    @Test
    fun testCmpPow52() {
        testCmpPow52(mutable("00000002", 1), 0, 31)
        testCmpPow52(mutable("00000002", 1), 0, 32)
        testCmpPow52(mutable("00000002", 1), 0, 33)
        testCmpPow52(mutable("00000002", 1), 0, 34)
        testCmpPow52(mutable("00000002", 1), 0, 64)
        testCmpPow52(mutable("00000003", 1), 0, 32)
        testCmpPow52(mutable("00000003", 1), 0, 33)
        testCmpPow52(mutable("00000003", 1), 0, 34)
    }

    private fun testAddAndCmp(t: FDBigInteger, x: FDBigInteger, y: FDBigInteger) {
        val bt = t.toBigInteger()
        val bx = x.toBigInteger()
        val by = y.toBigInteger()
        val cmp: Int = t.addAndCmp(x, y)
        val bcmp = bt.compareTo(bx.add(by))
        if (bcmp != cmp) {
            throw Exception("addAndCmp returns $cmp expected $bcmp")
        }
        check(bt, t, "addAndCmp corrupts this")
        check(bx, x, "addAndCmp corrupts x")
        check(by, y, "addAndCmp corrupts y")
    }

    @Test
    fun testAddAndCmp() {
        testAddAndCmp(MUTABLE_ZERO, MUTABLE_ZERO, MUTABLE_ZERO)
        testAddAndCmp(mutable("00000001", 0), MUTABLE_ZERO, MUTABLE_ZERO)
        testAddAndCmp(mutable("00000001", 0), mutable("00000001", 0), MUTABLE_ZERO)
        testAddAndCmp(mutable("00000001", 0), MUTABLE_ZERO, mutable("00000001", 0))
        testAddAndCmp(mutable("00000001", 0), mutable("00000002", 0), MUTABLE_ZERO)
        testAddAndCmp(mutable("00000001", 0), MUTABLE_ZERO, mutable("00000002", 0))
        testAddAndCmp(mutable("00000001", 2), mutable("FFFFFFFF", 0), mutable("FFFFFFFF", 0))
        testAddAndCmp(mutable("00000001", 0), mutable("00000001", 1), mutable("00000001", 0))

        testAddAndCmp(mutable("00000001", 2), mutable("0F0F0F0F80000000", 1), mutable("F0F0F0F080000000", 1))
        testAddAndCmp(mutable("00000001", 2), mutable("0F0F0F0E80000000", 1), mutable("F0F0F0F080000000", 1))

        testAddAndCmp(mutable("00000002", 1), mutable("0000000180000000", 1), mutable("0000000280000000", 1))
        testAddAndCmp(mutable("00000003", 1), mutable("0000000180000000", 1), mutable("0000000280000000", 1))
        testAddAndCmp(mutable("00000004", 1), mutable("0000000180000000", 1), mutable("0000000280000000", 1))
        testAddAndCmp(mutable("00000005", 1), mutable("0000000180000000", 1), mutable("0000000280000000", 1))

        testAddAndCmp(mutable("00000001", 2), mutable("8000000000000000", 0), mutable("8000000000000000", 0))
        testAddAndCmp(mutable("00000001", 2), mutable("8000000000000000", 0), mutable("8000000000000001", 0))
        testAddAndCmp(mutable("00000002", 2), mutable("8000000000000000", 0), mutable("8000000000000000", 0))
        testAddAndCmp(mutable("00000003", 2), mutable("8000000000000000", 0), mutable("8000000000000000", 0))
    }

    private fun testMultBy10(t: FDBigInteger, isImmutable: Boolean) {
        val bt = t.toBigInteger()
        val r: FDBigInteger = t.multBy10()
        if ((bt.signum() == 0 || !isImmutable) && r != t) {
            throw Exception("multBy10 of doesn't reuse its argument")
        }
        if (isImmutable) {
            check(bt, t, "multBy10 corrupts its argument")
        }
        check(bt.multiply(BigInteger.TEN), r, "multBy10 returns wrong result")
    }

    @Test
    fun testMultBy10() {
        for (p5 in 0 .. MAX_P5) {
            for (p2 in 0 .. MAX_P2) {
                // This strange way of creating a value ensures that it is mutable.
                val value: FDBigInteger = FDBigInteger.valueOfPow52(0, 0).multByPow52(p5, p2)
                testMultBy10(value, false)
                value.isImmutable = true
                testMultBy10(value, true)
            }
        }
    }


    private fun testMultByPow52(t: FDBigInteger, p5: Int, p2: Int) {
        val bt = t.toBigInteger()
        val r: FDBigInteger = t.multByPow52(p5, p2)
        if (bt.signum() == 0 && r != t) {
            throw Exception("multByPow52 of doesn't reuse its argument")
        }
        check(bt.multiply(biPow52(p5, p2)), r, "multByPow52 returns wrong result")
    }

    @Test
    fun testMultByPow52() {
        for (p5 in 0 .. MAX_P5) {
            for (p2 in 0 .. MAX_P2) {
                // This strange way of creating a value ensures that it is mutable.
                val value: FDBigInteger = FDBigInteger.valueOfPow52(0, 0).multByPow52(p5, p2)
                testMultByPow52(value, p5, p2)
            }
        }
    }

    private fun testLeftInplaceSub(left: FDBigInteger, right: FDBigInteger, isImmutable: Boolean) {
        val biLeft = left.toBigInteger()
        val biRight = right.toBigInteger()
        val diff: FDBigInteger = left.leftInplaceSub(right)
        if (!isImmutable && diff !== left) {
            throw Exception("leftInplaceSub of doesn't reuse its argument")
        }
        if (isImmutable) {
            check(biLeft, left, "leftInplaceSub corrupts its left immutable argument")
        }
        check(biRight, right, "leftInplaceSub corrupts its right argument")
        check(biLeft.subtract(biRight), diff, "leftInplaceSub returns wrong result")
    }

    @Test
    fun testLeftInplaceSub() {
        for (p5 in 0 .. MAX_P5)
        for (p2 in 0 .. MAX_P2)
        for (p5r in 0 .. p5 step 10)
        for (p2r in 0 .. p2 step 10) {
            // This strange way of creating a value ensures that it is mutable.
            var left = FDBigInteger.valueOfPow52(0, 0).multByPow52(p5, p2)
            val right = FDBigInteger.valueOfPow52(0, 0).multByPow52(p5r, p2r)
            testLeftInplaceSub(left, right, false)
            left = FDBigInteger.valueOfPow52(0, 0).multByPow52(p5, p2)
            left.isImmutable = true
            testLeftInplaceSub(left, right, true)
        }
    }

    private fun testRightInplaceSub(left: FDBigInteger, right: FDBigInteger, isImmutable: Boolean) {
        val biLeft = left.toBigInteger()
        val biRight = right.toBigInteger()
        val diff: FDBigInteger = left.rightInplaceSub(right)
        if (!isImmutable && diff !== right) {
            throw Exception("rightInplaceSub of doesn't reuse its argument")
        }
        check(biLeft, left, "leftInplaceSub corrupts its left argument")
        if (isImmutable) {
            check(biRight, right, "leftInplaceSub corrupts its right immutable argument")
        }
        try {
            check(biLeft.subtract(biRight), diff, "rightInplaceSub returns wrong result")
        } catch (e: Exception) {
            println("$biLeft - $biRight = ${biLeft.subtract(biRight)}")
            throw e
        }
    }

    @Test
    fun testRightInplaceSub() {
        for (p5 in 0 .. MAX_P5)
        for (p2 in 0 .. MAX_P2)
        for (p5r in 0 .. p5 step 10)
        for (p2r in 0 .. p2 step 10) {
            // This strange way of creating a value ensures that it is mutable.
            val left = FDBigInteger.valueOfPow52(0, 0).multByPow52(p5, p2)
            var right = FDBigInteger.valueOfPow52(0, 0).multByPow52(p5r, p2r)
            testRightInplaceSub(left, right, isImmutable = false)
            right = FDBigInteger.valueOfPow52(0, 0).multByPow52(p5r, p2r)
            right.isImmutable = true
            testRightInplaceSub(left, right, isImmutable = true)
        }
    }

    private fun FDBigInteger.toBigInteger(): BigInteger {
        val magnitude = ByteArray(nWords * 4 + 1)
        for (i in 0 ..< nWords) {
            val w: Int = data[i]
            magnitude[magnitude.size - 4 * i - 1] = w.toByte()
            magnitude[magnitude.size - 4 * i - 2] = (w shr 8).toByte()
            magnitude[magnitude.size - 4 * i - 3] = (w shr 16).toByte()
            magnitude[magnitude.size - 4 * i - 4] = (w shr 24).toByte()
        }
        return BigInteger(magnitude).shiftLeft(offset * 32)
    }

    private fun FDBigInteger.toHexString(): String {
        if (nWords == 0) return "0"

        val sb = StringBuilder((nWords + offset) * 8)
        for (i in nWords - 1 downTo 0) {
            val subStr = Integer.toHexString(data[i])
            for (j in subStr.length .. 7) {
                sb.append('0')
            }
            sb.append(subStr)
        }
        for (i in offset downTo 1) {
            sb.append("00000000")
        }
        return sb.toString()
    }
}
