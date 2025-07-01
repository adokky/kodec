package io.kodec.text

import io.kodec.StringsASCII.LOWER_CASE_BIT

internal fun TextReader.readBooleanDefault(): Boolean {
    val firstChar = readCodePoint()

    if (firstChar or LOWER_CASE_BIT == 't'.code &&
        readCodePoint() or LOWER_CASE_BIT == 'r'.code &&
        readCodePoint() or LOWER_CASE_BIT == 'u'.code &&
        readCodePoint() or LOWER_CASE_BIT == 'e'.code)
    {
        return true
    }

    if (firstChar or LOWER_CASE_BIT == 'f'.code &&
        readCodePoint() or LOWER_CASE_BIT == 'a'.code &&
        readCodePoint() or LOWER_CASE_BIT == 'l'.code &&
        readCodePoint() or LOWER_CASE_BIT == 's'.code &&
        readCodePoint() or LOWER_CASE_BIT == 'e'.code)
    {
        return false
    }

    fail("Expected 'true' or 'false'")
}

internal fun CharSequence.readBooleanDefault(start: Int, end: Int): Boolean {
    val firstChar = this[start].code

    if (firstChar or LOWER_CASE_BIT == 't'.code &&
        end - start == 4 &&
        this[start + 1].code or LOWER_CASE_BIT == 'r'.code &&
        this[start + 2].code or LOWER_CASE_BIT == 'u'.code &&
        this[start + 3].code or LOWER_CASE_BIT == 'e'.code)
    {
        return true
    }

    if (firstChar or LOWER_CASE_BIT == 'f'.code &&
        end - start == 5 &&
        this[start + 1].code or LOWER_CASE_BIT == 'a'.code &&
        this[start + 2].code or LOWER_CASE_BIT == 'l'.code &&
        this[start + 3].code or LOWER_CASE_BIT == 's'.code &&
        this[start + 4].code or LOWER_CASE_BIT == 'e'.code)
    {
        return false
    }

    throw TextDecodingException(
        "The string doesn't represent a boolean value: ${this.substring(start, end)}"
    )
}