package io.kodec.text

import io.kodec.DecodingErrorHandler
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
            val container = errorContainer.prepare<IntegerParsingError>()
            accumulator = readLongExponent(accumulator, charClasses, terminatorClass, onFormatError = container)
            container.consumeError { err ->
                when(err) {
                    IntegerParsingError.MalformedNumber -> { onFail(NumberParsingError.MalformedNumber); return }
                    IntegerParsingError.Overflow -> { onFail(NumberParsingError.IntegerOverflow); return }
                }
            }
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

class ReadNumberResult {
    var asDouble: Double = 0.0
        private set
    var asLong: Long = 0
        private set
    var isDouble: Boolean = false
        private set

    fun set(double: Double) {
        asDouble = double
        isDouble = true
    }

    fun set(long: Long) {
        asLong = long
        isDouble = false
    }

    fun clear(): ReadNumberResult {
        asLong = 0
        asDouble = 0.0
        isDouble = false
        return this
    }

    override fun toString(): String {
        return (if (isDouble) asDouble else asLong).toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReadNumberResult) return false

        if (asDouble != other.asDouble) return false
        if (asLong != other.asLong) return false
        if (isDouble != other.isDouble) return false

        return true
    }

    override fun hashCode(): Int {
        var result = asDouble.hashCode()
        result = 31 * result + asLong.hashCode()
        result = 31 * result + isDouble.hashCode()
        return result
    }
}

fun <BDS: BitDescriptors> RandomAccessTextReader.readNumber(
    result: ReadNumberResult,
    charClasses: CharToClassMapper<BDS>,
    terminatorClass: Bits32<BDS>,
    onFail: DecodingErrorHandler<NumberParsingError> = fail,
    allowSpecialFp: Boolean = false
) {
    readNumberTemplate(
        acceptInt = { result.set(it) },
        acceptFloat = { result.set(it) },
        charClasses = charClasses,
        terminatorClass = terminatorClass,
        onFail = { onFail(it) },
        allowSpecialFp = allowSpecialFp
    )
}

@PublishedApi
internal fun isSpecialFpFirstChar(code: Int): Boolean {
    val lc = code or StringsASCII.LOWER_CASE_BIT
    return lc == 'i'.code || lc == 'n'.code
}