package io.kodec.text

import io.kodec.DecodingErrorWithMessage
import io.kodec.StringsASCII
import karamel.utils.BitDescriptors
import karamel.utils.Bits32

enum class NumberParsingError(override val message: String): DecodingErrorWithMessage {
    MalformedNumber("expected a number"),
    FloatOverflow("floating-point number is too big"),
    IntegerOverflow("number is out of range ${(Long.MIN_VALUE .. Long.MAX_VALUE)}")
}

inline fun RandomAccessTextReader.readNumberTemplate(
    acceptInt: (Long) -> Unit,
    acceptFloat: (Double) -> Unit,
    onFail: (error: NumberParsingError) -> Unit = { fail(it.message) },
    allowSpecialFp: Boolean = false
) {
    readNumberTemplate(
        acceptInt = acceptInt,
        acceptFloat = acceptFloat,
        charClasses = DefaultCharClasses.mapper,
        terminatorClass = DefaultCharClasses.JSON_STR_TERM,
        onFail = onFail,
        allowSpecialFp = allowSpecialFp,
    )
}

inline fun <BDS: BitDescriptors> RandomAccessTextReader.readNumberTemplate(
    acceptInt: (Long) -> Unit,
    acceptFloat: (Double) -> Unit,
    charClasses: CharToClassMapper<BDS>,
    terminatorClass: Bits32<BDS>,
    onFail: (error: NumberParsingError) -> Unit = { fail(it.message) },
    allowSpecialFp: Boolean = false
) {
    val start = position
    var isNegative = false

    if (nextCodePoint == '-'.code) {
        readCodePoint()
        isNegative = true
    }

    var overflow = false
    var hasIntDigits = false
    var accumulator = 0L

    var ch = nextCodePoint
    while (ch in '0'.code..'9'.code) {
        accumulator = accumulator * 10 - (ch - '0'.code)
        if (accumulator > 0) overflow = true
        readCodePoint()
        ch = nextCodePoint
        hasIntDigits = true
    }

    if (nextCodePoint == '.'.code || (allowSpecialFp && !hasIntDigits && isSpecialFpFirstChar(nextCodePoint))) { // Float
        if (overflow) { onFail(NumberParsingError.FloatOverflow); return }

        while (!charClasses.hasClass(nextCodePoint, terminatorClass) && nextCodePoint >= 0) readCodePoint()

        val result = parseFloat(start, position, onFormatError = errorContainer.prepare()).doubleValue()
        errorContainer.consumeError { onFail(NumberParsingError.MalformedNumber); return }

        if (!charClasses.hasClass(nextCodePoint, terminatorClass)) {
            onFail(NumberParsingError.MalformedNumber)
            return
        }

        acceptFloat(result)
    } else { // Int
        if (overflow) { onFail(NumberParsingError.IntegerOverflow); return }
        if (!hasIntDigits) { onFail(NumberParsingError.MalformedNumber); return }
        
        if (nextCodePoint or StringsASCII.LOWER_CASE_BIT == 'e'.code) {
            readCodePoint()
            accumulator = readLongExponent(accumulator, charClasses, terminatorClass, onFormatError = errorContainer.prepare())
            errorContainer.handle(IntegerParsingError.MalformedNumber) { onFail(NumberParsingError.MalformedNumber); return }
            errorContainer.handle(IntegerParsingError.Overflow) { onFail(NumberParsingError.IntegerOverflow); return }
        }

        if (!charClasses.hasClass(nextCodePoint, terminatorClass)) {
            onFail(NumberParsingError.MalformedNumber)
            return
        }

        acceptInt(when {
            isNegative -> accumulator
            accumulator != Long.MIN_VALUE -> -accumulator
            else -> { onFail(NumberParsingError.IntegerOverflow); return }
        })
    }
}

@PublishedApi
internal fun isSpecialFpFirstChar(code: Int): Boolean {
    val lc = code or StringsASCII.LOWER_CASE_BIT
    return lc == 'i'.code || lc == 'n'.code
}