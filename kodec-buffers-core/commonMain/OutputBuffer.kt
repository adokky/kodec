package io.kodec.buffers

import karamel.utils.asInt

interface OutputBuffer {
    val size: Int

    /**
     * @param byte the unsigned byte value (`0-255`). Only 8 right-most bits are taken, the rest is ignored
     */
    operator fun set(pos: Int, byte: Int)

    operator fun set(pos: Int, byte: Byte): Unit = set(pos, byte.asInt())

    fun putBytes(pos: Int, bytes: Buffer, startIndex: Int = 0, endIndex: Int = bytes.size) {
        if (this is ArrayBuffer && bytes is ArrayBuffer) {
            putBytes(pos, bytes, startIndex, endIndex)
        } else {
            putBytesCommon(pos, bytes, startIndex, endIndex)
        }
    }

    fun fill(byte: Int, start: Int = 0, endExclusive: Int = size) {
        for (pos in start until endExclusive) {
            set(pos, byte)
        }
    }

    fun clear(start: Int = 0, endExclusive: Int = size) {
        fill(0, start, endExclusive)
    }
}

internal fun OutputBuffer.putBytesCommon(
    pos: Int,
    bytes: Buffer,
    startIndex: Int,
    endIndex: Int
) {
    for (i in 0 ..< (endIndex - startIndex)) {
        set(pos + i, bytes[startIndex + i])
    }
}