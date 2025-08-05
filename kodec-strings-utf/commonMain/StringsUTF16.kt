package io.kodec

import io.kodec.StringsUTF8.HIGH_SURROGATE_HEADER
import io.kodec.StringsUTF8.LOW_SURROGATE_HEADER

object StringsUTF16 {
    /** Basic Multilingual Plane (`U+0000` - `U+FFFF`) */
    const val BASIC_PLANE_BITS: Int = 16
    /** Minimum code point of Supplementary Multilingual Plane (`U+10000`) */
    const val MIN_SUPPLEMENTARY_CODE_POINT: Int = 1 shl BASIC_PLANE_BITS

    /** UTF-16 high surrogate: 110110xxxxxxxxxx (10 bits) */
    fun highSurrogateCharCode(codePoint: Int): Int = (codePoint ushr 10) + HIGH_SURROGATE_HEADER
    /** UTF-16 low surrogate:  110111yyyyyyyyyy (10 bits) */
    fun lowSurrogateCharCode(codePoint: Int): Int = (codePoint and 0x3ff) + LOW_SURROGATE_HEADER

    fun highSurrogate(codePoint: Int): Char = highSurrogateCharCode(codePoint).toChar()
    fun lowSurrogate(codePoint: Int): Char = lowSurrogateCharCode(codePoint).toChar()

    fun codePoint(highSurrogate: Int, lowSurrogate: Int): Int {
        // UTF-16 high surrogate: 110110xxxxxxxxxx (10 bits)
        // UTF-16 low surrogate:  110111yyyyyyyyyy (10 bits)
        // Unicode code point:    00010000000000000000 + xxxxxxxxxxyyyyyyyyyy (21 bits)
        return (0x010000 - (0xd800 shl 10) - 0xdc00) + ((highSurrogate shl 10) + lowSurrogate)
    }

    fun countCodePoints(s: CharSequence): Int {
        val length = s.length
        var result = 0
        var i = 0
        while(i < length) {
            i += if (s[i].isSurrogate()) 2 else 1
            result++
        }
        return result
    }

    inline fun getCharsHeavyInline(codePoint: Int, acceptChar: (Char) -> Unit) {
        getCharCodesHeavyInline(codePoint) { acceptChar(it.toChar()) }
    }

    inline fun getCharCodesHeavyInline(codePoint: Int, acceptCharCode: (Int) -> Unit) {
        var c = codePoint
        if (codePoint >= MIN_SUPPLEMENTARY_CODE_POINT) {
            acceptCharCode(highSurrogateCharCode(codePoint))
            c = lowSurrogateCharCode(codePoint)
        }

        acceptCharCode(c)
    }

    inline fun getChars(codePoint: Int, acceptChar: (Char) -> Unit) {
        getCharCodes(codePoint) { acceptChar(it.toChar()) }
    }

    inline fun getCharCodes(codePoint: Int, acceptCharCode: (Int) -> Unit) {
        var c1 = codePoint
        var c2 = -1

        if (codePoint >= MIN_SUPPLEMENTARY_CODE_POINT) {
            c1 = highSurrogateCharCode(codePoint)
            c2 = lowSurrogateCharCode(codePoint)
        }

        do {
            acceptCharCode(c1)
            c1 = c2
            c2 = -1
        } while (c1 >= 0)
    }

    inline fun readFromCharStream(
        dest: CharArray,
        destStart: Int,
        destEnd: Int,
        readChar: () -> Char
    ) {
        for (i in destStart until destEnd)
            dest[i] = readChar()
    }

    inline fun readFromByteStream(
        readByte: () -> Int,
        acceptChar: (Char) -> Unit
    ) {
        while (true) {
            val b0 = readByte()
            if (b0 < 0) break

            val b1 = readByte()
            if (b1 < 0) {
                acceptChar(StringsASCII.INVALID_BYTE_PLACEHOLDER)
                break
            }

            acceptChar((b0 or (b1 shl 8)).toChar())
        }
    }

    inline fun readFromByteStream(
        dest: CharArray,
        destStart: Int,
        destEnd: Int,
        readByte: () -> Int
    ) {
        readFromCharStream(dest, destStart, destEnd,
            readChar = { (readByte() or (readByte() shl 8)).toChar() })
    }

    inline fun writeChars(
        str: CharSequence,
        strStart: Int = 0,
        strEnd: Int = str.length,
        writeChar: (Char) -> Int
    ): Int {
        var written = 0

        for (i in strStart ..< strEnd)
            written += writeChar(str[i])

        return written
    }

    inline fun writeBytes(
        str: CharSequence,
        strStart: Int = 0,
        strEnd: Int = str.length,
        writeByte: (Int) -> Unit
    ): Int {
        return writeChars(str, strStart, strEnd, writeChar = {
            writeByte(it.code)
            writeByte(it.code ushr 8)
            2
        })
    }
}

