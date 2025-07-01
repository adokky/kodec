package io.kodec.text

import io.kodec.DecodingErrorHandler
import karamel.utils.BitDescriptors
import karamel.utils.Bits32

fun RandomAccessTextReader.readUnsignedLong(
    onFail: DecodingErrorHandler<IntegerParsingError> = fail
): ULong {
    return readUnsignedLong(
        charClasses = DefaultCharClasses.mapper,
        terminatorClass = DefaultCharClasses.WORD_TERM,
        onFail = onFail,
    )
}

fun <BDS: BitDescriptors> RandomAccessTextReader.readUnsignedLong(
    charClasses: CharToClassMapper<BDS>,
    terminatorClass: Bits32<BDS>,
    onFail: DecodingErrorHandler<IntegerParsingError> = fail,
): ULong {
    trySkip('+')

    val limitForMaxRadix = ULong.MAX_VALUE / 10u
    var limitBeforeMul = limitForMaxRadix
    var result = 0uL
    val digitsStart = position
    var malformed = false

    run {
        while (nextCodePoint in '0'.code..'9'.code) {
            val char = readAsciiCode()
            val digit = char - '0'.code

            if (result > limitBeforeMul) {
                if (limitBeforeMul != limitForMaxRadix) return@run
                limitBeforeMul = ULong.MAX_VALUE / 10u
                if (result > limitBeforeMul) return@run
            }

            result *= 10u

            val beforeAdding = result
            result += digit.toUInt()
            if (result < beforeAdding) return@run
        }

        fixNextCodePoint()

        if (position == digitsStart || !charClasses.hasClass(nextCodePoint, terminatorClass)) {
            malformed = true
            return@run
        }

        return result
    }

    onFail(if (malformed) IntegerParsingError.MalformedNumber else IntegerParsingError.Overflow)
    return 0uL
}