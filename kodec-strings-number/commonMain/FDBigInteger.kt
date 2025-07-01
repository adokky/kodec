package io.kodec

import io.kodec.buffers.MutableBuffer
import karamel.utils.asInt
import karamel.utils.assertionsEnabled
import kotlin.math.max

// https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/jdk/internal/math/FDBigInteger.java
/**
 * A simple big integer package specifically for floating point base conversion.
 */
internal class FDBigInteger {
    // value: data[0] is least significant
    private var data: IntArray

    // number of the least significant zero padding ints
    private var offset: Int

    // if nWords==0 -> this FDBigInteger is zero
    // data[nWords-1]!=0, all values above are zero
    private var nWords: Int

    var isImmutable = false

    private fun checkInvariants() {
        require(offset >= 0)
        require(nWords in 0..data.size)
        if (nWords == 0) require(offset == 0)
        if (nWords > 0) require(data[nWords - 1] != 0)
        for (i in nWords until data.size) require(data[i] == 0)
    }

    /**
     * Constructs an `FDBigInteger` from data and padding. The
     * `data` parameter has the least significant `int` at
     * the zeroth index. The `offset` parameter gives the number of
     * zero `int`s to be inferred below the least significant element
     * of `data`.
     *
     * @param data An array containing all non-zero `int`s of the value.
     * @param offset An offset indicating the number of zero `int`s to pad
     * below the least significant element of `data`.
     */
    private constructor(data: IntArray, offset: Int) {
        this.data = data
        this.offset = offset
        this.nWords = data.size
        trimLeadingZeros()
        if (assertionsEnabled) checkInvariants()
    }

    /**
     * Constructs an `FDBigInteger` from a starting value and some
     * decimal digits.
     *
     * @param lValue The starting value.
     * @param digits The decimal digits.
     * @param kDigits The initial index into `digits`.
     * @param nDigits The final index into `digits`.
     */
    constructor(lValue: Long, digits: ByteArray, kDigits: Int, nDigits: Int) {
        if (assertionsEnabled) {
            require(kDigits in 0..nDigits && nDigits <= digits.size)
            for (i in 0 until nDigits) require(digits[i] in '0'.code..'9'.code)
        }

        val n = max(((nDigits + 8) / 9).toDouble(), 2.0).toInt() // estimate size needed.
        data = IntArray(n) // allocate enough space
        data[0] = lValue.toInt() // starting value
        data[1] = (lValue ushr 32).toInt()
        offset = 0
        nWords = 2
        var i = kDigits
        val limit = nDigits - 5 // slurp digits 5 at a time.
        var v: Int
        while (i < limit) {
            val ilim = i + 5
            v = digits[i++].asInt() - '0'.code
            while (i < ilim) {
                v = 10 * v + digits[i++].asInt() - '0'.code
            }
            multAddMe(100000, v) // ... where 100000 is 10^5.
        }
        var factor = 1
        v = 0
        while (i < nDigits) {
            v = 10 * v + digits[i++].asInt() - '0'.code
            factor *= 10
        }
        if (factor != 1) multAddMe(factor, v)

        trimLeadingZeros()
    }

    /**
     * Removes all leading zeros from this `FDBigInteger` adjusting
     * the offset and number of non-zero leading words accordingly.
     */
    private fun trimLeadingZeros() {
        if (assertionsEnabled) {
            require(offset >= 0)
            require(nWords in 0..data.size)
            if (nWords == 0) require(offset == 0)
        }

        var i = nWords
        if (i > 0 && (data[--i] == 0)) {
            //for (; i > 0 && data[i - 1] == 0; i--) ;
            while (i > 0 && data[i - 1] == 0) {
                i--
            }
            this.nWords = i
            if (i == 0) { // all words are zero
                this.offset = 0
            }
        }
    }

    fun getNormalizationBias(): Int {
        if (nWords == 0) {
            throw IllegalArgumentException("Zero value cannot be normalized")
        }
        val zeros: Int = data[nWords - 1].countLeadingZeroBits()
        return if ((zeros < 4)) 28 + zeros else zeros - 4
    }

    /**
     * Shifts this `FDBigInteger` to the left. The shift is performed
     * in-place unless the `FDBigInteger` is immutable in which case
     * a new instance of `FDBigInteger` is returned.
     *
     * @param shift The number of bits to shift left.
     * @return The shifted `FDBigInteger`.
     */
    fun leftShift(shift: Int): FDBigInteger {
        if (shift == 0 || nWords == 0) return this

        val wordCount = shift shr 5
        val bitcount = shift and 0x1f

        if (this.isImmutable) {
            return if (bitcount == 0) {
                FDBigInteger(data.copyOf(nWords), offset + wordCount)
            } else {
                val anticount = 32 - bitcount
                val idx = nWords - 1
                val prev = data[idx]
                val hi = prev ushr anticount
                val result: IntArray
                if (hi != 0) {
                    result = IntArray(nWords + 1)
                    result[nWords] = hi
                } else {
                    result = IntArray(nWords)
                }
                leftShift(data, idx, result, bitcount, anticount, prev)
                FDBigInteger(result, offset + wordCount)
            }
        }

        if (bitcount == 0) {
            offset += wordCount
            return this
        }

        val anticount = 32 - bitcount
        if ((data[0] shl bitcount) == 0) {
            var idx = 0
            var prev = data[idx]
            while (idx < nWords - 1) {
                var v = (prev ushr anticount)
                prev = data[idx + 1]
                v = v or (prev shl bitcount)
                data[idx] = v
                idx++
            }
            val v = prev ushr anticount
            data[idx] = v
            if (v == 0) {
                nWords--
            }
            offset++
        } else {
            val idx = nWords - 1
            val prev = data[idx]
            val hi = prev ushr anticount
            var result = data
            val src = data
            if (hi != 0) {
                if (nWords == data.size) {
                    result = IntArray(nWords + 1)
                    data = result
                }
                result[nWords++] = hi
            }
            leftShift(src, idx, result, bitcount, anticount, prev)
        }

        offset += wordCount
        return this
    }

    /**
     * Returns the number of `int`s this `FDBigInteger` represents.
     *
     * @return Number of `int`s required to represent this `FDBigInteger`.
     */
    private /*@ pure @*/ fun size(): Int = nWords + offset

    /**
     * Computes
     *
     *    q = (int)(this / S)
     *    this = 10 * (this mod S)
     *    Return q
     *
     * This is the iteration step of digit development for output.
     * We assume that S has been normalized, as above, and that
     * "this" has been left-shifted accordingly.
     * Also assumed, of course, is that the result, q, can be expressed
     * as an integer, `0 <= q < 10`.
     *
     * @param S The divisor of this `FDBigInteger`.
     * @return `q = (int)(this / S)`.
     */
    @Throws(IllegalArgumentException::class)
    fun quoRemIteration(S: FDBigInteger): Int {
        if (assertionsEnabled) {
            require(!isImmutable)
            require(size() <= S.size())
            require(this.data.size + this.offset >= S.size())
        }

        // ensure that this and S have the same number of
        // digits. If S is properly normalized and q < 10 then
        // this must be so.
        val thSize = this.size()
        val sSize = S.size()
        if (thSize < sSize) {
            // this value is significantly less than S, result of division is zero.
            // just mult this by 10.
            val p = multAndCarryBy10(this.data, this.nWords, this.data)
            if (p != 0) {
                data[nWords++] = p
            } else {
                trimLeadingZeros()
            }
            return 0
        } else if (thSize > sSize) {
            throw IllegalArgumentException("disparate values")
        }
        // estimate q the obvious way. We will usually be
        // right. If not, then we're only off by a little and
        // will re-add.
        var q = (data[nWords - 1].toLong() and LONG_MASK) / (S.data[S.nWords - 1].toLong() and LONG_MASK)
        val diff = multDiffMe(q, S)
        if (diff != 0L) {
            //@ assert q != 0;
            //@ assert this.offset == \old(Math.min(this.offset, S.offset));
            //@ assert this.offset <= S.offset;

            // q is too big.
            // add S back in until this turns +. This should
            // not be very many times!

            var sum = 0L
            val tStart = S.offset - this.offset
            //@ assert tStart >= 0;
            val sd = S.data
            val td = this.data
            while (sum == 0L) {
                var sIndex = 0
                var tIndex = tStart
                while (tIndex < this.nWords) {
                    sum += (td[tIndex].toLong() and LONG_MASK) + (sd[sIndex].toLong() and LONG_MASK)
                    td[tIndex] = sum.toInt()
                    sum = sum ushr 32 // Signed or unsigned, answer is 0 or 1
                    sIndex++
                    tIndex++
                }
                q -= 1
            }
        }
        // finally, we can multiply this by 10.
        // it cannot overflow, right, as the high-order word has
        // at least 4 high-order zeros!
        multAndCarryBy10(this.data, this.nWords, this.data)
        trimLeadingZeros()
        return q.toInt()
    }

    /**
     * Multiplies this `FDBigInteger` by 10. The operation will be
     * performed in place unless the `FDBigInteger` is immutable in
     * which case a new `FDBigInteger` will be returned.
     *
     * @return The `FDBigInteger` multiplied by 10.
     */
    fun multBy10(): FDBigInteger = when {
        nWords == 0 -> this
        isImmutable -> {
            val res = IntArray(nWords + 1)
            res[nWords] = multAndCarryBy10(data, nWords, res)
            FDBigInteger(res, offset)
        }
        else -> {
            val p = multAndCarryBy10(this.data, this.nWords, this.data)
            if (p == 0) trimLeadingZeros() else {
                if (nWords == data.size) {
                    if (data[0] == 0) {
                        data.copyInto(data, destinationOffset = 0, startIndex = 1, endIndex = 1 + --nWords)
                        offset++
                    } else {
                        data = data.copyOf(data.size + 1)
                    }
                }
                data[nWords++] = p
            }
            this
        }
    }

    /**
     * Multiplies this `FDBigInteger` by
     * `5<sup>p5</sup> * 2<sup>p2</sup>`. The operation will be
     * performed in place if possible, otherwise a new `FDBigInteger`
     * will be returned.
     *
     * @param p5 The exponent of the power-of-five factor.
     * @param p2 The exponent of the power-of-two factor.
     * @return The multiplication result.
     */
    fun multByPow52(p5: Int, p2: Int): FDBigInteger {
        if (this.nWords == 0) return this
        var res = this
        if (p5 != 0) {
            val r: IntArray
            val extraSize = if ((p2 != 0)) 1 else 0
            if (p5 < SMALL_5_POW.size) {
                r = IntArray(this.nWords + 1 + extraSize)
                mult(this.data, this.nWords, SMALL_5_POW[p5], r)
                res = FDBigInteger(r, this.offset)
            } else {
                val pow5 = big5pow(p5)
                r = IntArray(this.nWords + pow5.size() + extraSize)
                mult(this.data, this.nWords, pow5.data, pow5.nWords, r)
                res = FDBigInteger(r, this.offset + pow5.offset)
            }
        }
        return res.leftShift(p2)
    }

    /**
     * Subtracts the supplied `FDBigInteger` subtrahend from this
     * `FDBigInteger`. Assert that the result is positive.
     * If the subtrahend is immutable, store the result in this(minuend).
     * If this(minuend) is immutable a new `FDBigInteger` is created.
     *
     * @param subtrahend The `FDBigInteger` to be subtracted.
     * @return This `FDBigInteger` less the subtrahend.
     */
    fun leftInplaceSub(subtrahend: FDBigInteger): FDBigInteger {
        val minuend = when {
            this.isImmutable -> FDBigInteger(data.copyOf(), this.offset)
            else -> this
        }
        var offsetDiff = subtrahend.offset - minuend.offset
        val sData = subtrahend.data
        var mData = minuend.data
        val subLen = subtrahend.nWords
        var minLen = minuend.nWords
        if (offsetDiff < 0) {
            // need to expand minuend
            val rLen = minLen - offsetDiff
            if (rLen < mData.size) {
                mData.copyInto(mData, destinationOffset = -offsetDiff, startIndex = 0, endIndex = 0 + minLen)
                mData.fill(0, 0, -offsetDiff)
            } else {
                val r = IntArray(rLen)
                mData.copyInto(r, destinationOffset = -offsetDiff, startIndex = 0, endIndex = 0 + minLen)
                mData = r
                minuend.data = mData
            }
            minuend.offset = subtrahend.offset
            minLen = rLen
            minuend.nWords = minLen
            offsetDiff = 0
        }
        var borrow = 0L
        var mIndex = offsetDiff
        var sIndex = 0
        while (sIndex < subLen && mIndex < minLen) {
            val diff = (mData[mIndex].toLong() and LONG_MASK) - (sData[sIndex].toLong() and LONG_MASK) + borrow
            mData[mIndex] = diff.toInt()
            borrow = diff shr 32 // signed shift
            sIndex++
            mIndex++
        }
        while (borrow != 0L && mIndex < minLen) {
            val diff = (mData[mIndex].toLong() and LONG_MASK) + borrow
            mData[mIndex] = diff.toInt()
            borrow = diff shr 32 // signed shift
            mIndex++
        }
        // result should be positive
        minuend.trimLeadingZeros()
        return minuend
    }

    /**
     * Subtracts the supplied `FDBigInteger` subtrahend from this
     * `FDBigInteger`. Assert that the result is positive.
     * If this(minuend) is immutable, store the result in subtrahend.
     * If subtrahend is immutable a new `FDBigInteger` is created.
     *
     * @param subtrahend The `FDBigInteger` to be subtracted.
     * @return This `FDBigInteger` less the subtrahend.
     */
    fun rightInplaceSub(subtrahend: FDBigInteger): FDBigInteger {
        var ste = subtrahend
        val minuend = this
        if (ste.isImmutable) {
            ste = FDBigInteger(ste.data.copyOf(), ste.offset)
        }
        var offsetDiff = minuend.offset - ste.offset
        var sData = ste.data
        val mData = minuend.data
        val subLen = ste.nWords
        val minLen = minuend.nWords
        if (offsetDiff < 0) {
            if (minLen < sData.size) {
                sData.copyInto(sData, destinationOffset = -offsetDiff, startIndex = 0, endIndex = 0 + subLen)
                sData.fill(0, 0, -offsetDiff)
            } else {
                val r = IntArray(minLen)
                sData.copyInto(r, destinationOffset = -offsetDiff, startIndex = 0, endIndex = 0 + subLen)
                sData = r
                ste.data = sData
            }
            ste.offset = minuend.offset
            offsetDiff = 0
        } else {
            val rLen = minLen + offsetDiff
            if (rLen >= sData.size) {
                sData = sData.copyOf(rLen)
                ste.data = sData
            }
        }

        if (assertionsEnabled) {
            require(minLen == minuend.nWords)
            require(subtrahend.offset + subtrahend.data.size >= minuend.size())
            require(offsetDiff == minuend.offset - subtrahend.offset)
            require(0 <= offsetDiff && offsetDiff + minLen <= sData.size)
        }

        var sIndex = 0
        var borrow = 0L
        while (sIndex < offsetDiff) {
            val diff = 0L - (sData[sIndex].toLong() and LONG_MASK) + borrow
            sData[sIndex] = diff.toInt()
            borrow = diff shr 32 // signed shift
            sIndex++
        }
        //@ assert sIndex == offsetDiff;
        var mIndex = 0
        while (mIndex < minLen) {
            //@ assert sIndex == offsetDiff + mIndex;
            val diff = (mData[mIndex].toLong() and LONG_MASK) - (sData[sIndex].toLong() and LONG_MASK) + borrow
            sData[sIndex] = diff.toInt()
            borrow = diff shr 32 // signed shift
            sIndex++
            mIndex++
        }
        // result should be positive
        ste.nWords = sIndex
        ste.trimLeadingZeros()
        return ste
    }

    /**
     * Compares the parameter with this `FDBigInteger`. Returns an
     * integer accordingly as:
     * * > 0: this > other
     * * 0: this == other
     * * < 0: this < other
     * @param other The `FDBigInteger` to compare.
     * @return A negative value, zero, or a positive value according to the result of the comparison.
     */
    fun cmp(other: FDBigInteger): Int {
        val aSize = nWords + offset
        val bSize = other.nWords + other.offset
        return when {
            aSize > bSize -> 1
            aSize < bSize -> -1
            else -> {
                var aLen = nWords
                var bLen = other.nWords
                while (aLen > 0 && bLen > 0) {
                    val a = data[--aLen]
                    val b = other.data[--bLen]
                    if (a != b) {
                        return if (((a.toLong() and LONG_MASK) < (b.toLong() and LONG_MASK))) -1 else 1
                    }
                }
                when {
                    aLen > 0 -> checkZeroTail(data, aLen)
                    bLen > 0 -> -checkZeroTail(other.data, bLen)
                    else -> 0
                }
            }
        }
    }

    /**
     * Compares this `FDBigInteger` with
     * `5<sup>p5</sup> * 2<sup>p2</sup>`.
     * Returns an integer accordingly as:
     * * > 0: this > other
     * * 0: this == other
     * * < 0: this < other
     * @param p5 The exponent of the power-of-five factor.
     * @param p2 The exponent of the power-of-two factor.
     * @return A negative value, zero, or a positive value according to the result of the comparison.
     */
    fun cmpPow52(p5: Int, p2: Int): Int {
        if (assertionsEnabled) require(p5 >= 0 && p2 >= 0)

        if (p5 != 0) return this.cmp(big5pow(p5).leftShift(p2))

        val wordCount = p2 shr 5
        val bitcount = p2 and 0x1f
        val size = this.nWords + this.offset
        return when {
            size > wordCount + 1 -> 1
            size < wordCount + 1 -> -1
            else -> {
                val a = data[nWords - 1]
                val b = 1 shl bitcount
                when {
                    a != b -> if (((a.toLong() and LONG_MASK) < (b.toLong() and LONG_MASK))) -1 else 1
                    else -> checkZeroTail(this.data, this.nWords - 1)
                }
            }
        }
    }

    /**
     * Compares this `FDBigInteger` with `x + y`. Returns a
     * value according to the comparison as:
     * * -1: this <  x + y
     * * 0: this == x + y
     * * 1: this >  x + y
     * @param x The first addend of the sum to compare.
     * @param y The second addend of the sum to compare.
     * @return -1, 0, or 1 according to the result of the comparison.
     */
    fun addAndCmp(x: FDBigInteger, y: FDBigInteger): Int {
        val big: FDBigInteger
        val small: FDBigInteger
        val xSize = x.size()
        val ySize = y.size()
        val bSize: Int
        val sSize: Int
        if (xSize >= ySize) {
            big = x
            small = y
            bSize = xSize
            sSize = ySize
        } else {
            big = y
            small = x
            bSize = ySize
            sSize = xSize
        }
        val thSize = this.size()// (top>>>32)!=0 guaranteed carry extension
        // here sum.nWords == this.nWords
        // good case - no carry extension
        // here sum.nWords == this.nWords
        when {
            bSize == 0 -> return if (thSize == 0) 0 else 1
            sSize == 0 -> return this.cmp(big)
            bSize > thSize -> return -1
            bSize + 1 < thSize -> return 1
            else -> {
                var top = (big.data[big.nWords - 1].toLong() and LONG_MASK)
                if (sSize == bSize) {
                    top += (small.data[small.nWords - 1].toLong() and LONG_MASK)
                }
                if ((top ushr 32) == 0L) {
                    if (((top + 1) ushr 32) == 0L) {
                        // good case - no carry extension
                        if (bSize < thSize) return 1

                        // here sum.nWords == this.nWords
                        val v = (data[nWords - 1].toLong() and LONG_MASK)
                        if (v < top) return -1
                        if (v > top + 1) return 1
                    }
                } else { // (top>>>32)!=0 guaranteed carry extension
                    if (bSize + 1 > thSize) return -1

                    // here sum.nWords == this.nWords
                    top = top ushr 32
                    val v = (data[nWords - 1].toLong() and LONG_MASK)
                    if (v < top) return -1
                    if (v > top + 1) return 1
                }
                return this.cmp(big.add(small))
            }
        }
    }

    /**
     * Multiplies this `FDBigInteger` by an integer.
     * @param i The factor by which to multiply this `FDBigInteger`.
     * @return This `FDBigInteger` multiplied by an integer.
     */
    private fun mult(i: Int): FDBigInteger {
        if (this.nWords == 0) return this
        val r = IntArray(nWords + 1)
        mult(data, nWords, i, r)
        return FDBigInteger(r, offset)
    }

    /**
     * Multiplies this `FDBigInteger` by another `FDBigInteger`.
     * @param other The `FDBigInteger` factor by which to multiply.
     * @return The product of this and the parameter `FDBigInteger`s.
     */
    private fun mult(other: FDBigInteger): FDBigInteger = when {
        this.nWords == 0 -> this
        this.size() == 1 -> other.mult(data[0])
        other.nWords == 0 -> other
        other.size() == 1 -> this.mult(other.data[0])
        else -> {
            val r = IntArray(nWords + other.nWords)
            mult(this.data, this.nWords, other.data, other.nWords, r)
            FDBigInteger(r, this.offset + other.offset)
        }
    }

    /**
     * Adds another `FDBigInteger` to this `FDBigInteger`.
     * @param other The `FDBigInteger` to add.
     * @return The sum of the `FDBigInteger`s.
     */
    private fun add(other: FDBigInteger): FDBigInteger {
        val big: FDBigInteger
        val small: FDBigInteger
        val bigLen: Int
        val smallLen: Int
        val tSize = this.size()
        val oSize = other.size()
        if (tSize >= oSize) {
            big = this
            bigLen = tSize
            small = other
            smallLen = oSize
        } else {
            big = other
            bigLen = oSize
            small = this
            smallLen = tSize
        }
        val r = IntArray(bigLen + 1)
        var i = 0
        var carry = 0L
        while (i < smallLen) {
            carry += ((if (i < big.offset) 0L else (big.data[i - big.offset].toLong() and LONG_MASK))
                    + ((if (i < small.offset) 0L else (small.data[i - small.offset].toLong() and LONG_MASK))))
            r[i] = carry.toInt()
            carry = carry shr 32 // signed shift.
            i++
        }
        while (i < bigLen) {
            carry += (if (i < big.offset) 0L else (big.data[i - big.offset].toLong() and LONG_MASK))
            r[i] = carry.toInt()
            carry = carry shr 32 // signed shift.
            i++
        }
        r[bigLen] = carry.toInt()
        return FDBigInteger(r, 0)
    }


    /**
     * Multiplies a `FDBigInteger` by an int and adds another int. The
     * result is computed in place. This method is intended only to be invoked from
     * 
     *      FDBigInteger(long lValue, char[] digits, int kDigits, int nDigits)
     *
     * @param iv The factor by which to multiply this `FDBigInteger`.
     * @param addend The value to add to the product of this
     * `FDBigInteger` and `iv`.
     */
    private fun multAddMe(iv: Int, addend: Int) {
        val v = iv.toLong() and LONG_MASK
        // unroll 0th iteration, doing addition.
        var p = v * (data[0].toLong() and LONG_MASK) + (addend.toLong() and LONG_MASK)
        data[0] = p.toInt()
        p = p ushr 32
        for (i in 1 until nWords) {
            p += v * (data[i].toLong() and LONG_MASK)
            data[i] = p.toInt()
            p = p ushr 32
        }
        if (p != 0L) {
            data[nWords++] = p.toInt() // will fail noisily if illegal!
        }
    }

    /**
     * Multiplies the parameters and subtracts them from this
     * `FDBigInteger`.
     *
     * @param q The integer parameter.
     * @param S The `FDBigInteger` parameter.
     * @return `this - q*S`.
     */
    private fun multDiffMe(q: Long, S: FDBigInteger): Long {
        if (assertionsEnabled) {
            require(q in 0..(1L shl 31))
            require(offset >= 0)
            require(nWords in 0..data.size)
            require(!isImmutable)
            require(this.size() == S.size())
        }
        
        if (q == 0L) return 0L

        var diff = 0L
        var deltaSize = S.offset - this.offset
        if (deltaSize >= 0) {
            val sd = S.data
            val td = this.data
            var sIndex = 0
            var tIndex = deltaSize
            while (sIndex < S.nWords) {
                diff += (td[tIndex].toLong() and LONG_MASK) - q * (sd[sIndex].toLong() and LONG_MASK)
                td[tIndex] = diff.toInt()
                diff = diff shr 32 // N.B. SIGNED shift.
                sIndex++
                tIndex++
            }
        } else {
            deltaSize = -deltaSize
            val rd = IntArray(nWords + deltaSize)
            var sIndex = 0
            var rIndex = 0
            val sd = S.data
            while (rIndex < deltaSize && sIndex < S.nWords) {
                diff -= q * (sd[sIndex].toLong() and LONG_MASK)
                rd[rIndex] = diff.toInt()
                diff = diff shr 32 // N.B. SIGNED shift.
                sIndex++
                rIndex++
            }
            var tIndex = 0
            val td = this.data
            while (sIndex < S.nWords) {
                diff += (td[tIndex].toLong() and LONG_MASK) - q * (sd[sIndex].toLong() and LONG_MASK)
                rd[rIndex] = diff.toInt()
                diff = diff shr 32 // N.B. SIGNED shift.
                sIndex++
                tIndex++
                rIndex++
            }
            this.nWords += deltaSize
            this.offset -= deltaSize
            this.data = rd
        }
        
        return diff
    }

    companion object {
        val SMALL_5_POW: IntArray = run {
            var n = 1
            IntArray(14) {
                n.also { n *= 5 }
            }
        }

        val LONG_5_POW: LongArray = run {
            var n = 1L
            LongArray(27) {
                n.also { n *= 5 }
            }
        }

        // Maximum size of cache of powers of 5 as FDBigIntegers.
        private const val MAX_FIVE_POW = 340

        // Cache of big powers of 5 as FDBigIntegers.
        private val POW_5_CACHE: Array<FDBigInteger> = run {
            val pow5cache = arrayOfNulls<FDBigInteger>(MAX_FIVE_POW)
            var i = 0
            while (i < SMALL_5_POW.size) {
                val pow5 = FDBigInteger(intArrayOf(SMALL_5_POW[i]), 0)
                pow5.isImmutable = true
                pow5cache[i] = pow5
                i++
            }
            var prev = pow5cache[i - 1]
            while (i < MAX_FIVE_POW) {
                prev = prev!!.mult(5)
                pow5cache[i] = prev
                prev.isImmutable = true
                i++
            }
            @Suppress("UNCHECKED_CAST")
            pow5cache as Array<FDBigInteger>
        }

        val ZERO: FDBigInteger = FDBigInteger(IntArray(0), 0).apply {
            isImmutable = true
        }

        // Constant for casting an int to a long via bitwise AND.
        private const val LONG_MASK = 0xffffffffL

        /**
         * Returns an `FDBigInteger` with the numerical value
         * `5<sup>p5</sup> * 2<sup>p2</sup>`.
         *
         * @param p5 The exponent of the power-of-five factor.
         * @param p2 The exponent of the power-of-two factor.
         * @return `5<sup>p5</sup> * 2<sup>p2</sup>`
         */
        fun valueOfPow52(p5: Int, p2: Int): FDBigInteger {
            if (p5 != 0) {
                if (p2 == 0) {
                    return big5pow(p5)
                } else if (p5 < SMALL_5_POW.size) {
                    val pow5 = SMALL_5_POW[p5]
                    val wordCount = p2 shr 5
                    val bitcount = p2 and 0x1f
                    return if (bitcount == 0) {
                        FDBigInteger(intArrayOf(pow5), wordCount)
                    } else {
                        FDBigInteger(
                            intArrayOf(
                                pow5 shl bitcount,
                                pow5 ushr (32 - bitcount)
                            ), wordCount
                        )
                    }
                } else {
                    return big5pow(p5).leftShift(p2)
                }
            } else {
                return valueOfPow2(p2)
            }
        }

        /**
         * Returns an `FDBigInteger` with the numerical value
         * `value * 5<sup>p5</sup> * 2<sup>p2</sup>`.
         *
         * @param value The constant factor.
         * @param p5 The exponent of the power-of-five factor.
         * @param p2 The exponent of the power-of-two factor.
         * @return `value * 5<sup>p5</sup> * 2<sup>p2</sup>`
         */
        fun valueOfMulPow52(value: Long, p5: Int, p2: Int): FDBigInteger {
            var v0 = value.toInt()
            var v1 = (value ushr 32).toInt()
            val wordCount = p2 shr 5
            val bitcount = p2 and 0x1f
            if (p5 != 0) {
                if (p5 < SMALL_5_POW.size) {
                    val pow5 = SMALL_5_POW[p5].toLong() and LONG_MASK
                    var carry = (v0.toLong() and LONG_MASK) * pow5
                    v0 = carry.toInt()
                    carry = carry ushr 32
                    carry += (v1.toLong() and LONG_MASK) * pow5
                    v1 = carry.toInt()
                    val v2 = (carry ushr 32).toInt()
                    return if (bitcount == 0) {
                        FDBigInteger(intArrayOf(v0, v1, v2), wordCount)
                    } else {
                        FDBigInteger(
                            intArrayOf(
                                v0 shl bitcount,
                                (v1 shl bitcount) or (v0 ushr (32 - bitcount)),
                                (v2 shl bitcount) or (v1 ushr (32 - bitcount)),
                                v2 ushr (32 - bitcount)
                            ), wordCount
                        )
                    }
                } else {
                    val pow5 = big5pow(p5)
                    val r: IntArray
                    if (v1 == 0) {
                        r = IntArray(pow5.nWords + 1 + (if ((p2 != 0)) 1 else 0))
                        mult(pow5.data, pow5.nWords, v0, r)
                    } else {
                        r = IntArray(pow5.nWords + 2 + (if ((p2 != 0)) 1 else 0))
                        mult(pow5.data, pow5.nWords, v0, v1, r)
                    }
                    return FDBigInteger(r, pow5.offset).leftShift(p2)
                }
            } else if (p2 != 0) {
                return if (bitcount == 0) {
                    FDBigInteger(intArrayOf(v0, v1), wordCount)
                } else {
                    FDBigInteger(
                        intArrayOf(
                            v0 shl bitcount,
                            (v1 shl bitcount) or (v0 ushr (32 - bitcount)),
                            v1 ushr (32 - bitcount)
                        ), wordCount
                    )
                }
            }
            return FDBigInteger(intArrayOf(v0, v1), 0)
        }

        /**
         * Returns an `FDBigInteger` with the numerical value
         * `2<sup>p2</sup>`.
         *
         * @param p2 The exponent of 2.
         * @return `2<sup>p2</sup>`
         */
        private fun valueOfPow2(p2: Int): FDBigInteger {
            val wordCount = p2 shr 5
            val bitcount = p2 and 0x1f
            return FDBigInteger(intArrayOf(1 shl bitcount), wordCount)
        }

        /**
         * Left shifts the contents of one int array into another.
         *
         * @param src The source array.
         * @param idx The initial index of the source array.
         * @param result The destination array.
         * @param bitcount The left shift.
         * @param anticount The left anti-shift, e.g., `32-bitcount`.
         * @param previous The prior source value.
         */
        private fun leftShift(src: IntArray, idx: Int, result: IntArray, bitcount: Int, anticount: Int, previous: Int) {
            var i = idx
            var prev = previous
            while (i > 0) {
                var v = (prev shl bitcount)
                prev = src[i - 1]
                v = v or (prev ushr anticount)
                result[i] = v
                i--
            }
            val v = prev shl bitcount
            result[0] = v
        }

        /**
         * Multiplies two big integers represented as int arrays.
         *
         * @param s1 The first array factor.
         * @param s1Len The number of elements of `s1` to use.
         * @param s2 The second array factor.
         * @param s2Len The number of elements of `s2` to use.
         * @param dst The product array.
         */
        private fun mult(s1: IntArray, s1Len: Int, s2: IntArray, s2Len: Int, dst: IntArray) {
            for (i in 0 until s1Len) {
                val v = s1[i].toLong() and LONG_MASK
                var p = 0L
                for (j in 0 until s2Len) {
                    p += (dst[i + j].toLong() and LONG_MASK) + v * (s2[j].toLong() and LONG_MASK)
                    dst[i + j] = p.toInt()
                    p = p ushr 32
                }
                dst[i + s2Len] = p.toInt()
            }
        }

        /**
         * Determines whether all elements of an array are zero for all indices less
         * than a given index.
         *
         * @param a The array to be examined.
         * @param from The index strictly below which elements are to be examined.
         * @return Zero if all elements in range are zero, 1 otherwise.
         */
        private fun checkZeroTail(a: IntArray, from: Int): Int {
            var i = from
            while (i > 0) {
                if (a[--i] != 0) return 1
            }
            return 0
        }

        /**
         * Multiplies by 10 a big integer represented as an array. The final carry
         * is returned.
         *
         * @param src The array representation of the big integer.
         * @param srcLen The number of elements of `src` to use.
         * @param dst The product array.
         * @return The final carry of the multiplication.
         */
        private fun multAndCarryBy10(src: IntArray, srcLen: Int, dst: IntArray): Int {
            if (assertionsEnabled) require(src.size >= srcLen && dst.size >= srcLen)
            
            var carry: Long = 0
            for (i in 0 until srcLen) {
                val product = (src[i].toLong() and LONG_MASK) * 10L + carry
                dst[i] = product.toInt()
                carry = product ushr 32
            }
            return carry.toInt()
        }

        /**
         * Multiplies by a constant value a big integer represented as an array.
         * The constant factor is an `int`.
         *
         * @param src The array representation of the big integer.
         * @param srcLen The number of elements of `src` to use.
         * @param value The constant factor by which to multiply.
         * @param dst The product array.
         */
        /*@
          @ requires src.length >= srcLen && dst.length >= srcLen + 1;
          @ assignable dst[0 .. srcLen];
          @ ensures AP(dst, srcLen + 1) == \old(AP(src, srcLen) * UNSIGNED(value));
          @*/
        private fun mult(src: IntArray, srcLen: Int, value: Int, dst: IntArray) {
            val `val` = value.toLong() and LONG_MASK
            var carry: Long = 0
            for (i in 0 until srcLen) {
                val product = (src[i].toLong() and LONG_MASK) * `val` + carry
                dst[i] = product.toInt()
                carry = product ushr 32
            }
            dst[srcLen] = carry.toInt()
        }

        /**
         * Multiplies by a constant value a big integer represented as an array.
         * The constant factor is a long represent as two `int`s.
         *
         * @param src The array representation of the big integer.
         * @param srcLen The number of elements of `src` to use.
         * @param v0 The lower 32 bits of the long factor.
         * @param v1 The upper 32 bits of the long factor.
         * @param dst The product array.
         */
        /*@
          @ requires src != dst;
          @ requires src.length >= srcLen && dst.length >= srcLen + 2;
          @ assignable dst[0 .. srcLen + 1];
          @ ensures AP(dst, srcLen + 2) == \old(AP(src, srcLen) * (UNSIGNED(v0) + (UNSIGNED(v1) << 32)));
          @*/
        private fun mult(src: IntArray, srcLen: Int, v0: Int, v1: Int, dst: IntArray) {
            var v = v0.toLong() and LONG_MASK
            var carry: Long = 0
            for (j in 0 until srcLen) {
                val product = v * (src[j].toLong() and LONG_MASK) + carry
                dst[j] = product.toInt()
                carry = product ushr 32
            }
            dst[srcLen] = carry.toInt()
            v = v1.toLong() and LONG_MASK
            carry = 0
            for (j in 0 until srcLen) {
                val product = (dst[j + 1].toLong() and LONG_MASK) + (v * (src[j].toLong() and LONG_MASK)) + carry
                dst[j + 1] = product.toInt()
                carry = product ushr 32
            }
            dst[srcLen + 1] = carry.toInt()
        }

        // Fails assertion for negative exponent.
        /**
         * Computes `5` raised to a given power.
         * @param p The exponent of 5.
         * @return `5<sup>p</sup>`.
         */
        private fun big5pow(p: Int): FDBigInteger = if (p < MAX_FIVE_POW) POW_5_CACHE[p] else big5powRec(p)

        // slow path
        /**
         * Computes `5` raised to a given power.
         * @param p The exponent of 5.
         * @return `5<sup>p</sup>`.
         */
        private fun big5powRec(p: Int): FDBigInteger {
            if (p < MAX_FIVE_POW) return POW_5_CACHE[p]

            val r: Int
            // in order to compute 5^p,
            // compute its square root, 5^(p/2) and square.
            // or, let q = p / 2, r = p -q, then
            // 5^p = 5^(q+r) = 5^q * 5^r
            // construct the value.
            // recursively.
            val q = p shr 1
            r = p - q
            val bigq = big5powRec(q)
            return if (r < SMALL_5_POW.size) {
                bigq.mult(SMALL_5_POW[r])
            } else {
                bigq.mult(big5powRec(r))
            }
        }
    }
}

internal fun CharArray.copyAsciiInto(
    srcPos: Int,
    dest: MutableBuffer,
    destPos: Int,
    length: Int
) {
    for (i in 0 until length) {
        dest[destPos + i] = this[srcPos + i].code
    }
}

