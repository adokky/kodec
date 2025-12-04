package io.kodec.text

import io.kodec.StringHashCode
import io.kodec.StringsUTF16
import io.kodec.buffers.asArrayBuffer

@Suppress("EqualsOrHashCode")
class TextReaderSubString(
    reader: RandomAccessTextReader,
    start: Int,
    end: Int,
    codePoints: Int,
    hashCode: Int = 0
) : AbstractMutableSubString(hashCode) {
    constructor(): this(StringTextReader.Empty, 0, 0, 0, 0)

    var reader: RandomAccessTextReader = reader
        private set
    override var start: Int = start
        private set
    override var end: Int = end
        private set
    var codePoints: Int = codePoints
        private set

    fun set(
        reader: RandomAccessTextReader,
        start: Int,
        end: Int,
        codePoints: Int,
        hashCode: Int = 0
    ) {
        setUnchecked(
            reader = reader,
            start = start,
            end = end,
            codePoints = codePoints,
            hashCode = hashCode,
        )

        validate()
    }

    fun setUnchecked(
        reader: RandomAccessTextReader,
        start: Int,
        end: Int,
        codePoints: Int,
        hashCode: Int = 0
    ) {
        resetCache(hashCode)

        this.reader = reader
        this.start = start
        this.end = end
        this.codePoints = codePoints
    }

    override fun clear() {
        setUnchecked(StringTextReader.Empty, 0, 0, 0)
    }

    override fun toLong(): Long = reader.useThreadLocalSubReader(start) { readLong() }

    override fun toFloat(): Float = reader.useThreadLocalSubReader(start) { readFloat() }

    override fun toDouble(): Double = reader.useThreadLocalSubReader(start) { readDouble() }

    override fun toBoolean(): Boolean = reader.useThreadLocalSubReader(start) { readBoolean() }

    override fun toChar(): Char {
        reader.readCodePoints(start, end) { cp ->
            require(cp < StringsUTF16.MIN_SUPPLEMENTARY_CODE_POINT) {
                "'$this' can not be converted to Char"
            }
            return cp.toChar()
        }
        reader.fail("unexpected EOF")
    }

    override fun computeHashCode(): Int {
        var hash = StringHashCode.init()
        forEachHeavyInline { char ->
            hash = StringHashCode.next(hash, char)
        }
        return hash
    }

    private fun validate() {
        require(start >= 0)
        require(start <= end) { "start > end, $start=start, $end=end" }
        require(codePoints in 0..sourceLength) { "codePoints=$codePoints, " + debugDescription() }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is AbstractSubString) return false
        if (notEqualHashCode(other)) return false

        if (other is TextReaderSubString) {
            if (codePoints != other.codePoints) return false

            val thisReader = reader
            val otherReader = other.reader

            return if (thisReader is Utf8TextReader && otherReader is Utf8TextReader) {
                bufferRangesEquals(other, thisReader, otherReader)
            } else {
                contentEquals(other.reader, other.start)
            }
        }
        
        if (other is SimpleSubString) return contentEquals(this, other)

        return toString() == other.toString()
    }

    internal fun contentEquals(otherReader: RandomAccessTextReader, otherStart: Int): Boolean {
        var thisPos = this.start
        var otherPos = otherStart

        repeat(codePoints) {
            val thisCp = reader.readCodePoint(thisPos)
            val otherCp = otherReader.readCodePoint(otherPos)

            if (thisCp.codepoint != otherCp.codepoint) return false

            thisPos += thisCp.size
            otherPos += otherCp.size
        }

        return true
    }

    private fun bufferRangesEquals(
        other: TextReaderSubString,
        thisReader: Utf8TextReader,
        otherReader: Utf8TextReader
    ): Boolean {
        if (sourceLength != other.sourceLength) return false
        return thisReader.buffer.equalsRange(
            otherReader.buffer,
            thisOffset = start,
            otherOffset = other.start,
            size = sourceLength
        )
    }

    override fun copy(): AbstractSubString = when(val reader = reader) {
        is Utf8TextReader -> Utf8TextReader
            .startReadingFrom(reader.buffer.toByteArray(start, end).asArrayBuffer())
            .substring(0)
        is StringTextReader -> SimpleSubString(reader.input, start, end)
    }

    override fun asString(): String = buildString(codePoints) {
        this@TextReaderSubString.forEach { append(it) }
    }
}

internal inline fun TextReaderSubString.forEachHeavyInline(body: (Char) -> Unit) {
    reader.readCodePoints(start, end) { cp ->
        StringsUTF16.getCharsHeavyInline(cp) { char ->
            body(char)
        }
    }
}

inline fun TextReaderSubString.forEach(body: (Char) -> Unit) {
    reader.readCodePoints(start, end) { cp ->
        StringsUTF16.getChars(cp) { char ->
            body(char)
        }
    }
}