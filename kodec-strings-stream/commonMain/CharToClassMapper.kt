package io.kodec.text

import karamel.utils.BitDescriptors
import karamel.utils.Bits32
import karamel.utils.asInt
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmOverloads

@JvmInline
value class CharToClassMapper<T: BitDescriptors> private constructor(private val charToBits: ByteArray) {
    constructor(): this(ByteArray(0xff))

    fun assignClasses(charCode: Int, bits: Bits32<T>) {
        val idx = charCode + 1
        if (idx !in charToBits.indices) error("char '${Char(charCode)}' can not be mapped")
        charToBits[idx] = bits.toInt().toByte()
    }

    fun assignClasses(char: Char, bits: Bits32<T>): Unit = assignClasses(char.code, bits)
    
    @JvmOverloads
    fun getClasses(charCode: Int, default: Bits32<T> = Bits32(0)): Bits32<T> =
        when (val idx = charCode + 1) {
            in charToBits.indices -> Bits32(charToBits[idx].asInt())
            else -> default
        }

    fun hasClass(charCode: Int, c0: Bits32<T>): Boolean = c0 in getClasses(charCode)

    fun hasClass(charCode: Int, c0: Bits32<T>, c1: Bits32<T>): Boolean =
        getClasses(charCode).containsAll(c0, c1)

    fun hasClass(charCode: Int, c0: Bits32<T>, c1: Bits32<T>, c2: Bits32<T>): Boolean =
        getClasses(charCode).containsAll(c0, c1, c2)

    fun hasClass(char: Char, c0: Bits32<T>): Boolean =
        hasClass(char.code, c0)

    fun hasClass(char: Char, c0: Bits32<T>, c1: Bits32<T>): Boolean =
        hasClass(char.code, c0, c1)

    fun hasClass(char: Char, c0: Bits32<T>, c1: Bits32<T>, c2: Bits32<T>): Boolean =
        hasClass(char.code, c0, c1, c2)

    fun hasAnyClass(charCode: Int, bits: Bits32<T>): Boolean = getClasses(charCode).containsAny(bits)

    fun hasNoClass(charCode: Int, bits: Bits32<T>): Boolean = !hasClass(charCode, bits)
}
