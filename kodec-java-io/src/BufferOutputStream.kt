package io.kodec.java

import io.kodec.buffers.MutableBuffer
import java.io.OutputStream

class BufferOutputStream(buffer: MutableBuffer, start: Int): OutputStream() {
    private var position = start

    var buffer: MutableBuffer = buffer
        private set

    fun setBuffer(buffer: MutableBuffer, start: Int) {
        this.buffer = buffer
        position = start
    }

    override fun write(p0: Int) {
        buffer[position++] = p0
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        val pos = position
        buffer.putBytes(pos, b, startIndex = off, endIndex = off + len)
        position = pos + len
    }
}