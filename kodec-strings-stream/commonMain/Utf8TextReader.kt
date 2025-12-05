package io.kodec.text

import dev.dokky.pool.use
import io.kodec.*
import io.kodec.StringsUTF8.is_header_2_bytes
import io.kodec.StringsUTF8.is_header_3_bytes
import io.kodec.StringsUTF8.is_header_4_bytes
import io.kodec.buffers.Buffer
import io.kodec.buffers.asArrayBuffer
import kotlin.jvm.JvmStatic

open class Utf8TextReader(buffer: Buffer = Buffer.Empty): RandomAccessTextReader() {
    var buffer: Buffer = buffer
        private set

    override fun close() {
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

    override fun readCodePoint(position: Int): CodePointAndSize {
        val buffer = buffer

        if (position >= buffer.size) return CodePointAndSize.EOF

        val firstByte = buffer[position]
        return when {
            firstByte < 128 -> CodePointAndSize(firstByte, 1)
            else -> readCodePointByte2(position + 1, firstByte)
        }
    }

    private fun readCodePointByte2(pos: Int, firstByte: Int): CodePointAndSize = when {
        !firstByte.is_header_2_bytes() -> readCodePointByte3(pos, firstByte)
        pos >= buffer.size -> CodePointAndSize.INVALID
        else -> CodePointAndSize(StringsUTF8.codePoint(firstByte, buffer[pos]), 2)
    }

    private fun readCodePointByte3(pos: Int, firstByte: Int): CodePointAndSize = when {
        !firstByte.is_header_3_bytes() -> readCodePointByte4(pos, firstByte)
        pos + 1 >= buffer.size -> CodePointAndSize.INVALID
        else -> CodePointAndSize(StringsUTF8.codePoint(firstByte, buffer[pos], buffer[pos + 1]), 3)
    }

    private fun readCodePointByte4(pos: Int, firstByte: Int): CodePointAndSize = when {
        firstByte.is_header_4_bytes() && pos + 2 < buffer.size ->
            CodePointAndSize(StringsUTF8.codePoint(firstByte, buffer[pos], buffer[pos + 1], buffer[pos + 2]), 4)
        else -> CodePointAndSize.INVALID
    }

    override fun readNextCodePoint(): Int {
        val buffer = buffer

        val pos = nextPosition
        if (pos >= buffer.size) return -1

        val firstByte = buffer[pos]
        nextPosition = pos + 1

        return if (firstByte < 128) firstByte else readCodePointByte2(firstByte)
    }

    private fun readCodePointByte2(firstByte: Int): Int {
        if (!firstByte.is_header_2_bytes()) return readCodePointByte3(firstByte)

        val pos = nextPosition
        if (pos >= buffer.size) return StringsASCII.INVALID_BYTE_PLACEHOLDER.code
        nextPosition = pos + 1
        return StringsUTF8.codePoint(firstByte, buffer[pos])
    }

    private fun readCodePointByte3(firstByte: Int): Int {
        if (!firstByte.is_header_3_bytes()) return readCodePointByte4(firstByte)

        val pos = nextPosition
        if (pos + 1 >= buffer.size) {
            nextPosition = buffer.size
            return StringsASCII.INVALID_BYTE_PLACEHOLDER.code
        }

        nextPosition = pos + 2
        return StringsUTF8.codePoint(firstByte, buffer[pos], buffer[pos + 1])
    }

    private fun readCodePointByte4(firstByte: Int): Int {
        if (firstByte.is_header_4_bytes()) {
            val pos = nextPosition
            if (pos + 2 < buffer.size) {
                nextPosition = pos + 3
                return StringsUTF8.codePoint(firstByte, buffer[pos], buffer[pos + 1], buffer[pos + 2])
            } else {
                nextPosition = buffer.size
            }
        }

        return StringsASCII.INVALID_BYTE_PLACEHOLDER.code
    }

    override fun parseFloat(start: Int, end: Int, onFormatError: DecodingErrorHandler<String>): StringToFpConverter =
        buffer.parseFloat(start, end, onFormatError)

    fun startReadingFrom(input: Buffer, position: Int = 0) {
        errorContainer.consumeError()
        this.buffer = input
        this.position = position
    }

    companion object: RandomAccessReaderCompanion<Utf8TextReader, Buffer>() {
        @JvmStatic
        override fun startReadingFrom(input: Buffer, position: Int): Utf8TextReader {
            val s = Utf8TextReader(input)
            s.position = position
            return s
        }

        override fun allocate() = Utf8TextReader()

        internal val Empty: Utf8TextReader = startReadingFrom(Buffer.Empty)

        @JvmStatic
        inline fun <R> useThreadLocal(source: Buffer, start: Int = 0, body: () -> R): R {
            return threadLocalPool().use { reader ->
                reader.startReadingFrom(source, start)
                body()
            }
        }
    }
}

fun Buffer.asUtf8Substring(): AbstractSubString {
    return Utf8TextReader.startReadingFrom(this).substring(0)
}

fun String.asUtf8SubString(): AbstractSubString =
    encodeToByteArray().asArrayBuffer().asUtf8Substring()