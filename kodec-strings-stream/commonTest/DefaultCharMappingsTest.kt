package io.kodec.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DefaultCharMappingsTest {
    @Test
    fun json_tokens() {
        assertFalse(DefaultCharClasses.isJsonToken(-1)) // EOF

        val tokens = arrayOf(',', ':', ' ', '[', ']', '{', '}', '"', '\n', '\r', '\\', '\t')
        for (c in 0..1100) {
            assertEquals(c.toChar() in tokens, DefaultCharClasses.isJsonToken(c), c.toChar().toString())
        }
    }

    @Test
    fun whitespace() {
        assertFalse(DefaultCharClasses.isWhitespace(-1)) // EOF

        val whitespaceChars = arrayOf(' ', '\u0009', '\u000a', '\u000d')
        for (c in 0..1100) {
            assertEquals(c.toChar() in whitespaceChars, DefaultCharClasses.isWhitespace(c), c.toChar().toString())
        }
    }

    @Test
    fun digits() {
        for (c in 0..1000) {
            assertEquals(c in '0'.code..'9'.code, DefaultCharClasses.isDigit(c))
        }
    }

    @Test
    fun word_terminators() {
        val tokens = arrayOf(',', ':', ' ', '[', ']', '{', '}', '"', '\n', '\r', '\\', '\t', '#', '!', '/')
        tokens.forEach { assertTrue(DefaultCharClasses.isWordTerminator(it.code), it.toString()) }
    }
}