package io.kodec.text

import io.kodec.*

/*
 * WARN: Derived classes should override default initialization
 * logic via init/factory to initialize [position] before usage.
 */
sealed class RandomAccessTextReader: TextReader {
    final override val nextCodePoint: Int get() = _nextCodePoint
    private var _nextCodePoint = -1

    var nextPosition: Int = 0
        protected set

    private var _position = nextPosition - 1
    var position: Int
        get() = _position
        set(value) {
            // WARN: this line breaks TextReader re-initialization
            //  on input change and (probably) ASCII "fixing" re-read
            //
            // if (_position == value) return

            _position = value
            this.nextPosition = value
            _nextCodePoint = readNextCodePoint()
        }

    protected abstract fun readNextCodePoint(): Int
    protected abstract fun readNextAsciiCode(): Int

    abstract fun parseFloat(start: Int, end: Int, onFormatError: DecodingErrorHandler<String> = fail): ASCIIToBinaryConverter

    /**
     * Unsafe and performant function assuming next code point is in ASCII range.
     *
     * If next code point is *not* in ASCII range then
     * result will be *any* number *not* in ASCII range (even negative).
     *
     * [fixNextCodePoint] can be used to re-read current code point with normal [readCodePoint].
     */
    abstract fun readAsciiCode(position: Int): Int

    /**
     * Intended to use with [readAsciiCode] on mixed (ASCII / non-ASCII) sequences.
     */
    fun fixNextCodePoint() {
        if (nextCodePoint and 0x7f.inv() != 0) {
            position = _position
        }
    }

    open fun readBoolean(position: Int): Boolean = readAtPosition(position, RandomAccessTextReader::readBoolean)
    open fun readByte(position: Int): Byte       = readAtPosition(position, RandomAccessTextReader::readByte)
    open fun readShort(position: Int): Short     = readAtPosition(position, RandomAccessTextReader::readShort)
    open fun readInt(position: Int): Int         = readAtPosition(position, RandomAccessTextReader::readInt)
    open fun readLong(position: Int): Long       = readAtPosition(position, RandomAccessTextReader::readLong)
    open fun readFloat(position: Int): Float     = readAtPosition(position, RandomAccessTextReader::readFloat)
    open fun readDouble(position: Int): Double   = readAtPosition(position, RandomAccessTextReader::readDouble)

    open fun readStringSized(start: Int, length: Int): String = readAtPosition(start) {
        readStringSized(length)
    }

    fun readStringCodePointSizedInto(
        start: Int,
        codePoints: Int,
        output: StringBuilder
    ): Int = readAtPosition(start) {
        readCharsInline(codePoints, output::append)
    }

    fun readStringCodePointSized(
        start: Int,
        codePoints: Int,
        stringBuilder: StringBuilder = StringBuilder(codePoints)
    ): String {
        val sbStart = stringBuilder.length
        readStringCodePointSizedInto(start, codePoints, stringBuilder)
        return stringBuilder.substring(sbStart)
    }

    open fun substring(start: Int, end: Int = Int.MAX_VALUE): AbstractSubString = readAtPosition(start) {
        var codePoints = 0
        var hash = StringHashCode.init()
        while (position < end) {
            val cp = readCodePoint()
            if (cp < 0) {
                if (end == Int.MAX_VALUE) break
                throw IndexOutOfBoundsException()
            }
            StringsUTF16.getChars(cp) { hash = StringHashCode.next(hash, it) }
            codePoints++
        }
        RandomAccessTextReaderSubString(
            reader = this,
            start = start,
            end = position,
            codePoints = codePoints,
            hashCode = hash
        )
    }

    final override fun readCodePoint(): Int {
        _position = nextPosition
        return nextCodePoint.also { _nextCodePoint = readNextCodePoint() }
    }

    override fun readAsciiCode(): Int {
        _position = nextPosition
        return nextCodePoint.also { _nextCodePoint = readNextAsciiCode() }
    }

    fun parseFloat(
        allowSpecialValues: Boolean = false,
        onFormatError: DecodingErrorHandler<String> = fail
    ): ASCIIToBinaryConverter {
        val start = position

        if (allowSpecialValues) {
            tryReadSpecialFpValue()?.let { return it }
            position = start
        }

        while (DefaultCharClasses.isFloatLiteral(nextCodePoint)) readAsciiCode()
        fixNextCodePoint()

        return parseFloat(start, end = position, onFormatError)
    }

    private fun tryReadSpecialFpValue(): ASCIIToBinaryConverter? {
        val negative = trySkip('-')

        return when(nextCodePoint or StringsASCII.LOWER_CASE_BIT) {
            'i'.code -> readInfinity(negative)
            'n'.code -> if (negative) null else readNaN()
            else -> null
        }
    }

    private fun readInfinity(negative: Boolean): ASCIIToBinaryConverter? {
        readAsciiCode()

        for (c in "nfinity") {
            if (c.code != readAsciiCode()) {
                fixNextCodePoint()
                return null
            }
        }

        return if (negative) ASCIIToBinaryConverter.NegativeInfinity else ASCIIToBinaryConverter.PositiveInfinity
    }

    private fun readNaN(): ASCIIToBinaryConverter? {
        readAsciiCode()

        if (readAsciiCode() == 'a'.code &&
            readAsciiCode() == 'n'.code)
            return ASCIIToBinaryConverter.NaN

        fixNextCodePoint()
        return null
    }

    override fun readFloat(allowSpecialValues: Boolean, onFormatError: DecodingErrorHandler<String>): Float =
        parseFloat(allowSpecialValues = allowSpecialValues, onFormatError = onFormatError).floatValue()

    override fun readDouble(allowSpecialValues: Boolean, onFormatError: DecodingErrorHandler<String>): Double =
        parseFloat(allowSpecialValues = allowSpecialValues, onFormatError = onFormatError).doubleValue()

    abstract fun resetInput()

    final override val fail: DecodingErrorHandler<Any> = DecodingErrorHandler { err ->
        fail(
            (err as? Throwable)?.message ?:
            (err as? DecodingErrorWithMessage)?.message ?:
            err.toString()
        )
    }

    final override val errorContainer: ErrorMessageContainer = ErrorMessageContainer()
}

