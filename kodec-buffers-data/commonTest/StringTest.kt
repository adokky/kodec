package io.kodec.buffers

import io.kodec.StringsDataSet
import io.kodec.StringsTestFailHandler
import io.kodec.StringsUTF8
import kotlin.test.Test
import kotlin.test.fail

open class StringTest: AbstractDataBufferTest() {
    @Test
    fun ascii() {
        test(
            StringsDataSet::getAsciiData,
            { pos ->
                val length = getInt32(pos)
                getStringAscii(pos + 4, length = length)
            },
            // cut off length (1 byte), which should be less than 128 for all data
            { pos, s ->
                putInt32(pos, s.length)
                putStringAscii(pos + 4, s) + 4
            },
            onFail = StringsTestFailHandler()
        )
    }

    @Test
    fun utf8() {
        test(
            { StringsDataSet.getUtfData().take(100_000) },
            { pos ->
                val length = getInt32(pos)
                getStringUtf8(pos + 4, length = length)
            },
            // cut off length (1 byte), which should be less than 128 for all data
            { pos, s ->
                putInt32(pos, s.length)
                putStringUtf8(pos + 4, s) + 4
            },
            onFail = StringsTestFailHandler()
        )
    }

    @Test
    fun utf8_until_end() {
        test(
            { StringsDataSet.getUtfData().take(100_000) },
            { offset ->
                val byteLength = getInt32(offset)
                if (byteLength < 0) fail("invalid string length: $byteLength")

                val subBufferEnd = offset + byteLength + 4
                val s1 = subBuffer(offset,     subBufferEnd).getStringUtf8UntilEnd(4)
                val s2 = subBuffer(offset + 4, subBufferEnd).getStringUtf8UntilEnd(0)
                if (s1 != s2) fail("problem with 'subBuffer':" +
                        "\n  subBuffer(offset,     offset + byteLength + 4).getStringUtf8UntilEnd(4) -> '$s1'" +
                        "\n  subBuffer(offset + 4, offset + byteLength + 4).getStringUtf8UntilEnd(0) -> '$s2'")
                s1
            },
            // cut off length (1 byte), which should be less than 128 for all data
            { offset, s ->
                putInt32(offset, StringsUTF8.getByteLength(s))
                putStringUtf8(offset + 4, s) + 4
            },
            onFail = StringsTestFailHandler(actualStringStart = 4)
        )
    }
}