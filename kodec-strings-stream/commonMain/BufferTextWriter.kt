package io.kodec.text

import io.kodec.NumberToString
import io.kodec.StringsUTF8
import io.kodec.buffers.ArrayBuffer
import io.kodec.buffers.MutableBuffer
import io.kodec.buffers.MutableDataBuffer

class BufferTextWriter(output: MutableBuffer): TextWriter {
    constructor(): this(ArrayBuffer.Empty)

    var output: MutableBuffer = output
        private set
    var position: Int = 0

    fun set(output: MutableBuffer, position: Int = 0) {
        this.position = position
        this.output = output
    }

    override fun append(value: Char): BufferTextWriter {
        StringsUTF8.write(writeByte = { output[position++] = it }, value)
        return this
    }

    override fun append(value: CharSequence?): BufferTextWriter = append(value, 0, value?.length ?: 0)

    override fun append(
        value: CharSequence?,
        startIndex: Int,
        endIndex: Int
    ): BufferTextWriter {
        if (value == null) return append("null")

        if (output is MutableDataBuffer) {
            position += (output as MutableDataBuffer).putStringUtf8(position, value, startIndex, endIndex)
            return this
        }

        return appendStringDefault(value, startIndex, endIndex)
    }

    private fun appendStringDefault(
        value: CharSequence,
        startIndex: Int,
        endIndex: Int
    ): BufferTextWriter {
        var index = position // use local for performance
        position += StringsUTF8.write(value, startIndex, endIndex) { output[index++] = it }
        return this
    }

    override fun append(v: Byte) {
        append(v.toInt())
    }

    override fun append(v: Short) {
        append(v.toInt())
    }

    override fun append(v: Int) {
        position += NumberToString.putDigits(v, output, position)
    }

    override fun append(v: Long) {
        position += NumberToString.putDigits(v, output, position)
    }

    override fun append(v: Float) {
        position += NumberToString.putDigits(v, output, position)
    }

    override fun append(v: Double) {
        position += NumberToString.putDigits(v, output, position)
    }

    override fun append(v: Boolean) {
        append(if (v) "true" else "false")
    }
}