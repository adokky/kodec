package io.kodec.text

import io.kodec.StringHashCode

inline fun RandomAccessTextReader.readSubStringInline(acceptChar: (Char) -> Boolean): TextReaderSubString {
    val result = TextReaderSubString()
    readSubStringInline(result, acceptChar)
    return result
}

inline fun RandomAccessTextReader.readSubStringInline(dest: TextReaderSubString, acceptChar: (Char) -> Boolean) {
    val start = position
    var hash = StringHashCode.init()
    val codePoints = readCharsHeavyInline { c ->
        if (acceptChar(c)) {
            hash = StringHashCode.next(hash, c)
            true
        } else false
    }
    dest.setUnchecked(
        reader = this,
        start = start,
        end = position,
        codePoints = codePoints,
        hashCode = hash
    )
}