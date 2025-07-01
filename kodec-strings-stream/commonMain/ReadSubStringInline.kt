package io.kodec.text

import io.kodec.StringHashCode

inline fun RandomAccessTextReader.readSubStringInline(acceptChar: (Char) -> Boolean): RandomAccessTextReaderSubString {
    val result = RandomAccessTextReaderSubString()
    readSubStringInline(result, acceptChar)
    return result
}

inline fun RandomAccessTextReader.readSubStringInline(dest: RandomAccessTextReaderSubString, acceptChar: (Char) -> Boolean) {
    val start = position
    var hash = StringHashCode.init()
    val codePoints = readCharsInline { c ->
        if (acceptChar(c)) {
            hash = StringHashCode.next(hash, c)
            true
        } else false
    }
    dest.set(
        reader = this,
        start = start,
        end = position,
        codePoints = codePoints,
        hashCode = hash
    )
}