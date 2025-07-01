package io.kodec.text

interface TextWriter: Appendable {
    fun append(v: Byte)

    fun append(v: Short)

    fun append(v: Int)

    fun append(v: Long)

    fun append(v: Float)

    fun append(v: Double)

    fun append(v: Boolean)

    override fun append(value: CharSequence?, startIndex: Int, endIndex: Int): TextWriter

    override fun append(value: CharSequence?): TextWriter = append(value, 0, value?.length ?: 0)
}