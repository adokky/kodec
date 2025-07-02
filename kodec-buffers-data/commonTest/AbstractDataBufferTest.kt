package io.kodec.buffers

import io.kodec.DiagnosticContext
import io.kodec.FailHandler
import karamel.utils.throwIfUncatchable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

abstract class AbstractDataBufferTest {
    private val array = ByteArray(700)
    private val bufferOffset = 11
    private val bufferSize = 500
    private val magicByte: Byte = 113

    private val bufferSafeLe   = array.asDataBuffer(bufferOffset, bufferSize, ByteOrder.LittleEndian, rangeChecks = true)
    private val bufferSafeBe   = array.asDataBuffer(bufferOffset, bufferSize, ByteOrder.BigEndian,    rangeChecks = true)
    private val bufferUnsafeLe = array.asDataBuffer(bufferOffset, bufferSize, ByteOrder.LittleEndian, rangeChecks = false)
    private val bufferUnsafeBe = array.asDataBuffer(bufferOffset, bufferSize, ByteOrder.BigEndian,    rangeChecks = false)

    open fun <T> test(
        dataSet: () -> Sequence<T>,
        readData: DataBuffer.(offset: Int) -> T,
        writeData: OutputDataBuffer.(offset: Int, T) -> Int,
        onFail: FailHandler<T>? = null,
        maxFails: Int = 100,
    ) {
        testEncoderDecoder(dataSet, writeData, readData, onFail, maxFails)
        testRangeChecks(dataSet, writeData, onFail, maxFails)
    }

    private fun <T> testEncoderDecoder(
        dataSet: () -> Sequence<T>,
        writeData: OutputDataBuffer.(offset: Int, T) -> Int,
        readData: DataBuffer.(offset: Int) -> T,
        onFail: FailHandler<T>?,
        maxFails: Int
    ) {
        var processedDataSetItems = 0

        for (buffer in arrayOf(bufferSafeLe, bufferSafeBe, bufferUnsafeLe, bufferUnsafeBe)) {
            var fails = 0

            for (expected in dataSet()) {
                processedDataSetItems++
                buffer.clear()

                val pos = 7

                var encoded: ByteArray? = null
                var actual: T? = null

                try {
                    val written = buffer.writeData(pos, expected)
                    assertTrue(written in 0..buffer.array.size,
                        "write function returned invalid number of bytes written: $written")

                    encoded = buffer.toByteArray(pos until pos + written)
                    actual = buffer.readData(pos)

                    assertEquals(expected, actual)
                } catch (cause: Throwable) {
                    cause.throwIfUncatchable()

                    fails++
                    if (fails > maxFails) throw AssertionError("too much fails ($fails), skipping the rest of the test data set")

                    if (onFail != null) DiagnosticContext(expected, actual, encoded, cause).onFail() else {
                        val nonZeroStartsAt = buffer.array.indexOfFirst { it != 0.toByte() }
                        val nonZeroEndsAt = buffer.array.indexOfLast { it != 0.toByte() }
                        val nonZeroRange = nonZeroStartsAt..nonZeroEndsAt

                        throw AssertionError(
                            buildString {
                                append("failed on byteOrder=${buffer.byteOrder}")
                                append("\nvalue: '$expected'")
                                append(
                                    if (nonZeroRange.isEmpty())
                                        "\nbuffer is zero" else
                                        "\nbuffer[$nonZeroRange]: ${
                                            buffer.array.sliceArray(nonZeroRange).contentToString()
                                        }"
                                )
                                if (encoded !=  null) append("\nencoded: ${encoded.contentToString()}")
                            },
                            cause
                        )
                    }
                }
            }
        }

        assertTrue(processedDataSetItems > 1, "data set is empty")
    }

    private fun <T> testRangeChecks(
        dataSet: () -> Sequence<T>,
        writeData: OutputDataBuffer.(offset: Int, T) -> Int,
        onFail: FailHandler<T>?,
        maxFails: Int
    ) {
        var processedDataSetItems = 0

        for (buffer in arrayOf(bufferSafeLe, bufferSafeBe)) {
            var fails = 0

            for (expected in dataSet()) {
                processedDataSetItems++
                array.fill(magicByte)

                var encoded: ByteArray? = null
                try {
                    assertFailsWith<IndexOutOfBoundsException> { buffer.writeData(-1, expected) }
                    assertFailsWith<IndexOutOfBoundsException> { buffer.writeData(bufferSize, expected) }

                    val pos = 7
                    val written = try {
                        buffer.writeData(pos, expected).also { written ->
                            encoded = buffer.toByteArray(pos until pos + written)
                        }
                    } catch (_: IndexOutOfBoundsException) {
                        // consider all valid buffer space is written
                        bufferSize - pos
                    }

                    assertTrue(array.asSequence().take(bufferOffset + pos).all { it == magicByte },
                        "bytes were written before target position")
                    assertTrue(array.asSequence().drop(bufferOffset + pos + written).all { it == magicByte },
                        "bytes were written after reported destination")
                } catch (e: Throwable) {
                    e.throwIfUncatchable()

                    fails++
                    if (fails > maxFails) throw AssertionError("too many failures ($fails), skipping the rest of the test data set")

                    if (onFail != null) DiagnosticContext(expected, actual = null, encoded, e).onFail() else {
                        throw AssertionError("failed on byteOrder=${buffer.byteOrder}, value: '$expected'", e)
                    }
                }
            }
        }

        assertTrue(processedDataSetItems > 1, "data set is empty")
    }
}

class AbstractBufferMetaTest: AbstractDataBufferTest() {
    @Test
    fun metaTest() {
        assertFailsWith<AssertionError> {
            test(
                dataSet = { sequenceOf(42) },
                readData = { 10 },
                writeData = { pos, _ -> set(pos, 1); 1 /* bytes written */}
            )
        }

        test(
            dataSet = { sequenceOf(42) },
            readData = { pos -> get(pos) },
            writeData = { pos, _ -> set(pos, 42); 1 /* bytes written */}
        )
    }
}