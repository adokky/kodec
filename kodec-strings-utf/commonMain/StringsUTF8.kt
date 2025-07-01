package io.kodec

import io.kodec.StringsASCII.INVALID_BYTE_PLACEHOLDER
import karamel.utils.assert
import kotlin.jvm.JvmField

/*
Unicode code points  Encoding  Binary value
-------------------  --------  ------------
 U+000000-U+00007f   0xxxxxxx  0xxxxxxx

 U+000080-U+0007ff   110yyyxx  00000yyy xxxxxxxx
                     10xxxxxx

 U+000800-U+00ffff   1110yyyy  yyyyyyyy xxxxxxxx
                     10yyyyxx
                     10xxxxxx

 U+010000-U+10ffff   11110zzz  000zzzzz yyyyyyyy xxxxxxxx
                     10zzyyyy
                     10yyyyxx
                     10xxxxxx

The basic rules:
- If a byte starts with a 0 bit, it's a single byte value less than 128.
- If it starts with 11, it's the first byte of a multibyte sequence and the number of 1 bits at the start
indicates how many bytes there are in total (110xxxxx has two bytes, 1110xxxx has three and 11110xxx has four).
- If it starts with 10, it's a continuation byte.

This distinction allows quite handy processing such as being able to back up from any byte in a sequence
to find the first byte of that code point. Just search backwards until you find one not beginning with the 10 bits.

Similarly, it can also be used for a UTF-8 strlen by only counting non-10xxxxxx bytes.

See also:
https://chromium.googlesource.com/external/github.com/google/protobuf/+/HEAD/java/core/src/main/java/com/google/protobuf/Utf8.java
*/

object StringsUTF8 {
    // Value added to the high UTF-16 surrogate after shifting
    const val HIGH_SURROGATE_HEADER: Int = 0xd800 - (0x010000 ushr 10)

    // Value added to the low UTF-16 surrogate after masking
    const val LOW_SURROGATE_HEADER: Int = 0xdc00

    @JvmField val INVALID_BYTE_PLACEHOLDER_BYTES: ByteArray = run {
        assert { !INVALID_BYTE_PLACEHOLDER.isSurrogate() }
        var size = 0
        val bytes = ByteArray(4)
        write(
            writeByte = { bytes[size++] = it },
            INVALID_BYTE_PLACEHOLDER,
            lowSurrogate = { error("char is not surrogate") }
        )
        bytes.copyOf(size)
    }

    // char code -> 2 byte encoding
    fun Int.utf8_2byte_0(): Byte = (0xC0 or (this ushr 6 and 0x1F)).toByte()
    fun Int.utf8_2byte_1(): Byte = (0x80 or (this and 0x3F)).toByte()

    // char code -> 3 byte encoding
    fun Int.utf8_3byte_0(): Byte = (0xE0 or (this ushr 12 and 0x0F)).toByte()
    fun Int.utf8_3byte_1(): Byte = (0x80 or (this ushr 6 and 0x3F)).toByte()
    fun Int.utf8_3byte_2(): Byte = (0x80 or (this and 0x3F)).toByte()

    // char code -> 4 byte encoding
    fun Int.utf8_4byte_0(): Byte = (this shr 18          or 0xf0).toByte() // 11110xxx
    fun Int.utf8_4byte_1(): Byte = (this shr 12 and 0x3f or 0x80).toByte() // 10xxxxxx
    fun Int.utf8_4byte_2(): Byte = (this shr 6  and 0x3f or 0x80).toByte() // 10xxyyyy
    fun Int.utf8_4byte_3(): Byte = (this        and 0x3f or 0x80).toByte() // 10yyyyyy

    // check for different length of encoded codepoint by first byte
    fun Int.is_header_2_bytes(): Boolean = this and 0xe0 == 0xc0
    fun Int.is_header_3_bytes(): Boolean = this and 0xf0 == 0xe0
    fun Int.is_header_4_bytes(): Boolean = this and 0xf8 == 0xf0

    // char code -> number of UTF-8 bytes
    fun Int.is1UtfByte(): Boolean = this < 0x80
    fun Int.is2UtfByte(): Boolean = this < 0x800
    fun Int.is3UtfByte(): Boolean = this < Char.MIN_SURROGATE.code || Char.MAX_SURROGATE.code < this

    // UTF codepoints from UTF-8 bytes

    fun codePoint(byte0: Int, byte1: Int): Int =
        (byte0 and 0x1f shl 6) or
        (byte1 and 0x3f)

    fun codePoint(byte0: Int, byte1: Int, byte2: Int): Int =
        (byte0 and 0x0f shl 12) or
        (byte1 and 0x3f shl 6) or
        (byte2 and 0x3f)

    fun codePoint(byte0: Int, byte1: Int, byte2: Int, byte3: Int): Int =
        (byte0 and 0x07 shl 18) or
        (byte1 and 0x3f shl 12) or
        (byte2 and 0x3f shl 6) or
        (byte3 and 0x3f)

    fun char(byte0: Int, byte1: Int): Char = codePoint(byte0, byte1).toChar()

    fun char(byte0: Int, byte1: Int, byte2: Int): Char = codePoint(byte0, byte1, byte2).toChar()

    @PublishedApi
    internal inline fun writeSlow(
        writeByte: (Byte) -> Unit,
        str: CharSequence,
        strStart: Int,
        strEnd: Int
    ): Int {
        var i = strStart
        var written = 0

        while (i < strEnd) {
            written += write(writeByte, str[i],
                lowSurrogate = {
                    i++
                    if (i >= strEnd) {
                        for (b in INVALID_BYTE_PLACEHOLDER_BYTES) writeByte(b)
                        return written + INVALID_BYTE_PLACEHOLDER_BYTES.size
                    }
                    str[i]
                }
            )
            i++
        }

        return written
    }

    /**
     * [lowSurrogate] must be provided in order to support UTF-16 surrogate characters!
     * Default [lowSurrogate] throws [IllegalArgumentException].
     */
    inline fun write(
        writeByte: (Byte) -> Unit,
        char: Char,
        lowSurrogate: () -> Char = { throw IllegalArgumentException("surrogate chars are not supported. Char code: ${char.code}") }
    ): Int {
        val c = char.code
        return when {
            c.is1UtfByte() -> {
                writeByte(c.toByte())
                1
            }
            c.is2UtfByte() -> {
                writeByte(c.utf8_2byte_0())
                writeByte(c.utf8_2byte_1())
                2
            }
            c.is3UtfByte() -> {
                writeByte(c.utf8_3byte_0())
                writeByte(c.utf8_3byte_1())
                writeByte(c.utf8_3byte_2())
                3
            }
            else -> {
                val codePoint = StringsUTF16.codePoint(c, lowSurrogate().code)
                writeByte(codePoint.utf8_4byte_0())
                writeByte(codePoint.utf8_4byte_1())
                writeByte(codePoint.utf8_4byte_2())
                writeByte(codePoint.utf8_4byte_3())
                4
            }
        }
    }

    fun getByteLength(char: Char): Int {
        val c = char.code

        // fast path - branch free
        if (c < 0x800) return 1 + ((0x7f - c) ushr 31)

        // slow path
        return if (c.is3UtfByte()) 3 else 2 // 2-byte high/low surrogate
    }

    /**
     * Returns the number of bytes in the UTF-8-encoded form of `sequence`.
     */
    fun getByteLength(sequence: CharSequence): Int {
        val utf16Length = sequence.length
        var utf8Length = utf16Length
        var i = 0

        // This loop optimizes for pure ASCII.
        while (i < utf16Length && sequence[i].code < 0x80) {
            i++
        }

        // This loop optimizes for chars less than 0x800.
        while (i < utf16Length) {
            val c = sequence[i]
            if (c.code >= 0x800) return utf8Length + getUtfLengthSlow(sequence, i)
            utf8Length += 0x7f - c.code ushr 31 // branch free!
            i++
        }

        return utf8Length
    }

    private fun getUtfLengthSlow(sequence: CharSequence, start: Int): Int {
        val utf16Length = sequence.length
        var utf8Length = 0
        var i = start
        while (i < utf16Length) {
            val c = sequence[i]
            if (c.code < 0x800) {
                utf8Length += 0x7f - c.code ushr 31 // branch free!
            } else {
                utf8Length += 2
                if (c in Char.MIN_SURROGATE..Char.MAX_SURROGATE) i++
            }
            i++
        }
        return utf8Length
    }

    inline fun readFromByteStreamInto(
        dest: CharArray,
        destStart: Int,
        destEnd: Int,
        readByte: () -> Int
    ) {
        if (destEnd - destStart <= 0) return

        var i = destStart

        readFromByteStream(
            readByte = { if (i >= destEnd) -1 else readByte() },
            appendChar = { dest[i++] = it }
        )
    }

    inline fun readFromBytes(
        getByte: (pos: Int) -> Int,
        endExclusive: Int,
        appendChar: (Char) -> Unit
    ): Int {
        require(endExclusive >= 0) { "invalid 'endExclusive': $endExclusive" }

        if (endExclusive == 0) return 0

        var firstByte = getByte(0)
        var pos = 1

        // fast path: ASCII-only sequences
        while (firstByte < 128) {
            appendChar(firstByte.toChar())
            if (pos >= endExclusive) return pos
            firstByte = getByte(pos++)
        }

        // slow path: capable to handle non-ASCII
        while (true) {
            if (firstByte < 128) {
                appendChar(firstByte.toChar())
            } else if (firstByte.is_header_2_bytes()) {
                if (pos >= endExclusive) break
                appendChar(char(firstByte, getByte(pos++)))
            } else if (firstByte.is_header_3_bytes()) {
                if (pos + 1 >= endExclusive) break
                val b1 = getByte(pos)
                val b2 = getByte(pos + 1)
                pos += 2
                appendChar(char(firstByte, b1, b2))
            } else if (firstByte.is_header_4_bytes()) {
                if (pos + 2 >= endExclusive) break
                val b1 = getByte(pos)
                val b2 = getByte(pos + 1)
                val b3 = getByte(pos + 2)
                pos += 3
                val codePoint = codePoint(firstByte, b1, b2, b3)
                appendChar(StringsUTF16.highSurrogate(codePoint))
                appendChar(StringsUTF16.lowSurrogate(codePoint))
            } else {
                appendChar(INVALID_BYTE_PLACEHOLDER)
            }

            if (pos >= endExclusive) return pos

            firstByte = getByte(pos++)
        }

        appendChar(INVALID_BYTE_PLACEHOLDER)
        return endExclusive
    }

    inline fun readCharUnsafe(
        readByte: () -> Int,
        acceptSingle: (Char) -> Unit,
        acceptSurrogate: (highSurrogate: Char, lowSurrogate: Char) -> Unit
    ) {
        readCharUnsafe(firstByte = readByte(), readByte, acceptSingle, acceptSurrogate)
    }

    inline fun readCharUnsafe(
        firstByte: Int,
        readByte: () -> Int,
        acceptSingle: (Char) -> Unit,
        acceptSurrogate: (highSurrogate: Char, lowSurrogate: Char) -> Unit
    ) {
        if (firstByte < 128) {
            acceptSingle(firstByte.toChar())
        } else if (firstByte.is_header_2_bytes()) {
            acceptSingle(char(firstByte, readByte()))
        } else if (firstByte.is_header_3_bytes()) {
            val b1 = readByte()
            val b2 = readByte()
            acceptSingle(char(firstByte, b1, b2))
        } else if (firstByte.is_header_4_bytes()) {
            val b1 = readByte()
            val b2 = readByte()
            val b3 = readByte()
            val codePoint = codePoint(firstByte, b1, b2, b3)
            acceptSurrogate(
                StringsUTF16.highSurrogate(codePoint),
                StringsUTF16.lowSurrogate(codePoint)
            )
        } else {
            acceptSingle(INVALID_BYTE_PLACEHOLDER)
        }
    }

    inline fun readCodePoint(readByte: () -> Int): Int = readCodePoint(firstByte = readByte(), readByte)

    inline fun readCodePoint(firstByte: Int, readByte: () -> Int): Int = when {
        firstByte < 128 -> firstByte
        firstByte.is_header_2_bytes() -> codePoint(firstByte, readByte())
        firstByte.is_header_3_bytes() -> codePoint(firstByte, readByte(), readByte())
        firstByte.is_header_4_bytes() -> codePoint(firstByte, readByte(), readByte(), readByte())
        else -> INVALID_BYTE_PLACEHOLDER.code
    }

    /**
     * @param readByte must return unsigned value of next byte or -1 there are no any data available.
     */
    inline fun readFromByteStream(
        readByte: () -> Int,
        appendChar: (Char) -> Unit
    ) {
        var firstByte = readByte()

        // fast path: ASCII-only sequences
        while (firstByte < 128) {
            if (firstByte < 0) return // EOF
            appendChar(firstByte.toChar())
            firstByte = readByte()
        }

        // slow path: capable to handle non-ASCII
        run {
            while (firstByte >= 0) {
                readCharUnsafe(
                    firstByte = firstByte,
                    readByte = { readByte().also { if (it < 0) return@run } },
                    acceptSingle = appendChar,
                    acceptSurrogate = { hs, ls ->
                        appendChar(hs)
                        appendChar(ls)
                    }
                )
                firstByte = readByte()
            }
            return
        }

        appendChar(INVALID_BYTE_PLACEHOLDER)
    }

    inline fun readFromByteStreamUntil(
        readByte: () -> Int,
        appendChar: (Char) -> Boolean
    ) {
        var firstByte = readByte()

        // fast path: ASCII-only sequences
        while (firstByte < 128) {
            if (firstByte < 0) return // EOF
            if (!appendChar(firstByte.toChar())) return
            firstByte = readByte()
        }

        // slow path: capable to handle non-ASCII
        run {
            while (firstByte >= 0) {
                readCharUnsafe(
                    firstByte = firstByte,
                    readByte = { readByte().also { if (it < 0) return@run } },
                    acceptSingle = { c ->
                        if (!appendChar(c)) return
                    },
                    acceptSurrogate = { hs, ls ->
                        if (!appendChar(hs)) return
                        if (!appendChar(ls)) return
                    }
                )

                firstByte = readByte()
            }
        }

        appendChar(INVALID_BYTE_PLACEHOLDER)
    }

    inline fun write(
        str: CharSequence,
        strStart: Int,
        strEnd: Int,
        writeByte: (Byte) -> Unit
    ): Int {
        var i = strStart

        // fast path: ASCII only symbols
        while (i < strEnd) {
            val c = str[i].code
            if (c > 0x007F) break
            writeByte(c.toByte())
            i++
        }

        return if (i == strEnd) (strEnd - strStart) else ((i - strStart) + writeSlow(writeByte, str, i, strEnd))
    }
}

