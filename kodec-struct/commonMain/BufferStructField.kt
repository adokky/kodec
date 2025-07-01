package io.kodec.struct

import kotlin.jvm.JvmInline

/**
 * @property offset offset of this field *inside the structure* (relative to the structure offset)
 * @property size size of this field in bytes
 * @property backwardOffset offset from right to left for reading in usual forward order (left to right)
 * For example: `BufferStructField(offset=3, size=2).backwardOffset` is `-5`.
 *
 * Used when fields ordered backwards in buffer (right to left).
 * Subtract this offset from buffer size, and you will get the position of the field.
 */
@JvmInline
value class BufferStructField<T> private constructor(private val asLong: Long) {
    constructor(offset: Int, size: Int): this(
        (offset.toLong() or (size.toLong() shl OFFSET_BIT_NUM)).also {
            require(offset in 0..OFFSET_MAX) { "offset should be in range ${0..OFFSET_MAX}" }
            require(size in 0..SIZE_MAX) { "size should be in range ${0..SIZE_MAX}" }
        }
    )

    val offset: Int get() = (asLong and OFFSET_MASK).toInt()
    val size: Int get() = (asLong ushr OFFSET_BIT_NUM).toInt()
    val end: Int get() = offset + size
    val backwardOffset: Int get() = -end

    private companion object {
        const val SIZE_BIT_NUM = 16
        const val OFFSET_BIT_NUM = 64 - SIZE_BIT_NUM
        const val OFFSET_MASK = (0L.inv() ushr SIZE_BIT_NUM)

        const val SIZE_MAX   = (0L.inv() shl SIZE_BIT_NUM).inv()
        const val OFFSET_MAX = (0L.inv() shl OFFSET_BIT_NUM).inv()
    }
}