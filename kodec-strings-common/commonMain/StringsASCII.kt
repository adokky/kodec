package io.kodec

object StringsASCII {
    const val INVALID_BYTE_PLACEHOLDER: Char = '�'

    const val LOWER_CASE_BIT: Int = 1 shl 5

    fun uppercase(byte: Int): Int = if (byte in 'A'.code..'Z'.code) byte and LOWER_CASE_BIT.inv() else byte

    fun lowercase(byte: Int): Int = if (byte in 'A'.code..'Z'.code) byte or LOWER_CASE_BIT else byte

    fun isAscii(string: String): Boolean = string.all { it.code < 128 }

    inline fun readFromByteStream(
        length: Int,
        readByte: () -> Int
    ): String {
        val chars = CharArray(length)
        readFromByteStream(chars, destStart = 0, destEnd = length, readByte)
        return chars.concatToString()
    }

    inline fun readFromByteStream(
        dest: CharArray,
        destStart: Int,
        destEnd: Int,
        readByte: () -> Int
    ) {
        for (i in destStart ..< destEnd) {
            val byte = readByte()
            dest[i] = if (byte > 0x7f) INVALID_BYTE_PLACEHOLDER else byte.toChar()
        }
    }
    
    inline fun write(
        str: CharSequence,
        strStart: Int,
        strEnd: Int,
        writeByte: (Int) -> Unit
    ): Int {
        for (i in strStart ..< strEnd) {
            val c = str[i]
            if (c.code > 0x7f) throw IllegalArgumentException("String is not ASCII")
            writeByte(c.code)
        }

        return str.length
    }
}