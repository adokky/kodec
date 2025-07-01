package io.kodec

import io.kodec.buffers.Buffer

@Throws(NumberFormatException::class)
fun Buffer.parseFloat(
    start: Int = 0,
    endExclusive: Int = size,
    onFormatError: DecodingErrorHandler<String>? = null
): ASCIIToBinaryConverter {
    return FloatingDecimalParsing.readBuffer(this, start, endExclusive, onFormatError = onFormatError)
}

@Throws(NumberFormatException::class)
fun CharSequence.toFloat(start: Int, endExclusive: Int = length): Float {
    return FloatingDecimalParsing.readString(this, start, endExclusive).floatValue()
}

@Throws(NumberFormatException::class)
fun CharSequence.toDouble(start: Int, endExclusive: Int = length): Double {
    return FloatingDecimalParsing.readString(this, start, endExclusive).doubleValue()
}