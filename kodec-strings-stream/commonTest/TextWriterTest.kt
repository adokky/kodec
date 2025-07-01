package io.kodec.text

import io.kodec.NumbersDataSet
import io.kodec.StringsDataSet
import io.kodec.buffers.ArrayDataBuffer
import io.kodec.buffers.fill
import io.kodec.buffers.getStringUtf8ByteSized
import karamel.utils.enrichMessageOf
import kotlin.test.Test
import kotlin.test.assertEquals

class TextWriterTest {
    val stringBuilder = StringBuilder()

    private fun testAllModes(body: TestMode.() -> Unit) {
        enrichMessageOf<Throwable>({ "TestMode.StringBased" }) {
            TestMode.StringBased(stringBuilder).body()
        }
        enrichMessageOf<Throwable>({ "TestMode.BufferBased" }) {
            TestMode.BufferBased(stringBuilder).body()
        }
    }

    @Test fun booleans() = testAllModes {
        writer.append(true)
        stringBuilder.append(true)
        checkAndClear()
        writer.append(false)
        stringBuilder.append(false)
        checkAndClear()
    }

    @Test fun strings() = testAllModes {
        for (s in StringsDataSet.getUtfData()) {
            for (c in s) {
                if (c.isSurrogate()) continue
                writer.append(c)
                stringBuilder.append(c)
            }
            checkAndClear()

            writer.append(s)
            stringBuilder.append(s)
            checkAndClear()
        }
    }

    @Test fun bytes() = testAllModes {
        for (n in NumbersDataSet.getInts8()) {
            writer.append(n)
            stringBuilder.append(n)
            checkAndClear()
        }
    }

    @Test fun shorts() = testAllModes {
        for (n in NumbersDataSet.getInts16()) {
            writer.append(n)
            stringBuilder.append(n)
            checkAndClear()
        }
    }

    @Test fun ints() = testAllModes {
        for (n in NumbersDataSet.getInts32()) {
            writer.append(n)
            stringBuilder.append(n)
            checkAndClear()
        }
    }

    @Test fun longs() = testAllModes {
        for (n in NumbersDataSet.getInts64()) {
            writer.append(n)
            stringBuilder.append(n)
            checkAndClear()
        }
    }
}

private sealed class TestMode(val stringBuilder: StringBuilder) {
    abstract val writer: TextWriter
    abstract fun getText(): String

    protected abstract fun clearWriter()

    fun check() {
        assertEquals(stringBuilder.toString(), getText())
    }

    fun checkAndClear() {
        check()
        clearWriter()
        stringBuilder.setLength(0)
    }

    class BufferBased(stringBuilder: StringBuilder): TestMode(stringBuilder) {
        private val arrayBuffer = ArrayDataBuffer(1000)
        override val writer = BufferTextWriter(arrayBuffer)

        override fun getText(): String =
            arrayBuffer.getStringUtf8ByteSized(pos = 0, byteLength = writer.position)

        override fun clearWriter() {
            arrayBuffer.fill(0, endExclusive = writer.position)
            writer.position = 0
        }
    }

    class StringBased(stringBuilder: StringBuilder): TestMode(stringBuilder) {
        private val sb = StringBuilder()
        override val writer = StringTextWriter(sb)
        override fun getText(): String = sb.toString()
        override fun clearWriter() { sb.setLength(0) }
    }
}