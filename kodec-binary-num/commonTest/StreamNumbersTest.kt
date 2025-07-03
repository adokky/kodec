package io.kodec

import karamel.utils.asInt
import karamel.utils.throwIfUncatchable
import kotlin.test.Test
import kotlin.test.assertEquals

class StreamNumbersTest {
    private val buffer = ByteArray(20)
    private var writerPos = 0
    private var readerPos = 0

    private fun write(byte: Int) {
        buffer[writerPos++] = byte.toByte()
    }

    private fun read(): Int {
        return buffer[readerPos++].asInt()
    }

    private fun reset() {
        buffer.fill(0, toIndex = writerPos)
        writerPos = 0
        readerPos = 0
    }

    private inline fun <T> test(
        size: Int,
        getValue: (index: Int) -> T,
        writeValue: (T, writeByte: (byte: Int) -> Unit) -> Int,
        readValue: (readByte: () -> Int) -> T
    ) {
        repeat(size) { index ->
            val v = getValue(index)
            reset()
            try {
                val written = writeValue(v, ::write)
                assertEquals(writerPos, written)
                assertEquals(v, readValue(::read))
            } catch (e: Throwable) {
                e.throwIfUncatchable()
                throw AssertionError("failed at: '$v'", e)
            }
        }
    }

    private inline fun ShortArray.test(
        writeValue: (Short, writeByte: (byte: Int) -> Unit) -> Int,
        readValue: (readByte: () -> Int) -> Short
    ) = test(size, getValue = { this[it] }, writeValue, readValue)

    private inline fun IntArray.test(
        writeValue: (Int, writeByte: (byte: Int) -> Unit) -> Int,
        readValue: (readByte: () -> Int) -> Int
    ) = test(size, getValue = { this[it] }, writeValue, readValue)

    private inline fun LongArray.test(
        writeValue: (Long, writeByte: (byte: Int) -> Unit) -> Int,
        readValue: (readByte: () -> Int) -> Long
    ) = test(size, getValue = { this[it] }, writeValue, readValue)

    @Test
    fun int16_BE() = NumbersDataSet.ints16.test(
        NumbersBigEndian::writeInt16,
        NumbersBigEndian::readInt16
    )

    @Test
    fun int16_LE() = NumbersDataSet.ints16.test(
        NumbersLittleEndian::writeInt16,
        NumbersLittleEndian::readInt16
    )

    @Test
    fun int24_BE() = NumbersDataSet.ints24.test(
        NumbersBigEndian::writeInt24,
        NumbersBigEndian::readInt24
    )

    @Test
    fun int24_LE() = NumbersDataSet.ints24.test(
        NumbersLittleEndian::writeInt24,
        NumbersLittleEndian::readInt24
    )

    @Test
    fun uint24_BE() = NumbersDataSet.uints24.test(
        NumbersBigEndian::writeUInt24,
        NumbersBigEndian::readUInt24
    )

    @Test
    fun uint24_LE() = NumbersDataSet.uints24.test(
        NumbersLittleEndian::writeUInt24,
        NumbersLittleEndian::readUInt24
    )

    @Test
    fun int32_BE() = NumbersDataSet.ints32.test(
        NumbersBigEndian::writeInt32,
        NumbersBigEndian::readInt32
    )

    @Test
    fun int32_LE() = NumbersDataSet.ints32.test(
        NumbersLittleEndian::writeInt32,
        NumbersLittleEndian::readInt32
    )

    @Test
    fun int40_BE() = NumbersDataSet.ints40.test(
        NumbersBigEndian::writeInt40,
        NumbersBigEndian::readInt40
    )

    @Test
    fun int40_LE() = NumbersDataSet.ints40.test(
        NumbersLittleEndian::writeInt40,
        NumbersLittleEndian::readInt40
    )

    @Test
    fun uint40_BE() = NumbersDataSet.uints40.test(
        NumbersBigEndian::writeUInt40,
        NumbersBigEndian::readUInt40
    )

    @Test
    fun uint40_LE() = NumbersDataSet.uints40.test(
        NumbersLittleEndian::writeUInt40,
        NumbersLittleEndian::readUInt40
    )

    @Test
    fun int48_BE() = NumbersDataSet.ints48.test(
        NumbersBigEndian::writeInt48,
        NumbersBigEndian::readInt48
    )

    @Test
    fun int48_LE() = NumbersDataSet.ints48.test(
        NumbersLittleEndian::writeInt48,
        NumbersLittleEndian::readInt48
    )

    @Test
    fun int56_BE() = NumbersDataSet.ints56.test(
        NumbersBigEndian::writeInt56,
        NumbersBigEndian::readInt56
    )

    @Test
    fun int56_LE() = NumbersDataSet.ints56.test(
        NumbersLittleEndian::writeInt56,
        NumbersLittleEndian::readInt56
    )

    @Test
    fun uint56_BE() = NumbersDataSet.uints56.test(
        NumbersBigEndian::writeUInt56,
        NumbersBigEndian::readUInt56
    )

    @Test
    fun uint56_LE() = NumbersDataSet.uints56.test(
        NumbersLittleEndian::writeUInt56,
        NumbersLittleEndian::readUInt56
    )

    @Test
    fun int64_BE() = NumbersDataSet.ints64.test(
        NumbersBigEndian::writeInt64,
        NumbersBigEndian::readInt64
    )

    @Test
    fun int64_LE() = NumbersDataSet.ints64.test(
        NumbersLittleEndian::writeInt64,
        NumbersLittleEndian::readInt64
    )

    @Test
    fun varint32_opt_positive() = varint32_test(true)

    @Test
    fun varint32_opt_negative() = varint32_test(false)

    @Test
    fun varint64_opt_positive() = varint64_test(true)

    @Test
    fun varint64_opt_negative() = varint64_test(false)

    private fun varint32_test(optimizePositive: Boolean) {
        NumbersDataSet.ints32.test(
            { v, write ->
                VariableEncoding.writeInt32(
                    v,
                    optimizePositive = optimizePositive,
                    writeByte = { write(it.asInt()) })
            },
            { read -> VariableEncoding.readInt32(optimizePositive = optimizePositive, nextByte = read) }
        )
    }

    private fun varint64_test(optimizePositive: Boolean) {
        NumbersDataSet.ints64.test(
            { v, write ->
                VariableEncoding.writeInt64(
                    v,
                    optimizePositive = optimizePositive,
                    writeByte = { write(it.asInt()) })
            },
            { read -> VariableEncoding.readInt64(optimizePositive = optimizePositive, nextByte = read) }
        )
    }
}