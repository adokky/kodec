package io.kodec.text

internal fun contentEquals(ss1: TextReaderSubString, ss2: SimpleSubString): Boolean {
    var ss2Pos = ss2.start
    val ss2End = ss2.end
    ss1.forEach { char ->
        if (ss2Pos >= ss2End) return false
        if (ss2.source[ss2Pos++] != char) return false
    }
    return ss2Pos == ss2End
}