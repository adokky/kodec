package io.kodec.text

import karamel.utils.AutoBitDescriptors
import karamel.utils.Bits32

object DefaultCharClasses: AutoBitDescriptors() {
    val FLOAT: Bits32<DefaultCharClasses>         = uniqueBit()
    val DIGIT: Bits32<DefaultCharClasses>         = uniqueBit() + FLOAT
    val JSON_TOKEN: Bits32<DefaultCharClasses>    = uniqueBit()
    val WORD_TERM: Bits32<DefaultCharClasses>     = uniqueBit()
    val JSON_STR_TERM: Bits32<DefaultCharClasses> = uniqueBit() + WORD_TERM
    val WHITESPACE: Bits32<DefaultCharClasses>    = uniqueBit() + JSON_STR_TERM + JSON_TOKEN
    val INVALID: Bits32<DefaultCharClasses>       = uniqueBit()
    val EOF: Bits32<DefaultCharClasses>           = uniqueBit() + JSON_STR_TERM

    val mapper: CharToClassMapper<DefaultCharClasses> = CharToClassMapper<DefaultCharClasses>().apply {
        assignClasses(-1, EOF)

        // ASCII control characters
        for (i in 0 ..< 0x20) {
            assignClasses(i, JSON_STR_TERM + INVALID)
        }

        // whitespace
        assignClasses(0x09, WHITESPACE) // HT
        assignClasses(0x0a, WHITESPACE) // LF
        assignClasses(0x0d, WHITESPACE) // CR
        assignClasses(0x20, WHITESPACE) // space

        // digits
        for (c in 0x30..0x39) {
            assignClasses(c, DIGIT)
        }

        assignClasses('e', FLOAT)
        assignClasses('E', FLOAT)

        assignClasses('N', FLOAT)
        assignClasses('a', FLOAT)

        assignClasses('*', WORD_TERM)
        assignClasses('!', WORD_TERM)
        assignClasses('#', WORD_TERM)
        assignClasses('$', WORD_TERM)
        assignClasses('%', WORD_TERM)
        assignClasses('&', WORD_TERM)
        assignClasses('\'', WORD_TERM)
        assignClasses('(', WORD_TERM)
        assignClasses(')', WORD_TERM)
        assignClasses('`', WORD_TERM)
        assignClasses('/', WORD_TERM)
        assignClasses(';', WORD_TERM)
        assignClasses('<', WORD_TERM)
        assignClasses('>', WORD_TERM)
        assignClasses('=', WORD_TERM)
        assignClasses('?', WORD_TERM)
        assignClasses('@', WORD_TERM)

        assignClasses(',', JSON_STR_TERM + JSON_TOKEN)
        assignClasses('.', FLOAT)
        assignClasses('-', FLOAT)
        assignClasses('+', FLOAT)
        assignClasses(':', JSON_STR_TERM + JSON_TOKEN)
        assignClasses('{', JSON_STR_TERM + JSON_TOKEN)
        assignClasses('}', JSON_STR_TERM + JSON_TOKEN)
        assignClasses('[', JSON_STR_TERM + JSON_TOKEN)
        assignClasses(']', JSON_STR_TERM + JSON_TOKEN)
        assignClasses('"', JSON_STR_TERM + JSON_TOKEN)
        assignClasses('\\', JSON_TOKEN + WORD_TERM)
    }

    fun hasValidClass(code: Int, classes: Bits32<DefaultCharClasses>): Boolean =
        mapper.getClasses(code).containsAllExcept(classes, none = INVALID)

    fun isFloatLiteral(code: Int): Boolean = hasValidClass(code, FLOAT)

    fun isJsonToken(code: Int): Boolean = hasValidClass(code, JSON_TOKEN)

    fun isWordTerminator(code: Int): Boolean = hasValidClass(code, WORD_TERM)

    fun isWhitespace(code: Int): Boolean = hasValidClass(code, WHITESPACE)

    fun isDigit(code: Int): Boolean = code in '0'.code..'9'.code
}