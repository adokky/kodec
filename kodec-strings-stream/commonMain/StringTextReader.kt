package io.kodec.text

import dev.dokky.pool.use
import io.kodec.*
import kotlin.jvm.JvmStatic

open class StringTextReader(input: CharSequence = ""): RandomAccessTextReader() {
    var input: CharSequence = input
        private set

    override fun close() {
        errorContainer.consumeError()
        input = ""
    }

    override fun readNextCodePoint(): Int {
        var pos = nextPosition
        if (pos >= input.length) return -1

        val c = input[pos++]

        if (c.isHighSurrogate()) return readSurrogatePair(high = c, lowPos = pos)

        nextPosition = pos
        return c.code
    }

    private fun readSurrogatePair(high: Char, lowPos: Int): Int {
        if (lowPos >= input.length) {
            nextPosition = input.length
            return StringsASCII.INVALID_BYTE_PLACEHOLDER.code
        }
        val code = StringsUTF16.codePoint(high.code, input[lowPos and 0x7f_ff_ff_ff].code)
        nextPosition = lowPos + 1
        return code
    }

    override fun readCodePoint(position: Int): CodePointAndSize {
        var pos = position
        if (pos >= input.length) return CodePointAndSize.EOF

        val c1 = input[pos++]
        var code = c1.code

        if (c1.isHighSurrogate()) {
            if (pos >= input.length) return CodePointAndSize.INVALID
            code = StringsUTF16.codePoint(code, input[pos and 0x7f_ff_ff_ff].code)
            pos++
        }

        return CodePointAndSize(code, size = pos - position)
    }

    override fun readNextAsciiCode(): Int {
        val pos = nextPosition
        return if (pos >= input.length) -1 else {
            nextPosition = pos + 1
            input[pos and 0x7f_ff_ff_ff].code
        }
    }

    override fun readAsciiCode(position: Int): Int {
        return if (position >= input.length) -1 else {
            input[position and 0x7f_ff_ff_ff].code
        }
    }

    override fun substring(start: Int): AbstractSubString =
        substring(start, input.length)

    override fun substring(start: Int, end: Int): AbstractSubString =
        SimpleSubString(input, start, end).also { it.validateRange() }

    override fun parseFloat(start: Int, end: Int, onFormatError: DecodingErrorHandler<String>): StringToFpConverter =
        FloatingDecimalParsing.readString(input, start, end, onFormatError = onFormatError)

    fun startReadingFrom(input: CharSequence, position: Int = 0) {
        this.input = input
        this.position = position
    }

    companion object: RandomAccessReaderCompanion<StringTextReader, CharSequence>() {
        @JvmStatic
        override fun startReadingFrom(input: CharSequence, position: Int): StringTextReader {
            val s = StringTextReader(input)
            s.position = position
            return s
        }

        override fun allocate() = StringTextReader()

        internal val Empty: StringTextReader = startReadingFrom("")

        @JvmStatic
        inline fun <R> useThreadLocal(source: CharSequence, start: Int = 0, body: () -> R): R {
            return threadLocalPool().use { reader ->
                reader.startReadingFrom(source, start)
                body()
            }
        }
    }
}