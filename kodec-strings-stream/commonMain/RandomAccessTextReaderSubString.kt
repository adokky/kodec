package io.kodec.text

import io.kodec.StringHashCode
import io.kodec.StringsUTF16
import io.kodec.buffers.asArrayBuffer
import karamel.utils.assertionsEnabled

@Suppress("EqualsOrHashCode")
class RandomAccessTextReaderSubString(
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
        resetCache(hashCode)

        this.reader = reader
        this.start = start
        this.end = end
        this.codePoints = codePoints

        if (assertionsEnabled) validate()
    }

    override fun clear() {
        set(StringTextReader.Empty, 0, 0, 0)
    }

    override fun toLong(): Long = reader.readLong(start)

    override fun toFloat(): Float = reader.readFloat(start)

    override fun toDouble(): Double = reader.readDouble(start)

    override fun toBoolean(): Boolean = reader.readBoolean(start)

    override fun toChar(): Char {
        val cp = reader.readCodePoint()
        if (cp < 0) reader.fail("unexpected EOF")
        require(cp < StringsUTF16.MIN_SUPPLEMENTARY_CODE_POINT) {
            "'$this' can not be converted to Char"
        }
        return cp.toChar()
    }

    override fun computeHashCode(): Int {
        var hash = StringHashCode.init()
        reader.readAtPosition(start) {
            readCharsInline(codePoints) { c ->
                hash = StringHashCode.next(hash, c)
            }
        }
        return hash
    }

    private fun validate() {
        require(start >= 0)
        require(start <= end) { "start > end, $start=start, $end=end" }
        require(codePoints in 0..sourceLength) { "codePoints=$codePoints, " + debugDescription() }
    }

    override fun equals(other: Any?): Boolean {
        if (other is RandomAccessTextReaderSubString) {
            // fast paths buffered case
            val thisReader = reader
            val otherReader = other.reader
            if (otherReader is Utf8TextReader && thisReader is Utf8TextReader) {
                return equals(other, thisReader, otherReader)
            }

            return equals(other)
        }

        if (other !is AbstractSubString) return false
        if (fastNonEqualityCheck(other)) return false

        return toString() == other.toString()
    }

    fun equals(other: RandomAccessTextReaderSubString): Boolean {
        if (codePoints != other.codePoints) return false
        return equals(other.reader, other.start)
    }

    fun equals(reader: RandomAccessTextReader, start: Int): Boolean {
        val thisInitPos = this.reader.position
        val otherInitPos = reader.position

        try {
            if (reader === this.reader) {
                if (notEqualsForSameReader(reader, start)) return false
            } else {
                if (notEquals(reader, start)) return false
            }
        } finally {
            this.reader.position = thisInitPos
            reader.position = otherInitPos
        }

        return true
    }

    private fun notEquals(reader: RandomAccessTextReader, start: Int): Boolean {
        this.reader.position = this.start
        reader.position = start

        repeat(codePoints) {
            if (this.reader.readCodePoint() != reader.readCodePoint()) {
                return true
            }
        }
        return false
    }

    private fun notEqualsForSameReader(reader: RandomAccessTextReader, start: Int): Boolean {
        repeat(codePoints) { i ->
            reader.position = this.start + i
            val cp1 = reader.readCodePoint()
            reader.position = start + i
            val cp2 = reader.readCodePoint()
            if (cp1 != cp2) return true
        }
        return false
    }

    private fun equals(
        other: RandomAccessTextReaderSubString,
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

    override fun asString(): String = reader.readStringCodePointSized(
        start = start,
        codePoints = codePoints
    )
}