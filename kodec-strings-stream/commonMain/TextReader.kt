package io.kodec.text

import io.kodec.DecodingErrorHandler
import io.kodec.ErrorContainer
import io.kodec.StringsASCII
import karamel.utils.BitDescriptors
import karamel.utils.Bits32
import kotlin.jvm.JvmStatic

interface TextReader {
    /** UTF codepoint or -1 if reached end of stream */
    val nextCodePoint: Int

    /** @return UTF codepoint or -1 if reached end of stream */
    fun readCodePoint(): Int

    fun skipWhitespace() {
        while(nextCodePoint in 0..' '.code) readCodePoint()
    }

    fun skipWhitespaceStrict() {
        while(DefaultCharClasses.isWhitespace(nextCodePoint)) readCodePoint()
    }

    fun nextIs(char: Char): Boolean = nextCodePoint == char.code

    fun trySkip(char: Char): Boolean {
        if (nextCodePoint != char.code) return false
        readCodePoint()
        return true
    }

    fun expectNextIs(b: Char) { expectNextIs(b.code) }

    fun expectNextIs(code: Int) {
        if (nextCodePoint != code) unexpectedChar(this, expected = code, actual = nextCodePoint)
    }

    fun expect(b: Char) {
        expectNextIs(b)
        readCodePoint()
    }

    fun expectEof() {
        expectNextIs(-1)
        readCodePoint()
    }

    fun <BDS: BitDescriptors> readByte(
        charMappings: CharToClassMapper<BDS>,
        terminatorClass: Bits32<BDS>,
        onFormatError: DecodingErrorHandler<IntegerParsingError> = fail
    ): Byte = readLongDefault(Byte.MIN_VALUE.toLong(), Byte.MAX_VALUE.toLong(), charMappings, terminatorClass, onFormatError)
        .toByte()

    fun <BDS: BitDescriptors> readShort(
        charMappings: CharToClassMapper<BDS>,
        terminatorClass: Bits32<BDS>,
        onFormatError: DecodingErrorHandler<IntegerParsingError> = fail
    ): Short = readLongDefault(Short.MIN_VALUE.toLong(), Short.MAX_VALUE.toLong(), charMappings, terminatorClass, onFormatError)
        .toShort()

    fun <BDS: BitDescriptors> readInt(
        charMappings: CharToClassMapper<BDS>,
        terminatorClass: Bits32<BDS>,
        onFormatError: DecodingErrorHandler<IntegerParsingError> = fail
    ): Int = readLongDefault(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong(), charMappings, terminatorClass, onFormatError)
        .toInt()

    fun <BDS: BitDescriptors> readLong(
        charMappings: CharToClassMapper<BDS>,
        terminatorClass: Bits32<BDS>,
        onFormatError: DecodingErrorHandler<IntegerParsingError> = fail
    ): Long = readLongDefault(charMappings, terminatorClass, onFormatError = onFormatError)

    fun readFloat(allowSpecialValues: Boolean = false, onFormatError: DecodingErrorHandler<String> = fail): Float

    fun readDouble(allowSpecialValues: Boolean = false, onFormatError: DecodingErrorHandler<String> = fail): Double

    fun readBoolean(): Boolean = readBooleanDefault()

    fun readStringSizedInto(length: Int, output: StringBuilder) {
        if (length > 0) {
            var l = 0
            readCharsHeavyInline { c ->
                if (l++ < length) { output.append(c); true } else false
            }
        }
    }

    fun readStringSized(length: Int, builder: StringBuilder = StringBuilder(length)): String {
        val start = builder.length
        readStringSizedInto(length, builder)
        return builder.substring(start).also { builder.setLength(start) }
    }

    val fail: DecodingErrorHandler<Any>

    fun fail(msg: String): Nothing = throw TextDecodingException(msg, position = positionOrNegative())

    /**
     * Temporary storage for error messages to avoid allocations. Free for use by everyone.
     */
    val errorContainer: ErrorContainer<Any>

    companion object {
        @JvmStatic
        fun charCodeToReadableString(code: Int): String = when (code) {
            -1 -> "EOF"
            in Char.MIN_VALUE.code..Char.MAX_VALUE.code -> Char(code).toString()
            else -> StringsASCII.INVALID_BYTE_PLACEHOLDER.toString()
        }

        internal fun unexpectedChar(reader: TextReader, expected: Int, actual: Int): Nothing {
            val got: String = charCodeToReadableString(actual)
            val expected: String = charCodeToReadableString(expected)
            reader.fail("expected '$expected', got: '$got' ($actual)")
        }
    }
}

fun TextReader.readByte(onFormatError: DecodingErrorHandler<IntegerParsingError> = fail): Byte =
    readByte(DefaultCharClasses.mapper, terminatorClass = DefaultCharClasses.WORD_TERM, onFormatError = onFormatError)

fun TextReader.readShort(onFormatError: DecodingErrorHandler<IntegerParsingError> = fail): Short =
    readShort(DefaultCharClasses.mapper, terminatorClass = DefaultCharClasses.WORD_TERM, onFormatError = onFormatError)

fun TextReader.readInt(onFormatError: DecodingErrorHandler<IntegerParsingError> = fail): Int =
    readInt(DefaultCharClasses.mapper, terminatorClass = DefaultCharClasses.WORD_TERM, onFormatError = onFormatError)

fun TextReader.readLong(onFormatError: DecodingErrorHandler<IntegerParsingError> = fail): Long =
    readLong(DefaultCharClasses.mapper, terminatorClass = DefaultCharClasses.WORD_TERM, onFormatError = onFormatError)

internal fun TextReader.positionOrNegative(): Int = if (this is RandomAccessTextReader) position else -1

private fun  <BDS: BitDescriptors> TextReader.readLongDefault(
    min: Long,
    max: Long,
    charMappings: CharToClassMapper<BDS>,
    terminatorClass: Bits32<BDS>,
    onFormatError: DecodingErrorHandler<IntegerParsingError>
): Long {
    val long = readLongDefault(charMappings, terminatorClass, onFormatError = onFormatError)

    if (long !in min..max) {
        onFormatError(IntegerParsingError.Overflow)
        return 0
    }

    return long
}