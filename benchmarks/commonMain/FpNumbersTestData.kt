package io.kodec

import io.kodec.buffers.ArrayDataBuffer
import io.kodec.buffers.asDataBuffer
import kotlin.jvm.JvmStatic
import kotlin.random.Random

class FpNumbersTestData private constructor(
    val buffer: ArrayDataBuffer,
    val numberSize: Int,
    val numberCount: Int
) {
    inline fun iterateNumbers(offset: Int, body: ArrayDataBuffer.(start: Int, end: Int) -> Unit) {
        repeat(numberCount) { i ->
            val start    = (offset + i * 2    ) * numberSize
            val frameEnd = (offset + i * 2 + 1) * numberSize
            var end = start
            while (end <= frameEnd && buffer[end] > 0) end++
            buffer.body(start, end)
        }
    }

    inline fun iterateFloatNumbers(body: ArrayDataBuffer.(start: Int, end: Int) -> Unit) {
        iterateNumbers(0, body)
    }

    inline fun iterateDoubleNumbers(body: ArrayDataBuffer.(start: Int, end: Int) -> Unit) {
        iterateNumbers(1, body)
    }

    fun toByteArray(): ByteArray {
        val result = ArrayDataBuffer(buffer.array.size + 8)
        result.putInt32(0, numberSize)
        result.putInt32(4, numberCount)
        result.putBytes(8, buffer)
        return result.array
    }

    companion object {
        @JvmStatic
        fun generate(numberSize: Int = 22, numberCount: Int = 3000): FpNumbersTestData {
            val buffer = ArrayDataBuffer(numberCount * numberSize * 2)
            val result = FpNumbersTestData(buffer, numberSize, numberCount)
            val random = Random(2)
            result.iterateNumbers(0) { start, _ ->
                buffer.putBytes(start, random.nextFloat().toString().encodeToByteArray())
            }
            result.iterateNumbers(1) { start, _ ->
                buffer.putBytes(start, random.nextDouble().toString().encodeToByteArray())
            }
            return result
        }

        @JvmStatic
        fun loadFromResource(resourceName: String): FpNumbersTestData? {
            val fileData = readResource(resourceName)?.asDataBuffer() ?: return null
            val numberSize = fileData.getInt32(0)
            val numberCount = fileData.getInt32(4)
            val data = fileData.subBuffer(8)
            return FpNumbersTestData(data, numberSize, numberCount)
        }
    }
}