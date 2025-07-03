package io.kodec

import karamel.utils.asInt
import karamel.utils.throwIfUncatchable
import kotlin.test.Test
import kotlin.test.assertEquals

class RandomAccessNumbersTest {
    private val buffer = ByteArray(20)
    private var maxOffset = 0

    private fun write(offset: Int, byte: Int) {
        if (offset > maxOffset) maxOffset = offset
        buffer[offset] = byte.toByte()
    }

    private fun read(offset: Int): Int {
        return buffer[offset].asInt()
    }

    private fun reset() {
        buffer.fill(0)
    }

    private inline fun <T> test(
        size: Int,
        getValue: (index: Int) -> T,
        writeValue: (T, writeByte: (offset: Int, byte: Int) -> Unit) -> Int,
        readValue: (readByte: (offset: Int) -> Int) -> T
    ) {
        repeat(size) { index ->
            val v = getValue(index)
            reset()
            try {
                val written = writeValue(v, ::write)
                assertEquals(maxOffset + 1, written)
                assertEquals(v, readValue(::read))
            } catch (e: Throwable) {
                e.throwIfUncatchable()
                throw AssertionError("failed at: '$v'", e)
            }
        }
    }

    private inline fun ShortArray.test(
        writeValue: (Short, writeByte: (offset: Int, byte: Int) -> Unit) -> Int,
        readValue: (readByte: (offset: Int) -> Int) -> Short
    ) = test(size, getValue = { this[it] }, writeValue, readValue)

    private inline fun IntArray.test(
        writeValue: (Int, writeByte: (offset: Int, byte: Int) -> Unit) -> Int,
        readValue: (readByte: (offset: Int) -> Int) -> Int
    ) = test(size, getValue = { this[it] }, writeValue, readValue)

    private inline fun LongArray.test(
        writeValue: (Long, writeByte: (offset: Int, byte: Int) -> Unit) -> Int,
        readValue: (readByte: (offset: Int) -> Int) -> Long
    ) = test(size, getValue = { this[it] }, writeValue, readValue)

    @Test
    fun int16_BE() = NumbersDataSet.ints16.test(
        NumbersBigEndian::putInt16,
        NumbersBigEndian::getInt16
    )

    @Test
    fun int16_LE() = NumbersDataSet.ints16.test(
        NumbersLittleEndian::putInt16,
        NumbersLittleEndian::getInt16
    )

    @Test
    fun int24_BE() = NumbersDataSet.ints24.test(
        NumbersBigEndian::putInt24,
        NumbersBigEndian::getInt24
    )

    @Test
    fun int24_LE() = NumbersDataSet.ints24.test(
        NumbersLittleEndian::putInt24,
        NumbersLittleEndian::getInt24
    )

    @Test
    fun uint24_BE() = NumbersDataSet.uints24.test(
        NumbersBigEndian::putUInt24,
        NumbersBigEndian::getUInt24
    )

    @Test
    fun uint24_LE() = NumbersDataSet.uints24.test(
        NumbersLittleEndian::putUInt24,
        NumbersLittleEndian::getUInt24
    )

    @Test
    fun int32_BE() = NumbersDataSet.ints32.test(
        NumbersBigEndian::putInt32,
        NumbersBigEndian::getInt32
    )

    @Test
    fun int32_LE() = NumbersDataSet.ints32.test(
        NumbersLittleEndian::putInt32,
        NumbersLittleEndian::getInt32
    )

    @Test
    fun int40_BE() = NumbersDataSet.ints40.test(
        NumbersBigEndian::putInt40,
        NumbersBigEndian::getInt40
    )

    @Test
    fun int40_LE() = NumbersDataSet.ints40.test(
        NumbersLittleEndian::putInt40,
        NumbersLittleEndian::getInt40
    )

    @Test
    fun uint40_BE() = NumbersDataSet.uints40.test(
        NumbersBigEndian::putUInt40,
        NumbersBigEndian::getUInt40
    )

    @Test
    fun uint40_LE() = NumbersDataSet.uints40.test(
        NumbersLittleEndian::putUInt40,
        NumbersLittleEndian::getUInt40
    )

    @Test
    fun int48_BE() = NumbersDataSet.ints48.test(
        NumbersBigEndian::putInt48,
        NumbersBigEndian::getInt48
    )

    @Test
    fun int48_LE() = NumbersDataSet.ints48.test(
        NumbersLittleEndian::putInt48,
        NumbersLittleEndian::getInt48
    )

    @Test
    fun int56_BE() = NumbersDataSet.ints56.test(
        NumbersBigEndian::putInt56,
        NumbersBigEndian::getInt56
    )

    @Test
    fun int56_LE() = NumbersDataSet.ints56.test(
        NumbersLittleEndian::putInt56,
        NumbersLittleEndian::getInt56
    )

    @Test
    fun uint56_BE() = NumbersDataSet.uints56.test(
        NumbersBigEndian::putUInt56,
        NumbersBigEndian::getUInt56
    )

    @Test
    fun uint56_LE() = NumbersDataSet.uints56.test(
        NumbersLittleEndian::putUInt56,
        NumbersLittleEndian::getUInt56
    )

    @Test
    fun int64_BE() = NumbersDataSet.ints64.test(
        NumbersBigEndian::putInt64,
        NumbersBigEndian::getInt64
    )

    @Test
    fun int64_LE() = NumbersDataSet.ints64.test(
        NumbersLittleEndian::putInt64,
        NumbersLittleEndian::getInt64
    )
}