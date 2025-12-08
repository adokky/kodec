package io.kodec.java

import io.kodec.buffers.ArrayBuffer
import io.kodec.buffers.Buffer
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class BufferInputStream(buffer: Buffer, start: Int = 0): InputStream() {
    var buffer: Buffer = buffer
        private set

    private var position = start
    private var mark = -1

    fun setBuffer(buffer: Buffer, start: Int = 0) {
        this.buffer = buffer
        position = start
        mark = -1
    }

    override fun read(): Int {
        val pos = position
        if (pos >= buffer.size) return -1
        return buffer[pos].also { position = pos + 1 }
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val size = buffer.size
        var pos = position
        if (position >= size) return -1

        val bytesRead = len.coerceAtMost(size - pos)
        for (i in off until (off + bytesRead)) {
            b[i] = buffer.getByte(pos++)
        }
        position = pos
        return bytesRead
    }

    override fun readAllBytes(): ByteArray {
        return buffer.toByteArray(start = position)
            .also { position = buffer.size }
    }

    override fun skip(n: Long): Long {
        val n = n.coerceAtLeast(0)

        val size = buffer.size
        val newPos = position.toLong() + n
        if (newPos < size) {
            position = newPos.toInt()
            return n
        }

        val result = size - position
        position = size
        return result.coerceAtLeast(0).toLong()
    }

    override fun available(): Int {
        return (buffer.size - position).coerceAtLeast(0)
    }

    override fun mark(readlimit: Int) {
        mark = position
    }

    override fun reset() {
        if (mark < 0) throw IOException("mark has not been called")
        position = mark
    }

    override fun markSupported(): Boolean = true

    override fun transferTo(out: OutputStream): Long = when (val buf = buffer) {
        is ArrayBuffer -> transferArrayBufferTo(buf, out)
        else -> super.transferTo(out)
    }

    private fun transferArrayBufferTo(buffer: ArrayBuffer, out: OutputStream): Long {
        val start = buffer.start + position
        val len = buffer.endExclusive - start
        out.write(buffer.array, start, len)
        return len.toLong()
    }
}