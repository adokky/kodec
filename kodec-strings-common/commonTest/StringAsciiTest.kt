package io.kodec

import kotlin.test.Test
import kotlin.test.assertEquals

class StringAsciiTest {
    @Test
    fun ascii_lowercase() {
        assertEquals('{'.code, StringsASCII.lowercase('{'.code))
        assertEquals(' '.code, StringsASCII.lowercase(' '.code))
        assertEquals('a'.code, StringsASCII.lowercase('A'.code))
        assertEquals('a'.code, StringsASCII.lowercase('a'.code))
        assertEquals('z'.code, StringsASCII.lowercase('Z'.code))
        assertEquals('z'.code, StringsASCII.lowercase('z'.code))
    }
}