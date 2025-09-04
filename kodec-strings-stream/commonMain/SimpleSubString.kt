package io.kodec.text

import io.kodec.StringHashCode
import io.kodec.toDouble
import io.kodec.toFloat
import karamel.utils.assertionsEnabled
import dev.dokky.pool.use
import kotlin.jvm.JvmStatic

@Suppress("EqualsOrHashCode")
class SimpleSubString internal constructor(
    source: CharSequence,
    start: Int,
    end: Int,
    hashCode: Int = 0
): AbstractMutableSubString(hashCode) {
    var source: CharSequence = source
        private set
    override var start: Int = start
        private set
    override var end: Int = end
        private set

    override fun clear() {
        set("", 0, 0)
    }

    fun set(source: CharSequence, start: Int = 0, end: Int = source.length, hashCode: Int = 0) {
        resetCache(hashCode)

        this.source = source
        this.start = start
        this.end = end

        if (assertionsEnabled) validateRange()
    }

    internal fun validateRange() {
        require(start in 0 .. end && end <= source.length) {
            "Invalid range: $start..<$end. Source length: ${source.length}"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is AbstractSubString) return false
        if (fastNonEqualityCheck(other)) return false

        return when (other) {
            is SimpleSubString -> fastEquals(other)
            is RandomAccessTextReaderSubString -> equals(other)
            else -> toString() == other.toString()
        }
    }

    private fun equals(other: RandomAccessTextReaderSubString): Boolean =
        StringTextReader.threadLocalPool().use { textReader ->
            textReader.startReadingFrom(source, start)
            other.equals(textReader, start)
        }

    private fun fastEquals(other: SimpleSubString): Boolean {
        if (other.sourceLength != sourceLength) return false
        val s1 = source
        val s2 = other.source
        val start1 = start
        val start2 = other.start
        for (i in 0 ..< sourceLength) {
            if (s1[start1 + i] != s2[start2 + i]) return false
        }
        return true
    }

    override fun computeHashCode(): Int {
        var hash = StringHashCode.init()
        for (i in start ..< end) {
            hash = StringHashCode.next(hash, source[i])
        }
        return hash
    }

    override fun toBoolean(): Boolean = source.readBooleanDefault(start, end)

    override fun toLong(): Long = StringTextReader.threadLocalPool().use { textReader ->
        textReader.startReadingFrom(source, start)
        val result = textReader.readLong()
        if (textReader.position < end) throw NumberFormatException()
        result
    }

    override fun toFloat(): Float = source.toFloat(start, end)

    override fun toDouble(): Double = source.toDouble(start, end)

    override fun toChar(): Char {
        require(sourceLength == 1) { "'$this' can not be converted to Char" }
        return source[start]
    }

    override fun asString(): String = source.substring(start, end)

    override fun copy(): SimpleSubString = SimpleSubString(source, start, end)

    companion object {
        @JvmStatic
        operator fun invoke(): SimpleSubString = empty()

        @JvmStatic
        fun empty(): SimpleSubString = SimpleSubString("", 0, 0)
    }
}

fun String.substringWrapper(start: Int = 0, end: Int = length): SimpleSubString =
    SimpleSubString(this, start, end).also {
        it.validateRange()
    }