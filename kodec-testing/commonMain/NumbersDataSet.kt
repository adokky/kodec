package io.kodec

import karamel.utils.asInt

object NumbersDataSet {
    val ints8: ByteArray =
        listOf(0, 1, 2, 3, 7, 8, 9, 63, 64, 65)
            .flatMap { listOf(it.toByte(), (-it).toByte()) }
            .plus(listOf(Byte.MIN_VALUE, Byte.MAX_VALUE))
            .toByteArray()

    val ints16: ShortArray =
        listOf<Short>(
            128, 129,
            0x191, -0x191,
            0xe13, -0xe13,
            0x1107, -0x1107,
            Short.MAX_VALUE,
            Short.MIN_VALUE,
            (Short.MAX_VALUE - 1).toShort(),
            (Short.MIN_VALUE + 1).toShort())
            .plus(ints8.map { it.asInt().toShort() })
            .toShortArray()

    val ints32: IntArray = intArrayOf(
        0x23_11_07, -0x23_11_07,
        0xFF_FF_01, -0xFF_FF_01,
        0xFF_FF_7F, -0xFF_FF_7F,
        0xFF_FF_FE, -0xFF_FF_FE,
        0xFF_FF_FF, -0xFF_FF_FF,
        0x59_23_11_07, -0x59_23_11_07,
        Int.MAX_VALUE,
        Int.MIN_VALUE,
        Int.MAX_VALUE - 1,
        Int.MIN_VALUE + 1
    ) + ints16.map { it.toInt() }

    val ints64: LongArray =
        longArrayOf(
            0x04_59_23_11_07L,
            0x04_89_59_23_11_07L,
            0x04_e1_89_59_23_11_07L,
            0x04_e1_a6_89_59_23_11_07L,
            0xFF_FF_FF_FFL,
            0xFF_FF_FF_FF_FFL,
            0xFF_FF_FF_FF_FF_FFL,
            0xFF_FF_FF_FF_FF_FF_FFL,
            0L.inv()
        ).flatMap { listOf(it, -it, 1 - it, it - 1, 3 - it, it - 3) }.toLongArray() +
        longArrayOf(
            Long.MIN_VALUE,
            Long.MIN_VALUE + 1,
            Long.MAX_VALUE,
            Long.MAX_VALUE - 1
        ) + ints32.map { it.toLong() }

    // partial ints

    val ints24: IntArray  = ints32.map { (it shl 8) shr  8 }.distinct().toIntArray()
    val uints24: IntArray = ints32.map { (it shl 8) ushr 8 }.distinct().toIntArray()

    private fun smallLongs(bits: Int): LongArray {
        val tail = 64 - bits
        return ints64.map { (it shl tail) shr tail }.distinct().toLongArray()
    }

    private fun ulongs(bits: Int): LongArray {
        val tail = 64 - bits
        return ints64.map { (it shl tail) ushr tail }.distinct().toLongArray()
    }

    val ints40: LongArray = smallLongs(40)
    val ints48: LongArray = smallLongs(48)
    val ints56: LongArray = smallLongs(56)

    val uints40: LongArray = ulongs(40)
    val uints48: LongArray = ulongs(48)
    val uints56: LongArray = ulongs(56)

    // floats

    val floats32 = getFloat32()
    val floats64 = getFloat64()

    fun getFloat32(
        max: Int = 1000,
        maxDiv: Int = 1000,
        special: Boolean = true
    ): FloatArray = buildList<Float>(max * maxDiv + 100) {
        add(0f)
        add(Float.MAX_VALUE)
        add(Float.MIN_VALUE)
        if (special) {
            add(Float.NEGATIVE_INFINITY)
            add(Float.POSITIVE_INFINITY)
            add(Float.NaN)
        }
        for (int in 1..max) {
            for (div in 1..maxDiv) {
                val f = int.toFloat() / div
                add(f)
                add(-f)
            }
        }
    }.toFloatArray()

    fun getFloat64(
        max: Int = 1000,
        maxDiv: Int = 1000,
        special: Boolean = true
    ): DoubleArray = buildList<Double>(max * maxDiv + 100) {
        add(0.0)
        add(Double.MAX_VALUE)
        add(Double.MIN_VALUE)
        add(-Double.MAX_VALUE)

        if (special) {
            add(Double.NEGATIVE_INFINITY)
            add(Double.POSITIVE_INFINITY)
            add(Double.NaN)
        }

        for (int in 1..max) {
            for (div in 1..maxDiv) {
                val f = int.toDouble() / div
                add(f)
                add(-f)
            }
        }

        // Values are from Paxson V, "A Program for Testing IEEE Decimal-Binary Conversion", tables 3 and 4
        add(8511030020275656.0)
        add(5201988407066741.0)
        add(6406892948269899.0)
        add(8431154198732492.0)
        add(6475049196144587.0)
        add(8274307542972842.0)
        add(5381065484265332.0)
        add(6761728585499734.0)
        add(7976538478610756.0)
        add(5982403858958067.0)
        add(5536995190630837.0)
        add(7225450889282194.0)
        add(7225450889282194.0)
        add(8703372741147379.0)
        add(8944262675275217.0)
        add(7459803696087692.0)
        add(6080469016670379.0)
        add(8385515147034757.0)
        add(7514216811389786.0)
        add(8397297803260511.0)
        add(6733459239310543.0)
        add(8091450587292794.0)
        add(6567258882077402.0)
        add(6712731423444934.0)
        add(6712731423444934.0)
        add(5298405411573037.0)
        add(5137311167659507.0)
        add(6722280709661868.0)
        add(5344436398034927.0)
        add(8369123604277281.0)
        add(8995822108487663.0)
        add(8942832835564782.0)
        add(8942832835564782.0)
        add(8942832835564782.0)
        add(6965949469487146.0)
        add(6965949469487146.0)
        add(6965949469487146.0)
        add(7487252720986826.0)
        add(5592117679628511.0)
        add(8887055249355788.0)
        add(6994187472632449.0)
        add(8797576579012143.0)
        add(7363326733505337.0)
        add(8549497411294502.0)

        fun add(v: Int) = add(v.toDouble())

        add(-342)
        add(-824)
        add(237)
        add(72)
        add(99)
        add(726)
        add(-456)
        add(-57)
        add(376)
        add(377)
        add(93)
        add(710)
        add(709)
        add(117)
        add(-1)
        add(-707)
        add(-381)
        add(721)
        add(-828)
        add(-345)
        add(202)
        add(-473)
        add(952)
        add(535)
        add(534)
        add(-957)
        add(-144)
        add(363)
        add(-169)
        add(-853)
        add(-780)
        add(-383)
        add(-384)
        add(-385)
        add(-249)
        add(-250)
        add(-251)
        add(548)
        add(164)
        add(665)
        add(690)
        add(588)
        add(272)
        add(-448)
    }.toDoubleArray()
}