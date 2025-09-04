package io.kodec.text

import karamel.utils.BitDescriptors
import karamel.utils.Bits32
import karamel.utils.asInt
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmOverloads

@JvmInline
value class CharToClassMapper<T: BitDescriptors> private constructor(private val charToBits: ByteArray) {
    constructor(): this(ByteArray(0xff))

    fun putBits(charCode: Int, bits: Bits32<T>) {
        val idx = charCode + 1
        if (idx !in charToBits.indices) error("char '${Char(charCode)}' can not be mapped")
        charToBits[idx] = bits.toInt().toByte()
    }

    fun putBits(char: Char, bits: Bits32<T>): Unit = putBits(char.code, bits)
    
    @JvmOverloads
    fun getBits(charCode: Int, default: Bits32<T> = Bits32(0)): Bits32<T> {
        val idx = charCode + 1
        var bits = default
        if (idx in charToBits.indices) bits = Bits32(charToBits[idx].asInt())
        return bits
    }

    fun hasClass(charCode: Int, c0: Bits32<T>): Boolean = c0 in getBits(charCode)

    fun hasClass(charCode: Int, c0: Bits32<T>, c1: Bits32<T>): Boolean =
        getBits(charCode).containsAll(c0, c1)

    fun hasClass(charCode: Int, c0: Bits32<T>, c1: Bits32<T>, c2: Bits32<T>): Boolean =
        getBits(charCode).containsAll(c0, c1, c2)

    fun hasClass(char: Char, c0: Bits32<T>): Boolean =
        hasClass(char.code, c0)

    fun hasClass(char: Char, c0: Bits32<T>, c1: Bits32<T>): Boolean =
        hasClass(char.code, c0, c1)

    fun hasClass(char: Char, c0: Bits32<T>, c1: Bits32<T>, c2: Bits32<T>): Boolean =
        hasClass(char.code, c0, c1, c2)

    fun hasAnyBit(charCode: Int, bits: Bits32<T>): Boolean = getBits(charCode).containsAny(bits)

    fun hasNoClass(charCode: Int, bits: Bits32<T>): Boolean = !hasClass(charCode, bits)
}
