package io.kodec

import io.kodec.PreparedStringToFpBuffer.Companion.NEGATIVE_INFINITY
import io.kodec.PreparedStringToFpBuffer.Companion.NEGATIVE_ZERO
import io.kodec.PreparedStringToFpBuffer.Companion.NOT_A_NUMBER
import io.kodec.PreparedStringToFpBuffer.Companion.POSITIVE_INFINITY
import io.kodec.PreparedStringToFpBuffer.Companion.POSITIVE_ZERO
import io.kodec.buffers.Buffer
import karamel.utils.ThreadLocal
import karamel.utils.suppress
import kotlin.jvm.JvmStatic

/**
 * A converter which can process an ASCII `String` representation
 * of a single or double precision floating point value into a
 * `float` or a `double`.
 */
object FloatingDecimalParsing {
    const val MAX_DIGITS: Int = 128

    private const val MIN_DECIMAL_EXPONENT: Int = -324
    private const val BIG_DECIMAL_EXPONENT: Int = 324 // i.e. abs(MIN_DECIMAL_EXPONENT)

    @Throws(NumberFormatException::class)
    fun parseDouble(s: String): Double = readString(s).doubleValue()

    @Throws(NumberFormatException::class)
    fun parseFloat(s: String): Float = readString(s).floatValue()

    @Throws(NumberFormatException::class)
    fun parseDouble(s: String, start: Int, endExclusive: Int = s.length): Double =
        readString(s, start, endExclusive).doubleValue()

    @Throws(NumberFormatException::class)
    fun parseFloat(s: String, start: Int, endExclusive: Int = s.length): Float =
        readString(s, start, endExclusive).floatValue()

    private class ThreadLocalCache {
        val buffer = StringToFpBuffer()

        val asciiWholeBuffer = WholeAsciiStringAsBuffer()
        val asciiBuffer = AsciiStringAsBuffer()
    }

    @JvmStatic private val TL_CACHE = ThreadLocal { ThreadLocalCache() }

    fun readString(
        s: CharSequence,
        onFormatError: DecodingErrorHandler<String>? = null
    ): StringToFpConverter {
        val cache = TL_CACHE.get()
        cache.asciiWholeBuffer.string = s
        return readBuffer(cache.asciiWholeBuffer, onFormatError = onFormatError)
    }

    fun readString(
        s: CharSequence,
        start: Int, endExclusive: Int,
        onFormatError: DecodingErrorHandler<String>? = null
    ): StringToFpConverter {
        val cache = TL_CACHE.get()
        cache.asciiBuffer.setString(s, start, endExclusive)
        return readBuffer(cache.asciiBuffer, onFormatError = onFormatError)
    }

    @JvmStatic
    internal val DEFAULT_ERROR_HANDLER = DecodingErrorHandler { message: String ->
        throw NumberFormatException(message)
    }

    fun readBuffer(
        input: Buffer,
        start: Int = 0,
        endExclusive: Int = input.size,
        onFormatError: DecodingErrorHandler<String>? = null
    ): StringToFpConverter {
        val onFormatError = onFormatError ?: DEFAULT_ERROR_HANDLER

        var isNegative = false
        var signSeen = false

        if (endExclusive - start > 0)
        suppress<IndexOutOfBoundsException> parseNumber@ {
            var i = start

            when (input[start]) {
                '-'.code -> {
                    isNegative = true
                    signSeen = true
                    i++
                }
                '+'.code -> {
                    signSeen = true
                    i++
                }
                'N'.code -> {
                    if (i + 3 != endExclusive ||
                        input[i + 1] != 'a'.code ||
                        input[i + 2] != 'N'.code)
                        return@parseNumber
                    return NOT_A_NUMBER
                }
            }

            if (input[i] == 'I'.code) {
                if (i + Float32Consts.INFINITY_REP_ARRAY.size != endExclusive ||
                    !input.equalsRange(Float32Consts.INFINITY_REP_ARRAY, thisOffset = i + 1, otherOffset = 1)) {
                    return@parseNumber
                }
                return if (isNegative) NEGATIVE_INFINITY else POSITIVE_INFINITY
            }

            val buffer = TL_CACHE.get().buffer
            val digits = buffer.digits
            digits.fill(0, toIndex = buffer.nDigits)

            var decSeen = false
            var nDigits = 0
            var decPt = 0 // number of decimal points
            var nLeadZero = 0
            var nTrailZero = 0

            while (i < endExclusive) {
                when (input[i]) {
                    '0'.code -> nLeadZero++
                    '.'.code -> {
                        if (decSeen) {
                            onFormatError("multiple points")
                            return NOT_A_NUMBER
                        }
                        decPt = i - start
                        if (signSeen) decPt--
                        decSeen = true
                    }
                    else -> break
                }
                i++
            }

            var c: Int
            while (i < endExclusive) {
                c = input[i]
                when (c) {
                    in '0'.code..'9'.code -> {
                        val idx = nDigits++
                        if (idx >= digits.size) {
                            onFormatError("number is too long")
                            return NOT_A_NUMBER
                        }
                        digits[idx] = c.toByte()
                        if (c == '0'.code) nTrailZero++ else nTrailZero = 0
                    }
                    '.'.code -> {
                        if (decSeen) {
                            onFormatError("multiple points")
                            return NOT_A_NUMBER
                        }
                        decPt = i - start
                        if (signSeen) decPt--
                        decSeen = true
                    }
                    else -> break
                }
                i++
            }
            nDigits -= nTrailZero

            // At this point, we've scanned all the digits and decimal point we're going to see.
            // Trim off leading and trailing zeros, which will just confuse us later, and adjust
            // our initial decimal exponent accordingly.
            if (nDigits == 0 && nLeadZero == 0) return@parseNumber // we saw NO DIGITS AT ALL

            // Our initial exponent is decPt, adjusted by the number of discarded zeros.
            // Or, if there was no decPt, then its just nDigits adjusted by discarded trailing zeros.
            var decExp: Int = if (decSeen) decPt - nLeadZero else nDigits + nTrailZero

            // Look for 'e' or 'E' and an optionally signed integer.
            if (i < endExclusive && (input[i].also { c = it } == 'e'.code || c == 'E'.code)) {
                var expSign = 1
                var expVal = 0
                var expOverflow = false
                when (input[++i]) {
                    '-'.code -> { i++; expSign = -1 }
                    '+'.code -> { i++; }
                }
                val expAt = i
                while (i < endExclusive) {
                    if (expVal >= Int.MAX_VALUE / 10) {
                        // the next character will cause integer overflow.
                        expOverflow = true
                    }
                    c = input[i++]
                    if (c in '0'.code..'9'.code) {
                        expVal = expVal * 10 + (c - '0'.code)
                    } else {
                        i--
                        break
                    }
                }
                val expLimit = BIG_DECIMAL_EXPONENT + nDigits + nTrailZero
                if (expOverflow || expVal > expLimit) {
                    // There is still a chance that the exponent will be safe to use: if it
                    // would eventually decrease due to a negative decExp, and that number
                    // is below the limit.  We check for that here.
                    if (!expOverflow && (expSign == 1 && decExp < 0) && expVal + decExp < expLimit) {
                        // Cannot overflow: adding a positive and negative number.
                        decExp += expVal
                    } else {
                        // The intent here is to end up with infinity or zero, as appropriate.
                        // The reason for yielding such a small decExponent, rather than something
                        // intuitive such as expSign*Integer.MAX_VALUE, is that this value is
                        // subject to further manipulation in  doubleValue() and floatValue(),
                        // and I don't want it to be able to cause overflow there! (The only way
                        // we can get into trouble here is for really outrageous nDigits+nTrailZero,
                        // such as 2 billion.)
                        decExp = expSign * expLimit
                    }
                } else {
                    // this should not overflow, since we tested
                    // for expVal > (MAX+N), where N >= abs(decExp)
                    decExp += expSign * expVal
                }

                // if we saw something not a digit ( or end of string ) after the [Ee][+-],
                // without seeing any digits at all this is certainly an error. If we saw some
                // digits, but then some trailing garbage, that might be ok. so we just fall
                // through in that case.
                if (i == expAt) return@parseNumber  // certainly bad
            }

            // We parsed everything we could. If there are leftovers, then this is not good input!
            if (i < endExclusive &&
                ((i != endExclusive - 2) ||
                 (input[i] != 'f'.code &&
                  input[i] != 'F'.code &&
                  input[i] != 'd'.code &&
                  input[i] != 'D'.code))
            ) return@parseNumber

            if (nDigits == 0 ||
                // Prevent an extreme negative exponent from causing overflow issues in doubleValue().
                // Large positive values are handled within doubleValue().
                decExp < MIN_DECIMAL_EXPONENT)
            {
                return if (isNegative) NEGATIVE_ZERO else POSITIVE_ZERO
            }

            buffer.isNegative = isNegative
            buffer.decExponent = decExp
            buffer.nDigits = nDigits

            return buffer
        }

        onFormatError("malformed number")
        return NOT_A_NUMBER
    }
}