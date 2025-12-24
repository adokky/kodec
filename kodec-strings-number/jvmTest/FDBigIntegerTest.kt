package io.kodec

import java.lang.Long.toHexString
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import kotlin.test.Test

class FDBigIntegerTest {
    private companion object {
        const val MAX_P5 = 413
        const val MAX_P2 = 65
        val LONG_SIGN_MASK = (1L shl 63)
        val FIVE: BigInteger = BigInteger.valueOf(5)
        val MUTABLE_ZERO = FDBigInteger(0)
        val IMMUTABLE_ZERO = FDBigInteger(0).makeImmutable()
        val IMMUTABLE_MILLION = genMillion1().makeImmutable()
        val IMMUTABLE_TEN18 = genTen18().makeImmutable()

        // data.length == 1, nWords == 1, offset == 0
        fun genMillion1(): FDBigInteger = FDBigInteger.valueOfPow52(6, 0).leftShift(6)

        // data.length == 2, nWords == 1, offset == 0
        fun genMillion2(): FDBigInteger = FDBigInteger.valueOfMulPow52(1000000L, 0, 0, FDBigInteger())

        // data.length == 2, nWords == 2, offset == 0
        fun genTen18(): FDBigInteger = FDBigInteger.valueOfPow52(18, 0).leftShift(18)
    }

    private fun toBigInteger(v: FDBigInteger): BigInteger = BigInteger(v.toByteArray())

    private fun FDBigInteger.multByPow52(e5: Int, e2: Int): FDBigInteger {
        return multByPow52(e5, e2, dst = FDBigInteger())
    }

    private fun mutable(hex: String, offset: Int): FDBigInteger {
        val chars = BigInteger(hex, 16).toString().toByteArray(StandardCharsets.US_ASCII)
        return FDBigInteger(0, chars, 0, chars.size).multByPow52(0, offset * 32)
    }

    private fun biPow52(p5: Int, p2: Int): BigInteger = FIVE.pow(p5).shiftLeft(p2)

    @Throws(Exception::class)
    private fun check(expected: BigInteger, actual: FDBigInteger, message: String?) {
        if (expected != toBigInteger(actual)) {
            throw Exception(message + " result " + actual + " expected " + expected.toString(16))
        }
    }

    @Throws(Exception::class)
    private fun testValueOfPow52(p5: Int, p2: Int) {
        check(
            biPow52(p5, p2), FDBigInteger.valueOfPow52(p5, p2),
            "valueOfPow52($p5,$p2)"
        )
    }

    @Test
    fun testValueOfPow52() {
        for (p5 in 0..MAX_P5) {
            for (p2 in 0..MAX_P2) {
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
            biPow52(p5, p2).multiply(bi), FDBigInteger.valueOfMulPow52(value, p5, p2, FDBigInteger()),
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
        for (p5 in 0..MAX_P5) {
            testValueOfMulPow52(0xFFFFFFFFL, p5)
            testValueOfMulPow52(0x123456789AL, p5)
            testValueOfMulPow52(0x7FFFFFFFFFFFFFFFL, p5)
            testValueOfMulPow52(-0xabcdfL, p5)
        }
    }

    private fun testLeftShift(t: FDBigInteger, shift: Int, isImmutable: Boolean) {
        val bt = toBigInteger(t)
        val r = t.leftShift(shift)
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

    private fun testQuoRemIteration(t: FDBigInteger, s: FDBigInteger) {
        val bt = toBigInteger(t)
        val bs = toBigInteger(s)
        val q = t.quoRemIteration(s)
        val qr = bt.divideAndRemainder(bs)
        if (BigInteger.valueOf(q.toLong()) != qr[0]) {
            throw Exception("quoRemIteration returns incorrect quo")
        }
        check(qr[1]!!.multiply(BigInteger.TEN), t, "quoRemIteration returns incorrect rem")
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
        val bt = toBigInteger(t)
        val bo = toBigInteger(o)
        val cmp = t.cmp(o)
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

    private fun testCmpPow52(t: FDBigInteger, p2: Int) {
        val o = FDBigInteger.valueOfPow52(0, p2)
        val bt = toBigInteger(t)
        val bo = biPow52(0, p2)
        val cmp = t.cmp(o)
        val bcmp = bt.compareTo(bo)
        if (bcmp != cmp) {
            throw Exception("cmp returns $cmp expected $bcmp")
        }
        check(bt, t, "cmp corrupts this")
        check(bo, o, "cmpPow5 corrupts other")
    }

    @Test
    fun testCmpPow52() {
        testCmpPow52(mutable("00000002", 1), 31)
        testCmpPow52(mutable("00000002", 1), 32)
        testCmpPow52(mutable("00000002", 1), 33)
        testCmpPow52(mutable("00000002", 1), 34)
        testCmpPow52(mutable("00000002", 1), 64)
        testCmpPow52(mutable("00000003", 1), 32)
        testCmpPow52(mutable("00000003", 1), 33)
        testCmpPow52(mutable("00000003", 1), 34)
    }

    private fun testAddAndCmp(t: FDBigInteger, x: FDBigInteger, y: FDBigInteger) {
        val bt = toBigInteger(t)
        val bx = toBigInteger(x)
        val by = toBigInteger(y)
        val cmp = t.addAndCmp(x, y)
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
        val bt = toBigInteger(t)
        val r = t.multBy10()
        if ((bt.signum() == 0 || !isImmutable) && r != t) {
            throw Exception("multBy10 of doesn't reuse its argument")
        }
        if (isImmutable) {
            check(bt, t, "multBy10 corrupts its argument")
        }
        check(bt.multiply(BigInteger.TEN), r, "multBy10 returns wrong result")
    }

//    @Ignore // Enable for more comprehensize but slow testing
    @Test
    fun testMultBy10() {
        for (p5 in 0..MAX_P5) {
            for (p2 in 0..MAX_P2) {
                // This strange way of creating a value ensures that it is mutable.
                val value = FDBigInteger.valueOfPow52(0, 0).multByPow52(p5, p2)
                testMultBy10(value, false)
                value.makeImmutable()
                testMultBy10(value, true)
            }
        }
    }

    private fun testMultByPow52(t: FDBigInteger, p5: Int, p2: Int) {
        val bt = toBigInteger(t)
        val r = t.multByPow52(p5, p2)
        if (bt.signum() == 0 && r != t) {
            throw Exception("multByPow52 of doesn't reuse its argument")
        }
        check(bt.multiply(biPow52(p5, p2)), r, "multByPow52 returns wrong result")
    }

//    @Ignore // Enable for more comprehensize but slow testing
    @Test
    fun testMultByPow52() {
        for (p5 in 0..MAX_P5) {
            for (p2 in 0..MAX_P2) {
                // This strange way of creating a value ensures that it is mutable.
                val value = FDBigInteger.valueOfPow52(0, 0).multByPow52(p5, p2)
                testMultByPow52(value, p5, p2)
            }
        }
    }
}
