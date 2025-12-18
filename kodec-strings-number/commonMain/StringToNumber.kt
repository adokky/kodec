package io.kodec

import io.kodec.buffers.Buffer

/**
 * @throws NumberFormatException if [onFormatError] is not specified
 */
fun Buffer.parseFloat(
    start: Int = 0,
    endExclusive: Int = size,
    onFormatError: DecodingErrorHandler<String> = FloatingDecimalParsing.DEFAULT_ERROR_HANDLER
): StringToFpConverter {
    return FloatingDecimalParsing.prepareFloat(this, start, endExclusive, onFormatError = onFormatError)
}

/**
 * @throws NumberFormatException if [onFormatError] is not specified
 */
fun Buffer.parseDouble(
    start: Int = 0,
    endExclusive: Int = size,
    onFormatError: DecodingErrorHandler<String> = FloatingDecimalParsing.DEFAULT_ERROR_HANDLER
): StringToFpConverter {
    return FloatingDecimalParsing.prepareDouble(this, start, endExclusive, onFormatError = onFormatError)
}

/**
 * @throws NumberFormatException if [onFormatError] is not specified
 */
fun CharSequence.parseFloat(
    start: Int = 0,
    endExclusive: Int = length,
    onFormatError: DecodingErrorHandler<String> = FloatingDecimalParsing.DEFAULT_ERROR_HANDLER
): StringToFpConverter {
    return FloatingDecimalParsing.prepareFloat(this, start, endExclusive, onFormatError)
}

/**
 * @throws NumberFormatException if [onFormatError] is not specified
 */
fun CharSequence.parseDouble(
    start: Int = 0,
    endExclusive: Int = length,
    onFormatError: DecodingErrorHandler<String> = FloatingDecimalParsing.DEFAULT_ERROR_HANDLER
): StringToFpConverter {
    return FloatingDecimalParsing.prepareDouble(this, start, endExclusive, onFormatError)
}

/**
 * @throws NumberFormatException if [onFormatError] is not specified
 */
fun CharSequence.toFloat(
    start: Int,
    endExclusive: Int = length,
    onFormatError: DecodingErrorHandler<String> = FloatingDecimalParsing.DEFAULT_ERROR_HANDLER
): Float {
    return parseFloat(start, endExclusive, onFormatError).floatValue()
}

/**
 * @throws NumberFormatException if [onFormatError] is not specified
 */
fun CharSequence.toDouble(
    start: Int,
    endExclusive: Int = length,
    onFormatError: DecodingErrorHandler<String> = FloatingDecimalParsing.DEFAULT_ERROR_HANDLER
): Double {
    return parseDouble(start, endExclusive, onFormatError).doubleValue()
}