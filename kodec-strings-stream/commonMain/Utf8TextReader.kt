package io.kodec.text

import io.kodec.*
import io.kodec.buffers.Buffer
import io.kodec.buffers.asArrayBuffer
import kotlin.jvm.JvmStatic

open class Utf8TextReader(buffer: Buffer = Buffer.Empty): RandomAccessTextReader() {
    var buffer: Buffer = buffer
        private set

    override fun resetInput() {
        buffer = Buffer.Empty
    }

    override fun readNextAsciiCode(): Int {
        val buffer = buffer
        val pos = nextPosition
        return if (pos >= buffer.size) -1 else {
            nextPosition = pos + 1
            buffer[pos]
        }
    }

    override fun readAsciiCode(position: Int): Int =
        if (position >= buffer.size) -1 else buffer[position]

    override fun readNextCodePoint(): Int {
        val buffer = buffer

        val pos = nextPosition
        if (pos >= buffer.size) return -1

        val firstByte = buffer[pos]
        nextPosition = pos + 1

        if (firstByte < 128) return firstByte

        return readCodePointSlow(firstByte)
    }

    private fun readCodePointSlow(firstByte: Int): Int {
        return StringsUTF8.readCodePoint(firstByte) {
            val pos = nextPosition
            if (pos >= buffer.size) return StringsASCII.INVALID_BYTE_PLACEHOLDER.code
            nextPosition = pos + 1
            buffer[pos]
        }
    }

    override fun parseFloat(start: Int, end: Int, onFormatError: DecodingErrorHandler<String>): ASCIIToBinaryConverter =
        buffer.parseFloat(start, end, onFormatError)

    fun startReadingFrom(input: Buffer, position: Int = 0) {
        errorContainer.consumeError()
        this.buffer = input
        this.position = position
    }

    companion object {
        @JvmStatic
        fun startReadingFrom(input: Buffer, position: Int = 0): Utf8TextReader {
            val s = Utf8TextReader(input)
            s.position = position
            return s
        }
    }
}

fun Buffer.asUtf8Substring(): AbstractSubString {
    return Utf8TextReader.startReadingFrom(this).substring(0)
}

fun String.asUtf8SubString(): AbstractSubString =
    encodeToByteArray().asArrayBuffer().asUtf8Substring()