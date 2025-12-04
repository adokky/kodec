package io.kodec.text

inline fun RandomAccessTextReader.readCodePoints(start: Int, end: Int, accept: (codepoint: Int) -> Unit) {
    var pos = start
    while (pos < end) {
        val cp = readCodePoint(pos)
        if (cp == CodePointAndSize.EOF) return
        accept(cp.codepoint)
        pos += cp.size
    }
}