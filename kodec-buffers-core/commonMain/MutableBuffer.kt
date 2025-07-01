package io.kodec.buffers

import karamel.utils.asInt
import kotlin.jvm.JvmOverloads

interface MutableBuffer {
    val size: Int

    /**
     * Only 8 right-most bits of [byte] are taken, the rest is ignored.
     */
    operator fun set(pos: Int, byte: Int)

    operator fun set(pos: Int, byte: Byte): Unit = set(pos, byte.asInt())
}

@JvmOverloads
fun MutableBuffer.putBytes(pos: Int, bytes: Buffer, startIndex: Int = 0, endIndex: Int = bytes.size) {
    if (this is ArrayBuffer && bytes is ArrayBuffer) {
        putBytes(pos, bytes, startIndex, endIndex)
    } else {
        putBytesCommon(pos, bytes, startIndex, endIndex)
    }
}

internal fun MutableBuffer.putBytesCommon(
    pos: Int,
    bytes: Buffer,
    startIndex: Int,
    endIndex: Int
) {
    for (i in 0 ..< (endIndex - startIndex)) {
        set(pos + i, bytes[startIndex + i])
    }
}

@JvmOverloads
fun MutableBuffer.fill(byte: Int, start: Int = 0, endExclusive: Int = size) {
    for (pos in start until endExclusive) {
        set(pos, byte)
    }
}

@JvmOverloads
fun MutableBuffer.clear(start: Int = 0, endExclusive: Int = size) {
    if (this is ArrayBuffer) clear(start, endExclusive) else fill(0, start, endExclusive)
}