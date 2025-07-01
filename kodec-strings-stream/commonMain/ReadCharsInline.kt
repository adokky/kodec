package io.kodec.text

import io.kodec.StringsUTF16

inline fun TextReader.readCharsInline(acceptChar: (Char) -> Boolean): Int {
    var codePoints = 0
    while (true) {
        val cp = nextCodePoint
        if (cp < 0) break
        StringsUTF16.getChars(cp) { c -> if (!acceptChar(c)) return codePoints }
        codePoints++
        readCodePoint()
    }
    return codePoints
}

inline fun TextReader.readCharsInline(maxCodePoints: Int, acceptChar: (Char) -> Unit): Int {
    var codePoints = 0
    while (true) {
        val cp = nextCodePoint
        if (cp < 0) break
        if (codePoints++ >= maxCodePoints) break
        StringsUTF16.getChars(cp, acceptChar)
        readCodePoint()
    }
    return codePoints
}