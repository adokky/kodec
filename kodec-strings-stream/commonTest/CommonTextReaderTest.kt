package io.kodec.text

import io.kodec.StringsDataSet
import io.kodec.buffers.asDataBuffer
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class CommonTextReaderTest {
    private val seed = 400

    private val bufferReader = Utf8TextReader()
    private val stringReader = StringTextReader()

    @Test
    fun read_whole_string() {
        for (s in StringsDataSet.getUtfData(random = Random(400))) {
            bufferReader.startReadingFrom(s.encodeToByteArray().asDataBuffer())
            assertEquals(s, bufferReader.readWholeString())

            stringReader.startReadingFrom(s)
            assertEquals(s, stringReader.readWholeString())
        }
    }
}