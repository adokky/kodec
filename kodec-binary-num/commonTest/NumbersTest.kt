package io.kodec

import karamel.utils.asInt
import karamel.utils.throwIfUncatchable
import kotlin.test.Test
import kotlin.test.assertEquals

class NumbersTest {
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

    private inline fun <T> Sequence<T>.test(
        writeValue: (T, writeByte: (byte: Int) -> Unit) -> Int,
        readValue: (readByte: () -> Int) -> T
    ) {
        for (v in this@test) {
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

    @Test
    fun int16_BE() = NumbersDataSet.getInts16().test(
        NumbersBigEndian::writeInt16,
        NumbersBigEndian::readInt16
    )

    @Test
    fun int16_LE() = NumbersDataSet.getInts16().test(
        NumbersLittleEndian::writeInt16,
        NumbersLittleEndian::readInt16
    )

    @Test
    fun int24_BE() = NumbersDataSet.getInts24().test(
        NumbersBigEndian::writeInt24,
        NumbersBigEndian::readInt24
    )

    @Test
    fun int24_LE() = NumbersDataSet.getInts24().test(
        NumbersLittleEndian::writeInt24,
        NumbersLittleEndian::readInt24
    )

    @Test
    fun int32_BE() = NumbersDataSet.getInts32().test(
        NumbersBigEndian::writeInt32,
        NumbersBigEndian::readInt32
    )

    @Test
    fun int32_LE() = NumbersDataSet.getInts32().test(
        NumbersLittleEndian::writeInt32,
        NumbersLittleEndian::readInt32
    )

    @Test
    fun int40_BE() = NumbersDataSet.getInts40().test(
        NumbersBigEndian::writeInt40,
        NumbersBigEndian::readInt40
    )

    @Test
    fun int40_LE() = NumbersDataSet.getInts40().test(
        NumbersLittleEndian::writeInt40,
        NumbersLittleEndian::readInt40
    )

    @Test
    fun int48_BE() = NumbersDataSet.getInts48().test(
        NumbersBigEndian::writeInt48,
        NumbersBigEndian::readInt48
    )

    @Test
    fun int48_LE() = NumbersDataSet.getInts48().test(
        NumbersLittleEndian::writeInt48,
        NumbersLittleEndian::readInt48
    )

    @Test
    fun int56_BE() = NumbersDataSet.getInts56().test(
        NumbersBigEndian::writeInt56,
        NumbersBigEndian::readInt56
    )

    @Test
    fun int56_LE() = NumbersDataSet.getInts56().test(
        NumbersLittleEndian::writeInt56,
        NumbersLittleEndian::readInt56
    )

    @Test
    fun int64_BE() = NumbersDataSet.getInts64().test(
        NumbersBigEndian::writeInt64,
        NumbersBigEndian::readInt64
    )

    @Test
    fun int64_LE() = NumbersDataSet.getInts64().test(
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
        NumbersDataSet.getInts32().test(
            { v, write -> VariableEncoding.writeInt32(v, optimizePositive = optimizePositive, writeByte = { write(it.asInt()) }) },
            { read -> VariableEncoding.readInt32(optimizePositive = optimizePositive, nextByte = read) }
        )
    }

    private fun varint64_test(optimizePositive: Boolean) {
        NumbersDataSet.getInts64().test(
            { v, write -> VariableEncoding.writeInt64(v, optimizePositive = optimizePositive, writeByte = { write(it.asInt()) }) },
            { read -> VariableEncoding.readInt64(optimizePositive = optimizePositive, nextByte = read) }
        )
    }
}