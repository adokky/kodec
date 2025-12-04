package io.kodec.text

import io.kodec.StringsASCII
import io.kodec.StringsUTF16
import karamel.utils.asLong
import kotlin.jvm.JvmInline

@JvmInline
value class CodePointAndSize(val asLong: Long) {
    constructor(codepoint: Int, size: Int):
            this(codepoint.asLong() or (size.toLong() shl 32))

    val codepoint: Int get() = (asLong and 0xff_ff_ff_ff).toInt()
    val size: Int get() = (asLong shr 32).toInt()

    operator fun component1(): Int = codepoint
    operator fun component2(): Int = size

    override fun toString(): String = buildString(2) {
        StringsUTF16.getChars(codepoint) {
            append(it)
        }
    }

    companion object {
        val EOF: CodePointAndSize = CodePointAndSize(-1, 0)
        val INVALID: CodePointAndSize = CodePointAndSize(StringsASCII.INVALID_BYTE_PLACEHOLDER.code, 1)
    }
}