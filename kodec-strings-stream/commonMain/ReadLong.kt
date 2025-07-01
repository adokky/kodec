package io.kodec.text

import io.kodec.DecodingErrorHandler
import io.kodec.DecodingErrorWithMessage
import io.kodec.StringsASCII
import karamel.utils.BitDescriptors
import karamel.utils.Bits32
import kotlin.math.floor
import kotlin.math.pow

enum class IntegerParsingError(override val message: String): DecodingErrorWithMessage {
    MalformedNumber("malformed number"),
    Overflow("number is too big")
}

internal fun <BDS: BitDescriptors> TextReader.readLongDefault(
    charMappings: CharToClassMapper<BDS>,
    terminatorClass: Bits32<BDS>,
    onFormatError: DecodingErrorHandler<IntegerParsingError> = fail
): Long {
    var isNegative = false
    if (nextCodePoint == '-'.code) {
        isNegative = true
        readCodePoint()
    }

    var hasDigits = false
    var accumulator = 0L

    var ch = nextCodePoint
    while (ch in '0'.code..'9'.code) {
        accumulator = accumulator * 10 - (ch - '0'.code)
        if (accumulator > 0) {
            onFormatError(IntegerParsingError.Overflow)
            return 0
        }
        readCodePoint()
        ch = nextCodePoint
        hasDigits = true
    }

    if (!hasDigits) {
        onFormatError(IntegerParsingError.MalformedNumber)
        return 0
    }

    if (ch or StringsASCII.LOWER_CASE_BIT == 'e'.code) {
        readCodePoint()
        // Exponent is rarely found in JSON integer literals,
        // so we consider this branch as cold path.
        accumulator = readLongExponent(accumulator, charMappings, terminatorClass, onFormatError)
    }

    if (!charMappings.hasClass(nextCodePoint, terminatorClass)) {
        onFormatError(IntegerParsingError.MalformedNumber)
        return 0
    }

    return when {
        isNegative -> accumulator
        accumulator != Long.MIN_VALUE -> -accumulator
        else -> { onFormatError(IntegerParsingError.Overflow); 0 }
    }
}

@PublishedApi
internal fun <BDS: BitDescriptors> TextReader.readLongExponent(
    accumulator: Long,
    charMappings: CharToClassMapper<BDS>,
    terminatorClass: Bits32<BDS>,
    onFormatError: DecodingErrorHandler<IntegerParsingError> = fail
): Long {
    var isExponentPositive = true
    var exponentAccumulator = 0L

    when (nextCodePoint) {
        '-'.code -> { isExponentPositive = false; readCodePoint() }
        '+'.code -> readCodePoint()
    }

    var ch = nextCodePoint
    while (ch in '0'.code..'9'.code) {
        exponentAccumulator = exponentAccumulator * 10 + (ch - '0'.code)
        readCodePoint()
        ch = nextCodePoint
    }

    if (!charMappings.hasClass(nextCodePoint, terminatorClass)) {
        onFormatError(IntegerParsingError.MalformedNumber)
        return 0
    }

    val doubleAccumulator: Double = accumulator.toDouble() * when (isExponentPositive) {
        false -> 10.0.pow(-exponentAccumulator.toDouble())
        true -> 10.0.pow(exponentAccumulator.toDouble())
    }

    if (doubleAccumulator > Long.MAX_VALUE || doubleAccumulator < Long.MIN_VALUE) {
        onFormatError(IntegerParsingError.Overflow)
        return 0
    }

    if (floor(doubleAccumulator) != doubleAccumulator) {
        onFormatError(IntegerParsingError.MalformedNumber)
        return 0
    }

    return doubleAccumulator.toLong()
}