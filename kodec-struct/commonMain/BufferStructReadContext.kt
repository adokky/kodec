package io.kodec.struct

import io.kodec.buffers.DataBuffer
import kotlin.jvm.JvmName

class BufferStructReadContext(private val buffer: DataBuffer, private val structOffset: Int) {
    @JvmName("getBoolField")
    fun get(field: BufferStructField<Boolean>): Boolean = buffer.get(structOffset + field.offset, field)
    @JvmName("getByteField")
    fun get(field: BufferStructField<Byte>): Byte = buffer.get(structOffset + field.offset, field)
    @JvmName("getShortField")
    fun get(field: BufferStructField<Short>): Short = buffer.get(structOffset + field.offset, field)
    @JvmName("getIntField")
    fun get(field: BufferStructField<Int>): Int = buffer.get(structOffset + field.offset, field)
    @JvmName("getLongField")
    fun get(field: BufferStructField<Long>): Long = buffer.get(structOffset + field.offset, field)
}

inline fun <R> DataBuffer.readBufferStruct(structOffset: Int, body: BufferStructReadContext.() -> R): R {
    return BufferStructReadContext(this, structOffset).body()
}