package io.kodec

import karamel.utils.asInt
import kotlin.math.absoluteValue
import kotlin.sequences.sequenceOf

object NumbersDataSet {
    fun getInts8(): Sequence<Byte> = sequenceOf(0, 1, 2, 3, 7, 8, 9, 63, 64, 65)
        .flatMap { sequenceOf(it.toByte(), (-it).toByte()) } +
            sequenceOf(Byte.MIN_VALUE, Byte.MAX_VALUE)

    fun getInts16(): Sequence<Short> =
        sequenceOf(128, 129,
            Short.MAX_VALUE,
            Short.MIN_VALUE,
            (Short.MAX_VALUE - 1).toShort(),
            (Short.MIN_VALUE + 1).toShort()
        ) + getInts8().map { it.asInt().toShort() }

    fun getInts24(): Sequence<Int> = sequenceOf(
        0xFF_FF_FF,
        0xFF_FF_FF - 1,
        0xFF_FF_7F,
        0xFF_FF_01
    ) + getInts16().map { it.toInt().absoluteValue }.distinct()

    fun getInts32(): Sequence<Int> = sequenceOf(
        Int.MAX_VALUE,
        Int.MIN_VALUE,
        Int.MAX_VALUE - 1,
        Int.MIN_VALUE + 1
    ) + getInts24()

    fun getInts40(): Sequence<Long> = sequenceOf(0xFF_FFFF_FFFFL, 0xFF_FFFF_FFFFL - 1) +
        getInts32().map { it.toLong().absoluteValue }.distinct()

    fun getInts48(): Sequence<Long> = sequenceOf(0xFFFF_FFFF_FFFFL, 0xFFFF_FFFF_FFFFL - 1) + getInts40()

    fun getInts56(): Sequence<Long> = sequenceOf(0xFF_FFFF_FFFF_FFFFL, 0xFF_FFFF_FFFF_FFFFL - 1) + getInts48()

    fun getInts64(): Sequence<Long> = sequenceOf(
        Long.MIN_VALUE + 1,
        Long.MAX_VALUE - 1,
        Long.MAX_VALUE,
        Long.MIN_VALUE
    ) + getInts48()

    fun getFloat32(
        max: Int = 1000,
        maxDiv: Int = 1000,
        special: Boolean = true
    ): Sequence<Float> = sequence {
        yield(0f)
        yield(Float.MAX_VALUE)
        yield(Float.MIN_VALUE)
        if (special) {
            yield(Float.NEGATIVE_INFINITY)
            yield(Float.POSITIVE_INFINITY)
            yield(Float.NaN)
        }
        for (int in 1..max) {
            for (div in 1..maxDiv) {
                val f = int.toFloat() / div
                yield(f)
                yield(-f)
            }
        }
    }

    fun getFloat64(
        max: Int = 1000,
        maxDiv: Int = 1000,
        special: Boolean = true
    ): Sequence<Double> = sequence {
        yield(0.0)
        yield(Double.MAX_VALUE)
        yield(Double.MIN_VALUE)
        if (special) {
            yield(Double.NEGATIVE_INFINITY)
            yield(Double.POSITIVE_INFINITY)
            yield(Double.NaN)
        }
        for (int in 1..max) {
            for (div in 1..maxDiv) {
                val f = int.toDouble() / div
                yield(f)
                yield(-f)
            }
        }
    }
}