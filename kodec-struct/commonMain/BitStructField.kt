package io.kodec.struct

import karamel.utils.ShortPair
import kotlin.jvm.JvmInline

/**
 * @property size size of this field in bits
 */
@JvmInline
value class BitStructField<T> private constructor(private val shortPair: ShortPair) {
    constructor(offset: Int, size: Int): this(ShortPair(offset, size))

    val offset: Int get() = shortPair.shortAsInt1
    val size: Int get() = shortPair.shortAsInt2
    val end: Int get() = offset + size

    val mask32: Int  get() = 0 .inv() ushr (32 - size)
    val mask64: Long get() = 0L.inv() ushr (64 - size)
}