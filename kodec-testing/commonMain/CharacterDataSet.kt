package io.kodec

object CharacterDataSet {
    fun ascii(): Sequence<Char> = (0..127).asSequence().map { it.toChar() }

    fun utf8_2bytes(): Sequence<Char> = ('\u0080'..'\u07FF').asSequence()

    fun utf8_3bytes(): Sequence<Char> = charArrayOf(
        '\u0800',
        '\u1234',
        '\u7FFF',
        '\u8000',
        '\uABCD',
        '\uF123',
        '\uFFFF'
    ).asSequence()
}