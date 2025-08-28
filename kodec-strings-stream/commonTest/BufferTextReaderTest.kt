package io.kodec.text

import io.kodec.StringsASCII
import io.kodec.StringsDataSet
import io.kodec.buffers.ArrayDataBuffer
import io.kodec.buffers.asDataBuffer
import karamel.utils.Bits32
import karamel.utils.asInt
import karamel.utils.enrichMessageOf
import karamel.utils.nearlyEquals
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BufferTextReaderTest: AbstractTextReaderTest() {
    private val random = Random(300)

    private val buffer = ArrayDataBuffer(0)
    override val reader: Utf8TextReader = Utf8TextReader.startReadingFrom(buffer)

    private val INVALID_BYTE = StringsASCII.INVALID_BYTE_PLACEHOLDER.code

    override fun setText(text: String) {
        buffer.setArray(text.encodeToByteArray())
        reader.startReadingFrom(buffer)
    }

    @Test
    fun float_eof_handling() {
        setText("1.1;")
        assertTrue(reader.readFloat().nearlyEquals(1.1f))
        reader.position = 0
        assertTrue(reader.readDouble().nearlyEquals(1.1))
    }

    @Test
    fun reading_utf8_sequence_with_first_byte_removed() {
        for (surrogatePair in StringsDataSet.getSurrogatePairs(10, random = random)) {
            // surrogate pair encoded into four UTF-8 bytes
            // remove first byte, making the whole byte sequence invalid
            val bytes = surrogatePair.encodeToByteArray().let { it.copyOfRange(1, it.size) }
            reader.startReadingFrom(bytes.asDataBuffer())

            assertEquals(INVALID_BYTE, reader.readCodePoint())
            assertEquals(INVALID_BYTE, reader.readCodePoint())
            assertEquals(INVALID_BYTE, reader.readCodePoint())
            reader.assertEof()

            reader.position = bytes.lastIndex
            assertEquals(INVALID_BYTE, reader.readCodePoint())
            reader.assertEof()

            reader.position = bytes.size
            reader.assertEof()
        }
    }

    @Test
    fun reading_utf8_sequence_with_last_bytes_removed() {
        for (surrogatePair in StringsDataSet.getSurrogatePairs(10, random = random)) {
            for (bytesRemoved in 1..3) {
                // surrogate pair encoded into four UTF-8 bytes
                // remove last N bytes and always keeping first byte
                val bytes = surrogatePair.encodeToByteArray().let {
                    it.copyOfRange(0, it.size - bytesRemoved)
                }
                reader.startReadingFrom(bytes.asDataBuffer())

                assertEquals(INVALID_BYTE, reader.readCodePoint())
                reader.assertEof()

                reader.position = bytes.lastIndex
                assertEquals(INVALID_BYTE, reader.readCodePoint())
                reader.assertEof()

                reader.position = bytes.size
                reader.assertEof()
            }
        }
    }

    @Test
    fun substring_by_indices_range() {
        for (s in StringsDataSet.getUtfData(random = random)) {
            val binary = s.encodeToByteArray()
            enrichMessageOf<Throwable>({
                "failed on '$s', binary: ${binary.map { Bits32<Unit>(it.asInt()) }}"
            }) {
                reader.startReadingFrom(binary.asDataBuffer())

                // substring() can not guarantee correct decoding if we do not start from utf8 codepoint
                var start = 0
                if (binary.isNotEmpty()) {
                    var i = 0
                    do {
                        start = Random.nextInt(binary.size)
                        if (i++ > 10_000) return@enrichMessageOf
                    } while (!binary[start].isFirstUtf8CodePointByte())
                }

                var end = start
                if (start < binary.size) {
                    var i = 0
                    do {
                        end = Random.nextInt(start, binary.size + 1)
                        if (i++ > 10_000) return@enrichMessageOf
                    } while (end >= binary.size || !binary[end].isFirstUtf8CodePointByte())
                }

                assertEquals(
                    binary.copyOfRange(start, binary.size).decodeToString(),
                    reader.substring(start).toString(),
                    "substring from $start"
                )

                assertEquals(
                    binary.copyOfRange(start, end).decodeToString(),
                    reader.substring(start, end).toString(),
                    "substring from $start to $end"
                )
            }
        }
    }

    private fun Byte.isFirstUtf8CodePointByte(): Boolean = this >= 0 || (this.asInt() >= 0b1100_0000)

    private fun TextReader.assertEof() {
        assertPositionAtEof()
        assertEquals(-1, nextCodePoint)
        assertEquals(-1, readCodePoint())
        assertEquals(-1, readCodePoint())
        assertPositionAtEof()
    }

    private fun TextReader.assertPositionAtEof() {
        when(this) {
            is Utf8TextReader -> assertEquals(buffer.size, nextPosition)
            is StringTextReader -> assertEquals(input.length, nextPosition)
        }
    }
}