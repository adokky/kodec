package io.kodec.text

import io.kodec.*
import karamel.utils.ThreadLocal
import dev.dokky.pool.AbstractObjectPool
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

open class StringTextReader(input: CharSequence = ""): RandomAccessTextReader() {
    var input: CharSequence = input
        private set

    override fun resetInput() {
        errorContainer.consumeError()
        input = ""
    }

    override fun readNextCodePoint(): Int {
        var pos = nextPosition
        if (pos >= input.length) return -1

        val c1 = input[pos++]
        var code = c1.code

        if (c1.isHighSurrogate()) {
            if (pos >= input.length) {
                nextPosition = pos
                return StringsASCII.INVALID_BYTE_PLACEHOLDER.code
            }
            code = StringsUTF16.codePoint(code, input[pos and 0x7f_ff_ff_ff].code)
            pos++
        }

        nextPosition = pos
        return code
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

    override fun substring(start: Int, end: Int): AbstractSubString =
        SimpleSubString(input, start, end).also { it.validateRange() }

    override fun parseFloat(start: Int, end: Int, onFormatError: DecodingErrorHandler<String>): ASCIIToBinaryConverter =
        FloatingDecimalParsing.readString(input, start, end, onFormatError = onFormatError)

    fun startReadingFrom(input: CharSequence, position: Int = 0) {
        this.input = input
        this.position = position
    }

    companion object {
        @JvmStatic
        fun startReadingFrom(input: CharSequence, position: Int = 0): StringTextReader {
            val s = StringTextReader(input)
            s.position = position
            return s
        }

        @JvmField
        val Empty: StringTextReader = startReadingFrom("")

        private val threadLocal = ThreadLocal {
            object : AbstractObjectPool<StringTextReader>(1..4) {
                override fun allocate() = startReadingFrom("")
                override fun beforeRelease(value: StringTextReader) { value.startReadingFrom("") }
            }
        }
        @JvmStatic
        fun threadLocalPool(): AbstractObjectPool<StringTextReader> = threadLocal.get()
    }
}