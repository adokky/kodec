package io.kodec

import karamel.utils.asInt
import karamel.utils.assert
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class StringsTestBase: KodecTemplateTest() {
    protected abstract fun readFromBytes(
        getByte: (pos: Int) -> Int,
        endExclusive: Int,
        appendChar: (Char) -> Unit
    ): Int

    protected abstract fun readFromByteStream(
        readByte: () -> Int,
        appendChar: (Char) -> Unit
    )

    protected abstract fun write(
        str: CharSequence,
        offset: Int,
        endExclusive: Int,
        writeByte: (Byte) -> Unit
    ): Int

    protected open fun checkEncoded(expected: String, bytes: ByteArray) {}

    private fun factorial(n: Int): Int = if (n <= 2) n else factorial(n - 1) * n

    protected fun decode(bytes: ByteArray): String = buildString {
        readFromBytes(
            getByte = { bytes[it].asInt() },
            endExclusive = bytes.size,
            appendChar = ::append
        )
    }

    protected fun decodeStreamed(bytes: ByteArray): String = buildString {
        var i = 0
        readFromByteStream(
            readByte = { if (i < bytes.size) bytes[i++].asInt() else -1 },
            appendChar = ::append
        )
    }

    @Test
    fun read_write() {
        check("\uD801\uDC37", 0, 2)

        val random = Random(300)

        for (s in StringsDataSet.getUtfData(random = random)) {
            check(s, 0, s.length)

            repeat(factorial(s.length).coerceAtMost(15)) {
                var start = random.nextInt(s.indices)
                if (start < s.length && s[start].isLowSurrogate()) start++

                var end = if (start >= s.length) s.length else random.nextInt(start, s.length)
                if (s.getOrNull(end - 1)?.isHighSurrogate() == true) end++

                if (start == 0 && end == s.length) return@repeat // already checked

                check(s, start, end)
            }
        }
    }

    private fun check(s: String, start: Int, end: Int) {
        assert { start in 0..s.length }
        assert { end in 0..s.length }
        assert { start <= end }

        val expected = if (start >= end) "" else s.substring(start, end)

        assert { expected.firstOrNull()?.isLowSurrogate() != true }
        assert { expected.lastOrNull()?.isHighSurrogate() != true }

        val bytes = getBytes { writeByte -> write(s, start, endExclusive = end, writeByte) }

        checkEncoded(expected, bytes)

        assertEquals(expected, decode(bytes))
        assertEquals(expected, decodeStreamed(bytes))
    }
}