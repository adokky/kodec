package io.kodec

import io.kodec.MathUtils.pow10
import karamel.utils.assert
import kotlin.math.max

// https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/jdk/internal/math/FDBigInteger.java
/**
 * A simple big integer package specifically for floating point base conversion.
 */
internal class FDBigInteger {
    private var _data: IntArray? = null
    var data: IntArray // value: data[0] is least significant
        get() = _data!!
        set(value) { _data = value }
    var offset: Int = 0 // number of least significant zero padding ints
    var nWords: Int = 0 // data[nWords-1]!=0, all values above are zero

    // if nWords==0 -> this FDBigInteger is zero
    private var isImmutable = false

    constructor(): this(IntArray(DATA_MAX_INIIAL_SIZE), 0)

    /**
     * Constructs an [FDBigInteger] from data and padding. The
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
        this.nWords = data.size
        this.data = data
        this.offset = offset
        trimLeadingZeros()
    }

    private fun setDataSize(nWords: Int, offset: Int) {
        assert { data.size >= nWords }
        if (nWords < this.nWords) data.fill(0, fromIndex = nWords, toIndex = this.nWords)
        this.nWords = nWords
        this.offset = offset
        trimLeadingZeros()
    }

    private fun clearData() {
        data.fill(0, toIndex = nWords)
    }

    private fun initData2(v0: Int, v1: Int, offset: Int): FDBigInteger {
        data[0] = v0
        data[1] = v1
        setDataSize(nWords = 2, offset = offset)
        return this
    }

    private fun initData3(v0: Int, v1: Int, v2: Int, offset: Int): FDBigInteger {
        data[0] = v0
        data[1] = v1
        data[2] = v2
        setDataSize(nWords = 3, offset = offset)
        return this
    }

    private fun initData4(v0: Int, v1: Int, v2: Int, v3: Int, offset: Int): FDBigInteger {
        data[0] = v0
        data[1] = v1
        data[2] = v2
        data[3] = v3
        setDataSize(nWords = 4, offset = offset)
        return this
    }

    /**
     * Constructs an [FDBigInteger] from a starting value and some decimal digits.
     *
     * @param lValue The starting value.
     * @param digits The decimal digits.
     * @param i The initial index into `digits`.
     * @param nDigits The final index into `digits`.
     */
    constructor(lValue: Long, digits: ByteArray, i: Int, nDigits: Int) {
        init(lValue, digits, i, nDigits)
    }

    fun init(lValue: Long, digits: ByteArray, i: Int, nDigits: Int): FDBigInteger {
        val n = (nDigits + 8) / 9 // estimate size needed: ⌈nDigits / 9⌉
        val nWords1 = max(n, 2)
        when {
            _data == null || nWords1 > data.size -> _data = IntArray(size = nWords1)
            nWords > 2 -> data.fill(0, fromIndex = 2, toIndex = nWords)
        }
        nWords = nWords1
        offset = 0
        data[0] = lValue.toInt() // starting value
        data[1] = (lValue ushr 32).toInt()

        var i = i
        val limit = nDigits - 9
        while (i < limit) {
            var v = 0
            val ilim = i + 9
            while (i < ilim) {
                v = 10 * v + digits[i++] - '0'.code
            }
            multAdd(1000000000, v) // 10^9
        }
        if (i < nDigits) {
            val factor = pow10(nDigits - i).toInt()
            var v = 0
            while (i < nDigits) {
                v = 10 * v + digits[i++] - '0'.code
            }
            multAdd(factor, v)
        }

        trimLeadingZeros()
        return this
    }

    constructor(v: Long) : this(intArrayOf(v.toInt(), (v ushr 32).toInt()), 0)

    /**
     * Removes all leading zeros from this [FDBigInteger] adjusting
     * the offset and number of non-zero leading words accordingly.
     */
    private fun trimLeadingZeros() {
        var i = nWords - 1
        while (i >= 0 && data[i] == 0) {
            // empty body
            --i
        }
        nWords = i + 1
        if (i < 0) {
            offset = 0
        }
    }

    /**
     * Shifts this [FDBigInteger] to the left. The shift is performed
     * in-place unless the [FDBigInteger] is immutable in which case
     * a new instance of [FDBigInteger] is returned.
     *
     * @param shift The number of bits to shift left.
     * @return The shifted [FDBigInteger].
     */
    fun leftShift(shift: Int): FDBigInteger {
        if (shift == 0 || nWords == 0) return this
        val wordcount = shift shr 5
        val bitcount = shift and 0x1f
        return when {
            isImmutable -> leftShiftImmutable(bitcount, wordcount)
            else -> leftShiftInPlace(bitcount, wordcount)
        }
    }

    private fun leftShiftInPlace(bitcount: Int, wordcount: Int): FDBigInteger {
        if (bitcount != 0) {
            if (data[0] shl bitcount == 0) {
                var idx = 0
                var prev = data[idx]
                while (idx < nWords - 1) {
                    var v = prev ushr -bitcount
                    prev = data[idx + 1]
                    v = v or (prev shl bitcount)
                    data[idx] = v
                    idx++
                }
                val v = prev ushr -bitcount
                data[idx] = v
                if (v == 0) nWords--
                offset++
            } else {
                val idx = nWords - 1
                val prev = data[idx]
                val hi = prev ushr -bitcount
                var result = data
                val src = data
                if (hi != 0) {
                    if (nWords == data.size) {
                        result = IntArray(nWords + 1)
                        data = result
                    }
                    result[nWords++] = hi
                }
                leftShift(src, idx, result, bitcount, prev)
            }
        }
        offset += wordcount
        return this
    }

    private fun leftShiftImmutable(bitcount: Int, wordcount: Int): FDBigInteger {
        if (bitcount == 0) return FDBigInteger(data.copyOf(nWords), offset + wordcount)
        val idx = nWords - 1
        val prev = data[idx]
        val hi = prev ushr -bitcount
        val result: IntArray?
        if (hi != 0) {
            result = IntArray(nWords + 1)
            result[nWords] = hi
        } else {
            result = IntArray(nWords)
        }
        leftShift(data, idx, result, bitcount, prev)
        return FDBigInteger(result, offset + wordcount)
    }

    /**
     * Returns the number of `int`s this [FDBigInteger] represents.
     */
    private fun size(): Int = nWords + offset

    /**
     * Computes
     * ```
     * q = (int)( this / S )
     * this = 10 * ( this mod S )
     * Return q.
     * ```
     * This is the iteration step of digit development for output.
     * We assume that S has been normalized, as above, and that
     * "this" has been left-shifted accordingly.
     * Also assumed, of course, is that the result, q, can be expressed
     * as an integer, `0 <= q < 10`.
     *
     * @param S The divisor of this [FDBigInteger].
     * @return `q = (int)(this / S)`.
     */
    fun quoRemIteration(S: FDBigInteger): Int {
        assert { !this.isImmutable }
        // ensure that this and S have the same number of
        // digits. If S is properly normalized and q < 10 then
        // this must be so.
        val thSize = this.size()
        val sSize = S.size()
        if (thSize >= sSize) require(thSize <= sSize) { "disparate values" }
        else {
            // this value is significantly less than S, result of division is zero.
            // just mult this by 10.
            when (val p = multAndCarryBy10(this.data, this.nWords, this.data)) {
                0 -> trimLeadingZeros()
                else -> this.data[nWords++] = p
            }
            return 0
        }
        // estimate q the obvious way. We will usually be
        // right. If not, then we're only off by a little and
        // will re-add.
        var q = (this.data[this.nWords - 1].toLong() and LONG_MASK) / (S.data[S.nWords - 1].toLong() and LONG_MASK)
        val diff = multSub(q, S)
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
                //
                // Originally the following line read
                // "if ( sum !=0 && sum != -1 )"
                // but that would be wrong, because of the
                // treatment of the two values as entirely unsigned,
                // it would be impossible for a carry-out to be interpreted
                // as -1 -- it would have to be a single-bit carry-out, or +1.
                //
                assert { sum == 0L || sum == 1L }
                q -= 1
            }
        }
        // finally, we can multiply this by 10.
        // it cannot overflow, right, as the high-order word has
        // at least 4 high-order zeros!
        val p = multAndCarryBy10(this.data, this.nWords, this.data)
        assert { p == 0 }
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
        else -> multBy10InPlace()
    }

    fun multBy10InPlace(): FDBigInteger {
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
        return this
    }

    /**
     * Multiplies this [FDBigInteger] by `5^e5 * 2^e2`. The operation will be
     * performed in place if possible, otherwise a new [FDBigInteger]
     * will be returned.
     *
     * @param e5 The exponent of the power-of-five factor.
     * @param e2 The exponent of the power-of-two factor.
     */
    fun multByPow52(e5: Int, e2: Int, dst: FDBigInteger): FDBigInteger {
        assert { !isImmutable }
        assert { dst !== this }
        if (nWords == 0) return this
        var res = this
        if (e5 != 0) {
            dst.clearData()
            val extraSize = if (e2 != 0) 1 else 0 // accounts for e2 % 32 shift bits
            if (e5 < SMALL_5_POW.size) {
                dst.nWords = nWords + 1 + extraSize
                mult(data, nWords, SMALL_5_POW[e5], dst.data)
            } else if (e5 < LONG_5_POW.size) {
                val pow5 = LONG_5_POW[e5]
                dst.nWords = nWords + 2 + extraSize
                mult(data, nWords, pow5.toInt(), (pow5 ushr 32).toInt(), dst.data)
            } else {
                val pow5 = pow5(e5)
                dst.nWords = nWords + pow5.nWords + extraSize
                mult(data, nWords, pow5.data, pow5.nWords, dst.data)
            }
            dst.offset = offset
            dst.trimLeadingZeros()
            res = dst
        }
        return res.leftShift(e2)
    }

    /**
     * Compares the parameter with this [FDBigInteger]. Returns an
     * integer accordingly as:
     * <pre>`> 0: this > other
     * 0: this == other
     * < 0: this < other
    `</pre> *
     *
     * @param other The [FDBigInteger] to compare.
     * @return A negative value, zero, or a positive value according to the
     * result of the comparison.
     */
    fun cmp(other: FDBigInteger): Int {
        val aSize = nWords + offset
        val bSize = other.nWords + other.offset
        if (aSize > bSize) return 1
        if (aSize < bSize) return -1
        var aLen = nWords
        var bLen = other.nWords
        while (aLen > 0 && bLen > 0) {
            val cmp: Int = compareUnsigned(data[--aLen], other.data[--bLen])
            if (cmp != 0) return cmp
        }
        if (aLen > 0) return checkZeroTail(data, aLen)
        if (bLen > 0) return -checkZeroTail(other.data, bLen)
        return 0
    }

    /**
     * Compares this [FDBigInteger] with `x + y`. Returns a
     * value according to the comparison as:
     * <pre>`-1: this <  x + y
     * 0: this == x + y
     * 1: this >  x + y
    `</pre> *
     * @param x The first addend of the sum to compare.
     * @param y The second addend of the sum to compare.
     * @return -1, 0, or 1 according to the result of the comparison.
     */
    fun addAndCmp(x: FDBigInteger, y: FDBigInteger): Int {
        val big: FDBigInteger?
        val small: FDBigInteger?
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
        val thSize = this.size()
        if (bSize == 0) return if (thSize == 0) 0 else 1
        if (sSize == 0) return this.cmp(big)
        if (bSize > thSize) return -1
        if (bSize + 1 < thSize) return 1
        var top = (big.data[big.nWords - 1].toLong() and LONG_MASK)
        if (sSize == bSize) {
            top += (small.data[small.nWords - 1].toLong() and LONG_MASK)
        }
        if ((top ushr 32) == 0L) {
            if (((top + 1) ushr 32) == 0L) {
                // good case - no carry extension
                if (bSize < thSize) return 1
                // here sum.nWords == this.nWords
                val v = (this.data[this.nWords - 1].toLong() and LONG_MASK)
                if (v < top) return -1
                if (v > top + 1) return 1
            }
        } else { // (top>>>32)!=0 guaranteed carry extension
            if (bSize + 1 > thSize) return -1
            // here sum.nWords == this.nWords
            top = top ushr 32
            val v = (this.data[this.nWords - 1].toLong() and LONG_MASK)
            if (v < top) return -1
            if (v > top + 1) return 1
        }
        return this.cmp(big.add(small))
    }

    fun makeImmutable(): FDBigInteger = also { isImmutable = true }
    fun makeMutable(): FDBigInteger = also { isImmutable = false }

    /**
     * Multiplies this [FDBigInteger] by an integer.
     *
     * @param v The factor by which to multiply this [FDBigInteger].
     * @return This [FDBigInteger] multiplied by an integer.
     */
    fun mult(v: Int): FDBigInteger {
        if (nWords == 0 || v == 0) return this

        val r = IntArray(nWords + 1)
        mult(data, nWords, v, r)
        return FDBigInteger(r, offset)
    }

    /**
     * Multiplies this [FDBigInteger] by another [FDBigInteger].
     *
     * @param other The [FDBigInteger] factor by which to multiply.
     * @return The product of this and the parameter [FDBigInteger]s.
     */
    private fun mult(other: FDBigInteger): FDBigInteger {
        val r = IntArray(nWords + other.nWords)
        mult(data, nWords, other.data, other.nWords, r)
        return FDBigInteger(r, offset + other.offset)
    }

    /**
     * Adds another [FDBigInteger] to this [FDBigInteger].
     *
     * @param other The [FDBigInteger] to add.
     * @return The sum of the [FDBigInteger]s.
     */
    private fun add(other: FDBigInteger): FDBigInteger {
        val big: FDBigInteger?
        val small: FDBigInteger?
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
     * Multiplies a [FDBigInteger] by an int and adds another int. The
     * result is computed in place.
     *
     * @param iv The factor by which to multiply this [FDBigInteger].
     * @param addend The value to add to the product of this
     * [FDBigInteger] and `iv`.
     */
    private fun multAdd(iv: Int, addend: Int) {
        assert { !isImmutable }
        val v = iv.toLong() and LONG_MASK
        // unroll 0th iteration, doing addition.
        var p = v * (data[0].toLong() and LONG_MASK) + (addend.toLong() and LONG_MASK)
        data[0] = p.toInt()
        p = p ushr 32
        for (i in 1..<nWords) {
            p += v * (data[i].toLong() and LONG_MASK)
            data[i] = p.toInt()
            p = p ushr 32
        }
        if (p != 0L) {
            data[nWords++] = p.toInt()
        }
    }

    /**
     * Multiplies the parameters and subtracts them from this [FDBigInteger].
     *
     * @param q The integer parameter.
     * @param S The [FDBigInteger] parameter.
     * @return `this - q*S`.
     */
    private fun multSub(q: Long, S: FDBigInteger): Long {
        var diff = 0L
        if (q != 0L) {
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
        }
        return diff
    }

    fun toByteArray(): ByteArray {
        val magnitude = ByteArray(4 * size() + 1) // +1 for the "sign" byte
        var i = 0
        var j = magnitude.size - 4 * offset
        while (i < nWords) {
            val w = data[i]
            magnitude[j - 1] = w.toByte()
            magnitude[j - 2] = (w shr 8).toByte()
            magnitude[j - 3] = (w shr 16).toByte()
            magnitude[j - 4] = (w shr 24).toByte()
            i += 1
            j -= 4
        }
        return magnitude
    }

    override fun toString(): String {
        return "FDBigInteger(data=${data.contentToString()}, offset=$offset, nWords=$nWords)"
    }

    companion object {
        val SMALL_5_POW: IntArray = IntArray(13 + 1) // 5^13 fits in an int, 5^14 does not

        val LONG_5_POW: LongArray

        // Size of full cache of powers of 5 as FDBigIntegers.
        private const val MAX_FIVE_POW = 340

        // Cache of big powers of 5 as FDBigIntegers.
        private val POW_5_CACHE: Array<FDBigInteger>

        // Constant for casting an int to a long via bitwise AND.
        private const val LONG_MASK = 0xffffffffL

        /**
         * Returns an [FDBigInteger] with the numerical value
         * 5<sup>`e5`</sup> * 2<sup>`e2`</sup>.
         *
         * @param e5 The exponent of the power-of-five factor.
         * @param e2 The exponent of the power-of-two factor.
         * @return 5<sup>`e5`</sup> * 2<sup>`e2`</sup>
         */
        fun valueOfPow52(e5: Int, e2: Int): FDBigInteger {
            if (e5 == 0) return valueOfPow2(e2)
            if (e2 == 0) return pow5(e5)
            if (e5 >= SMALL_5_POW.size) return pow5(e5).leftShift(e2)
            val pow5 = SMALL_5_POW[e5]
            val offset = e2 shr 5
            val bitcount = e2 and 0x1f
            if (bitcount == 0) return FDBigInteger(intArrayOf(pow5), offset)
            return FDBigInteger(
                intArrayOf(
                    pow5 shl bitcount,
                    pow5 ushr -bitcount
                ), offset
            )
        }

        /**
         * Returns an [FDBigInteger] with the numerical value:
         * `value * 5^e5 * 2^e2`.
         *
         * @param value The constant factor.
         * @param e5 The exponent of the power-of-five factor.
         * @param e2 The exponent of the power-of-two factor.
         */
        fun valueOfMulPow52(value: Long, e5: Int, e2: Int, dst: FDBigInteger): FDBigInteger {
            assert { dst.data.size >= 4 }
            var v0 = value.toInt()
            var v1 = (value ushr 32).toInt()
            val offset = e2 shr 5
            val bitcount = e2 and 0x1f
            if (e5 != 0) {
                if (e5 < SMALL_5_POW.size) {
                    val pow5 = SMALL_5_POW[e5].toLong() and LONG_MASK
                    var carry = (v0.toLong() and LONG_MASK) * pow5
                    v0 = carry.toInt()
                    carry = carry ushr 32
                    carry = (v1.toLong() and LONG_MASK) * pow5 + carry
                    v1 = carry.toInt()
                    val v2 = (carry ushr 32).toInt()
                    return when (bitcount) {
                        0 -> dst.initData3(v0, v1, v2, offset)
                        else -> dst.initData4(
                            v0 shl bitcount,
                            (v1 shl bitcount) or (v0 ushr -bitcount),
                            (v2 shl bitcount) or (v1 ushr -bitcount),
                            v2 ushr -bitcount,
                            offset
                        )
                    }
                }
                val pow5 = pow5(e5)
                val dataSize: Int
                if (v1 == 0) {
                    dataSize = pow5.nWords + 1 + (if (e2 != 0) 1 else 0)
                    mult(pow5.data, pow5.nWords, v0, dst = dst.data)
                } else {
                    dataSize = pow5.nWords + 2 + (if (e2 != 0) 1 else 0)
                    mult(pow5.data, pow5.nWords, v0, v1, dst = dst.data)
                }
                dst.setDataSize(nWords = dataSize, offset = 0)
                return dst.leftShift(e2)
            }
            if (e2 != 0) return when (bitcount) {
                0 -> dst.initData2(v0, v1, offset)
                else -> dst.initData3(
                    v0 shl bitcount,
                    (v1 shl bitcount) or (v0 ushr -bitcount),
                    v1 ushr -bitcount,
                    offset
                )
            }
            return dst.initData2(v0, v1, 0)
        }

        /**
         * Returns an [FDBigInteger] with the numerical value
         * 2<sup>`e`</sup>.
         *
         * @param e The exponent of 2.
         * @return 2<sup>`e`</sup>
         */
        private fun valueOfPow2(e: Int) = FDBigInteger(intArrayOf(1 shl (e and 0x1f)), e shr 5)

        /**
         * Left shifts the contents of one int array into another.
         *
         * @param src The source array.
         * @param idx The initial index of the source array.
         * @param result The destination array.
         * @param bitcount The left shift.
         * @param prev The prior source value.
         */
        private fun leftShift(src: IntArray, idx: Int, result: IntArray, bitcount: Int, prev: Int) {
            var idx = idx
            var prev = prev
            while (idx > 0) {
                var v = prev shl bitcount
                prev = src[idx - 1]
                v = v or (prev ushr -bitcount)
                result[idx] = v
                idx--
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
            var s1 = s1
            var s1Len = s1Len
            var s2 = s2
            var s2Len = s2Len
            if (s1Len > s2Len) {
                /* Swap ensures that inner loop is longest. */
                val l = s1Len
                s1Len = s2Len
                s2Len = l
                val s = s1
                s1 = s2
                s2 = s
            }
            for (i in 0..<s1Len) {
                val v = s1[i].toLong() and LONG_MASK
                var p = 0L
                for (j in 0..<s2Len) {
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
            var from = from
            while (from > 0) {
                if (a[--from] != 0) return 1
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
            var carry: Long = 0
            for (i in 0..<srcLen) {
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
        private fun mult(src: IntArray, srcLen: Int, value: Int, dst: IntArray) {
            val v = value.toLong() and LONG_MASK
            var carry = 0L
            var i = 0
            while (i < srcLen) {
                val product = v * (src[i].toLong() and LONG_MASK) + carry
                dst[i] = product.toInt()
                carry = product ushr 32
                i++
            }
            dst[i] = carry.toInt()
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
        private fun mult(src: IntArray, srcLen: Int, v0: Int, v1: Int, dst: IntArray) {
            mult(src, srcLen, v0, dst)

            val v = v1.toLong() and LONG_MASK
            var carry = 0L
            var i = 1
            while (i <= srcLen) {
                val product = (dst[i].toLong() and LONG_MASK) + v * (src[i - 1].toLong() and LONG_MASK) + carry
                dst[i] = product.toInt()
                carry = product ushr 32
                i++
            }
            dst[i] = carry.toInt()
        }

        /*
         * Lookup table of powers of 5 starting with 5^MAX_FIVE_POW.
         * The size just serves for the conversions.
         * It is filled lazily, except for the entries with exponent
         * 2 (MAX_FIVE_POW - 1) and 3 (MAX_FIVE_POW - 1).
         *
         * Access needs not be synchronized for thread-safety, since races would
         * produce the same non-null value (although not the same instance).
         */
        private val LARGE_POW_5_CACHE: Array<FDBigInteger?>

        // Initialize FDBigInteger cache of powers of 5.
        init {
            SMALL_5_POW[0] = 1
            for (i in 1..<SMALL_5_POW.size) {
                SMALL_5_POW[i] = 5 * SMALL_5_POW[i - 1]
            }

            LONG_5_POW = LongArray(27 + 1) // 5^27 fits in a long, 5^28 does not
            LONG_5_POW[0] = 1
            for (i in 1..<LONG_5_POW.size) {
                LONG_5_POW[i] = 5 * LONG_5_POW[i - 1]
            }

            val pow5cache = arrayOfNulls<FDBigInteger>(MAX_FIVE_POW)
            var i = 0
            while (i < LONG_5_POW.size) {
                pow5cache[i] = FDBigInteger(LONG_5_POW[i]).makeImmutable()
                ++i
            }
            var prev = pow5cache[i - 1]!!
            while (i < MAX_FIVE_POW) {
                prev = prev.mult(5).makeImmutable()
                pow5cache[i] = prev
                ++i
            }
            @Suppress("UNCHECKED_CAST")
            POW_5_CACHE = pow5cache as Array<FDBigInteger>

            /* Here prev is 5^(MAX_FIVE_POW-1). */
            LARGE_POW_5_CACHE = arrayOfNulls<FDBigInteger>((2 - DoubleToDecimal.Q_MIN) - MAX_FIVE_POW + 1)
            prev = prev.mult(prev).makeImmutable()
            LARGE_POW_5_CACHE[2 * (MAX_FIVE_POW - 1) - MAX_FIVE_POW] = prev
            LARGE_POW_5_CACHE[3 * (MAX_FIVE_POW - 1) - MAX_FIVE_POW] =
                pow5cache[MAX_FIVE_POW - 1].mult(prev).makeImmutable()
        }

        /**
         * Computes `5^e`.
         * @throws IllegalArgumentException if e > 2 - DoubleToDecimal.Q_MIN = 1076
         */
        private fun pow5(e: Int): FDBigInteger {
            if (e < MAX_FIVE_POW) return POW_5_CACHE[e]

            require(e <= 2 - DoubleToDecimal.Q_MIN) { "exponent too large: $e" }
            var p5 = LARGE_POW_5_CACHE[e - MAX_FIVE_POW]
            if (p5 == null) {
                val ep = (e - 1) - (e - 1) % (MAX_FIVE_POW - 1)
                p5 = (if (ep < MAX_FIVE_POW) POW_5_CACHE[ep] else LARGE_POW_5_CACHE[ep - MAX_FIVE_POW])!!
                    .mult(POW_5_CACHE[e - ep])
                LARGE_POW_5_CACHE[e - MAX_FIVE_POW] = p5.makeImmutable()
            }
            return p5
        }

        private val DATA_MAX_INIIAL_SIZE = pow5(2 - DoubleToDecimal.Q_MIN).nWords + 3
    }
}

internal expect fun compareUnsigned(a: Int, b: Int): Int