package io.kodec.text

import io.kodec.StringHashCode
import io.kodec.StringsUTF16
import io.kodec.buffers.asArrayBuffer

/**
 * @param reader the text reader from which the substring is mapped
 * @param start the starting position of the substring (inclusive)
 * @param end the ending position of the substring (exclusive)
 * @param codePoints the number of code points in the substring
 * @param hashCode [String.hashCode] of the equivalent substring,
 * or 0 to compute it lazily.
 * The actual hash code of this [TextReaderSubString] is always [String.hashCode] + 1.
 */
@Suppress("EqualsOrHashCode")
class TextReaderSubString(
    reader: RandomAccessTextReader,
    start: Int,
    end: Int,
    codePoints: Int,
    hashCode: Int = 0
) : AbstractSubString(hashCode) {
    constructor(): this(StringTextReader.Empty, 0, 0, 0)

    var reader: RandomAccessTextReader = reader
        private set
    override var start: Int = start
        private set
    override var end: Int = end
        private set
    var codePoints: Int = codePoints
        private set

    /**
     * @param hashCode [String.hashCode] of the equivalent substring, or 0 to compute it lazily.
     */
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

    /**
     * Same as [set] but without range checks.
     *
     * @param hashCode [String.hashCode] of the equivalent substring, or 0 to compute it lazily.
     */
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

    override fun close() {
        setUnchecked(StringTextReader.Empty, 0, 0, 0)
    }

    override fun toLong(): Long = readToken { readLong() }

    override fun toFloat(): Float = readToken { readFloat() }

    override fun toDouble(): Double = readToken { readDouble() }

    override fun toBoolean(): Boolean = readToken { readBoolean() }

    private inline fun <R> readToken(read: RandomAccessTextReader.() -> R): R =
        reader.useThreadLocalSubReader(start) {
            read().also { expectEof() }
        }

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
        cachedString?.let { return it.hashCode() + 1 }

        var hash = StringHashCode.init()
        forEachHeavyInline { char ->
            hash = StringHashCode.next(hash, char)
        }
        return hash + 1
    }

    private fun validate() {
        require(start >= 0) { "invalid start=$start" }
        require(start <= end) { "start > end, $start=start, $end=end" }
        require(codePoints in 0..sourceLength) { "invalid codePoints=$codePoints" }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is AbstractSubString) return false
        if (notEqualHashCode(other)) return false

        if (other is TextReaderSubString) {
            if (codePoints != other.codePoints) return false

            val rThis = reader
            val rOther = other.reader
            if (rThis is Utf8TextReader && rOther is Utf8TextReader) {
                return bufferRangesEquals(other, thisReader = rThis, otherReader = rOther)
            }
        }

        return contentStringEquals(other)
    }

    /** Either `this` or [other] MUST be [String]-based */
    internal fun contentStringEquals(other: AbstractSubString): Boolean {
        val cs1 = cachedString
        val cs2 = other.cachedString

        val ss2: TextReaderSubString?
        var s1: CharSequence? = null
        var s2: CharSequence? = null
        var start1 = 0
        var start2 = 0
        var end1 = 0
        var end2 = 0

        if (cs1 != null) {
            s1 = cs1
            end1 = cs1.length
        } else {
            val r = reader
            if (r is StringTextReader) {
                s1 = r.input
                start1 = start
                end1 = end
            }
        }

        if (cs2 != null) {
            s2 = cs2
            end2 = cs2.length
        } else if (other is TextReaderSubString) {
            val r = other.reader
            if (r is StringTextReader) {
                s2 = r.input
                start2 = other.start
                end2 = other.end
            }
        } else {
            other as SimpleSubString
            s2 = other.source
            start2 = other.start
            end2 = other.end
        }

        if (s1 == null) { // swap
            val t = s2!!
            s2 = s1
            s1 = t

            var ti = start1
            start1 = start2
            start2 = ti

            ti = end1
            end1 = end2
            end2 = ti

            ss2 = this
        } else {
            ss2 = other as? TextReaderSubString
        }

        if (s2 != null) return stringEquals(s1, start1, end1, s2, start2, end2)

        return ss2!!.contentEqualsSlow(s1, start1, end1)
    }

    private fun contentEqualsSlow(s2: CharSequence, s2Start: Int, s2End: Int): Boolean {
        var ss2Pos = s2Start

        reader.readCodePoints(start, end) { cp ->
            StringsUTF16.getChars(cp) { char ->
                if (ss2Pos >= s2End) return false
                if (s2[ss2Pos++] != char) return false
            }
        }

        return ss2Pos == s2End
    }

    private fun stringEquals(
        s1: CharSequence, start1: Int, end1: Int,
        s2: CharSequence, start2: Int, end2: Int,
    ): Boolean {
        val length = end1 - start1
        if (length != end2 - start2) return false

        for (i in 0 ..< length) {
            if (s1[start1 + i] != s2[start2 + i]) return false
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

    override fun asString(): String = when (val r = reader) {
        is StringTextReader -> r.input.substring(start, end)
        else -> super.asString()
    }

    override fun iterateChars(body: (Char) -> Unit) {
        when (val r = reader) {
            is StringTextReader -> {
                val s = r.input
                for (i in start..<end) {
                    body(s[i])
                }
            }
            else -> reader.readCodePoints(start, end) { cp ->
                StringsUTF16.getChars(cp) { char ->
                    body(char)
                }
            }
        }
    }

    internal inline fun forEachHeavyInline(body: (Char) -> Unit) {
        when(val reader = reader) {
            is StringTextReader -> {
                val input = reader.input
                for (i in start..<end) {
                    body(input[i])
                }
            }
            else -> reader.readCodePoints(start, end) { cp ->
                StringsUTF16.getCharsHeavyInline(cp) { char ->
                    body(char)
                }
            }
        }
    }
}