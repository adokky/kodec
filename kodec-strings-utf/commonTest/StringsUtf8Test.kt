package io.kodec

import karamel.utils.asInt
import kotlin.test.Test
import kotlin.test.assertEquals

class StringsUtf8Test: StringsTestBase() {
    @Test
    fun fixtures() {
        fun test(expected: String) {
            val bytes = expected.encodeToByteArray()
            val got = buildString {
                var pos = 0
                StringsUTF8.readFromByteStream(
                    readByte = { if (pos < bytes.size) bytes[pos++].asInt() else -1 },
                    appendChar = { append(it) }
                )
            }
            assertEquals(expected, got)
        }

        test("")
        test("abc")
        test(" 䣖㿼")
    }

    @Test
    fun utf8_length() {
        for (data in listOf(StringsDataSet.getUtfData()))
            for (string in data) {
                val expected = string.encodeToByteArray().size
                assertEquals(expected, StringsUTF8.getByteLength(string), "getUtf8Length failed on '$string'")
                assertEquals(expected, string.sumOf { StringsUTF8.getByteLength(it) }, "getUtf8Length(c) failed on '$string'")
            }
    }

    @Test
    fun invalid_sequence_decoding() {
        val placeholder = "${StringsASCII.INVALID_BYTE_PLACEHOLDER}"

        val encodedValid =  StringsDataSet.singleSurrogatePair.encodeToByteArray()

        assertEquals(StringsDataSet.singleSurrogatePair, decode(encodedValid))
        assertEquals(StringsDataSet.singleSurrogatePair, decodeStreamed(encodedValid))

        for (size in 1..3) {
            val corruptedBytes = encodedValid.copyOf(size)
            assertEquals(placeholder, decode(corruptedBytes))
            assertEquals(placeholder, decodeStreamed(corruptedBytes))
        }
    }

    override fun readFromBytes(
        getByte: (Int) -> Int,
        endExclusive: Int,
        appendChar: (Char) -> Unit
    ): Int = StringsUTF8.readFromBytes(getByte, endExclusive, appendChar)

    override fun readFromByteStream(readByte: () -> Int, appendChar: (Char) -> Unit) =
        StringsUTF8.readFromByteStream(readByte, appendChar)

    override fun write(
        str: CharSequence,
        offset: Int,
        endExclusive: Int,
        writeByte: (Byte) -> Unit
    ): Int = StringsUTF8.write(str, offset, endExclusive, writeByte)

    override fun checkEncoded(expected: String, bytes: ByteArray) {
        val expectedBytes = expected.encodeToByteArray()
        if (!expectedBytes.contentEquals(expectedBytes)) error(
            "expected bytes: " + expectedBytes.contentToString() + "\n" +
            "actual   bytes: " + bytes.contentToString() + "\n" +
            "expected string: '" + expected + "'\n" +
            "actual   string: '" + bytes.decodeToString() + "'"
        )
    }
}

