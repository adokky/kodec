package io.kodec.text

import io.kodec.buffers.asBuffer

abstract class TextReaderSubStringTest: AbstractSubStringTest<TextReaderSubString>() {
    override fun TextReaderSubString.resetData(
        source: String,
        start: Int,
        end: Int
    ) {
        val ss = substring(source, start, end)
        set(ss.reader, start = ss.start, end = ss.end, codePoints = ss.codePoints)
    }
}

class Utf8SubStringTest: TextReaderSubStringTest() {
    override fun substring(
        source: String,
        start: Int,
        end: Int
    ): TextReaderSubString {
        val encoded = source.encodeToByteArray().asBuffer()

        val startBytePos = source.substring(0, start).encodeToByteArray().size
        val endBytePos = encoded.size - source.substring(end).encodeToByteArray().size

        return Utf8TextReader.startReadingFrom(encoded).substringDefault(startBytePos, endBytePos)
    }
}

class StringReaderSubStringTest: TextReaderSubStringTest() {
    override fun substring(
        source: String,
        start: Int,
        end: Int
    ): TextReaderSubString {
        return StringTextReader.startReadingFrom(source).substringDefault(start, end)
    }
}