package io.kodec.struct

import io.kodec.buffers.OutputDataBuffer
import kotlin.jvm.JvmName

class BufferStructWriteContext(private val buffer: OutputDataBuffer, private val structOffset: Int) {
    @JvmName("putBoolField")
    fun put(field: BufferStructField<Boolean>, value: Boolean): Unit = buffer.put(structOffset + field.offset, field, value)
    @JvmName("putByteField")
    fun put(field: BufferStructField<Byte>, value: Byte): Unit = buffer.put(structOffset + field.offset, field, value)
    @JvmName("putShortField")
    fun put(field: BufferStructField<Short>, value: Short): Unit = buffer.put(structOffset + field.offset, field, value)
    @JvmName("putIntField")
    fun put(field: BufferStructField<Int>, value: Int): Unit = buffer.put(structOffset + field.offset, field, value)
    @JvmName("putLongField")
    fun put(field: BufferStructField<Long>, value: Long): Unit = buffer.put(structOffset + field.offset, field, value)
}

inline fun <R> OutputDataBuffer.writeBufferStruct(structOffset: Int, body: BufferStructWriteContext.() -> R): R {
    return BufferStructWriteContext(this, structOffset).body()
}