package io.kodec.java

import io.kodec.buffers.AbstractBuffer
import io.kodec.buffers.Buffer
import io.kodec.buffers.MutableBuffer
import io.kodec.buffers.asBuffer
import java.nio.ByteBuffer

class ByteBufferWrapper(var byteBuffer: ByteBuffer): AbstractBuffer(), MutableBuffer {
    override val size: Int get() = byteBuffer.limit()

    override fun set(pos: Int, byte: Int) {
        byteBuffer.put(pos, byte.toByte())
    }

    override fun set(pos: Int, byte: Byte) {
        byteBuffer.put(pos, byte)
    }

    override fun get(pos: Int): Int {
        return byteBuffer.get(pos).toInt() and 0xff
    }

    override fun getByte(pos: Int): Byte {
        return byteBuffer.get(pos)
    }

    override fun putBytes(pos: Int, bytes: ByteArray, startIndex: Int, endIndex: Int) {
        byteBuffer.put(pos, bytes, startIndex, endIndex - startIndex)
    }

    override fun subBuffer(start: Int, endExclusive: Int): Buffer {
        return ByteBufferWrapper(byteBuffer.slice(start, endExclusive - start))
    }

    override fun fill(byte: Int, start: Int, endExclusive: Int) {
        for (i in start..<endExclusive) {
            byteBuffer.put(i, byte.toByte())
        }
    }

    override fun toByteArray(start: Int, endExclusive: Int): ByteArray {
        val result = ByteArray(endExclusive - start)
        byteBuffer.get(start, result)
        return result
    }
}

fun ByteBuffer.asBuffer(): Buffer = when {
    hasArray() -> array().asBuffer()
    else -> ByteBufferWrapper(this)
}