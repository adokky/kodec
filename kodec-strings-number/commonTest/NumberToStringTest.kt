package io.kodec

import io.kodec.buffers.ArrayBuffer
import io.kodec.buffers.Buffer
import karamel.utils.buildString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class NumberToStringTest {
    @Test
    fun long() {
        val buf = ArrayBuffer(30)
        for (num in NumbersDataSet.getInts64()) {
            val length = NumberToString.putDigits(num, buf, 0)
            assertEquals(num, buf.getStringAscii(0, length).toLong())
        }
    }

    @Test
    fun int() {
        val buf = ArrayBuffer(30)
        for (num in NumbersDataSet.getInts32()) {
            val length = NumberToString.putDigits(num, buf, 0)
            assertEquals(num, buf.getStringAscii(0, length).toInt())
        }
    }

    @Test
    fun float() {
        val sb = StringBuilder()
        for (num in NumbersDataSet.getFloat32()) {
            val s = buildString(sb) { FloatingDecimalToAscii.getThreadLocalInstance().appendTo(num, this) }
            val decoded = try {
                s.toFloat()
            } catch (_: NumberFormatException) {
                fail("expected: $num, actual: '$s'")
            }
            if (num.isNaN() && decoded.isNaN()) continue
            if (decoded !in (num - 0.0001f..num + 0.0001f))
                fail("expected: $num, actual: '$s', decoded: $decoded")
        }
    }

    @Test
    fun double() {
        val sb = StringBuilder()
        for (num in NumbersDataSet.getFloat64()) {
            val s = buildString(sb) { FloatingDecimalToAscii.getThreadLocalInstance().appendTo(num, this) }
            val decoded = try {
                s.toDouble()
            } catch (_: NumberFormatException) {
                fail("expected: $num, actual: '$s'")
            }
            if (num.isNaN() && decoded.isNaN()) continue
            if (decoded !in (num - 0.00001..num + 0.00001))
                fail("expected: $num, actual: '$s', decoded: $decoded")
        }
    }
}

internal fun Buffer.getStringAscii(pos: Int, length: Int): String {
    val chars = CharArray(length)
    var index = pos
    StringsASCII.readFromByteStream(chars, 0, destEnd = length) { get(index++) }
    return chars.concatToString()
}