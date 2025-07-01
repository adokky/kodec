package io.kodec.text

class StringTextReaderTest: AbstractTextReaderTest() {
    override val reader: StringTextReader = StringTextReader.startReadingFrom("")

    override fun setText(text: String) {
        reader.startReadingFrom(text)
    }
}