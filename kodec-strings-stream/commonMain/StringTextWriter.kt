package io.kodec.text

class StringTextWriter(var output: StringBuilder = StringBuilder()): TextWriter, Appendable {
    override fun append(v: Byte) { output.append(v) }

    override fun append(v: Short) { output.append(v) }

    override fun append(v: Int) { output.append(v) }

    override fun append(v: Long) { output.append(v) }

    override fun append(v: Float) { output.append(v) }

    override fun append(v: Double) { output.append(v) }

    override fun append(v: Boolean) { output.append(v) }

    override fun toString(): String = output.toString()

    override fun append(value: Char): StringTextWriter { output.append(value); return this }

    override fun append(value: CharSequence?): StringTextWriter { output.append(value); return this }

    override fun append(
        value: CharSequence?,
        startIndex: Int,
        endIndex: Int
    ): StringTextWriter {
        output.append(value, startIndex, endIndex)
        return this
    }
}