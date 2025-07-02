package io.kodec.buffers

fun mutableBufferOf(vararg bytes: Byte): MutableBuffer = bytes.asArrayBuffer()

fun bufferOf(vararg bytes: Byte): Buffer = bytes.asArrayBuffer()


fun ByteArray.asMutableBuffer(start: Int = 0, endExclusive: Int = this.size): MutableBuffer =
    asArrayBuffer(start, endExclusive)

fun ByteArray.asBuffer(start: Int = 0, endExclusive: Int = this.size): Buffer =
    asArrayBuffer(start, endExclusive)