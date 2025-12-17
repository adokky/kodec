package io.kodec.java

import io.kodec.buffers.AbstractBuffer
import io.kodec.buffers.Buffer
import io.kodec.buffers.MutableBuffer
import io.kodec.buffers.asBuffer
import java.nio.ByteBuffer

class ByteBufferWrapper(
    byteBuffer: ByteBuffer,
    start: Int = byteBuffer.position(),
    endExclusive: Int = byteBuffer.limit()
): AbstractBuffer(), MutableBuffer {
    var byteBuffer: ByteBuffer = byteBuffer
        private set
    var start: Int = start
        private set
    var endExclusive: Int = endExclusive
        private set

    init {
        validate()
    }

    fun reset(
        byteBuffer: ByteBuffer,
        start: Int = byteBuffer.position(),
        endExclusive: Int = byteBuffer.limit()
    ) {
        this.byteBuffer = byteBuffer
        this.start = start
        this.endExclusive = endExclusive
        validate()
    }

    private fun validate() {
        if(endExclusive > byteBuffer.limit() || start !in 0..endExclusive) {
            invalidRange()
        }
    }

    private fun invalidRange(): Nothing = throw IllegalArgumentException(
        "invalid range $start..<$endExclusive. buffer.limit()=${byteBuffer.limit()}"
    )

    override val size: Int get() = endExclusive - start

    override fun set(pos: Int, byte: Int) {
        byteBuffer.put(start + pos, byte.toByte())
    }

    override fun set(pos: Int, byte: Byte) {
        byteBuffer.put(start + pos, byte)
    }

    override fun get(pos: Int): Int {
        return byteBuffer.get(start + pos).toInt() and 0xff
    }

    override fun getByte(pos: Int): Byte {
        return byteBuffer.get(start + pos)
    }

    override fun putBytes(pos: Int, bytes: ByteArray, startIndex: Int, endIndex: Int) {
        byteBuffer.put(start + pos, bytes, startIndex, endIndex - startIndex)
    }

    override fun subBuffer(start: Int, endExclusive: Int): Buffer {
        return ByteBufferWrapper(byteBuffer.slice(this.start + start, endExclusive - start))
    }

    override fun fill(byte: Int, start: Int, endExclusive: Int) {
        for (i in start..<endExclusive) {
            byteBuffer.put(this.start + i, byte.toByte())
        }
    }

    override fun toByteArray(start: Int, endExclusive: Int): ByteArray {
        val result = ByteArray(endExclusive - start)
        byteBuffer.get(this.start + start, result)
        return result
    }
}

fun ByteBuffer.asBuffer(
    start: Int = position(),
    endExclusive: Int = limit()
): Buffer = when {
    hasArray() -> array().asBuffer(start, endExclusive)
    else -> ByteBufferWrapper(this, start, endExclusive)
}