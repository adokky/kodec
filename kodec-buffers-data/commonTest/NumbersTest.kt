package io.kodec.buffers

import io.kodec.NumbersDataSet
import kotlin.test.Test
import kotlin.test.assertTrue

class NumbersTest: AbstractDataBufferTest() {
    @Test
    fun int8() = test(
        NumbersDataSet::getInts8,
        DataBuffer::getInt8,
        { pos, v -> putInt8(pos, v); 1 }
    )

    @Test
    fun int16() = test(
        NumbersDataSet::getInts16,
        DataBuffer::getInt16,
        OutputDataBuffer::putInt16
    )

    @Test
    fun int24() = test(
        NumbersDataSet::getInts24,
        DataBuffer::getInt24,
        OutputDataBuffer::putInt24
    )

    @Test
    fun int32() = test(
        NumbersDataSet::getInts32,
        DataBuffer::getInt32,
        OutputDataBuffer::putInt32
    )

    @Test
    fun int40() = test(
        NumbersDataSet::getInts40,
        DataBuffer::getInt40,
        OutputDataBuffer::putInt40
    )

    @Test
    fun int48() = test(
        NumbersDataSet::getInts48,
        DataBuffer::getInt48,
        OutputDataBuffer::putInt48
    )

    @Test
    fun int56() = test(
        NumbersDataSet::getInts56,
        DataBuffer::getInt56,
        OutputDataBuffer::putInt56
    )

    @Test
    fun int64() = test(
        NumbersDataSet::getInts64,
        DataBuffer::getInt64,
        OutputDataBuffer::putInt64
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
        test(
            NumbersDataSet::getInts32,
            { pos ->
                val (bytesRead, result) = getVarInt32(pos, optimizePositive)
                assertTrue(bytesRead in 1..5)
                result
            },
            { pos, v -> putVarInt32(pos = pos, value = v, optimizePositive) }
        )
    }

    private fun varint64_test(optimizePositive: Boolean) {
        test(
            NumbersDataSet::getInts64,
            { pos ->
                val (bytesRead, result) = getVarInt64(pos, optimizePositive)
                assertTrue(bytesRead in 1..10)
                result
            },
            { pos, v -> putVarInt64(pos = pos, value = v, optimizePositive) }
        )
    }
}