package io.kodec.struct

import io.kodec.buffers.*
import karamel.utils.assert
import kotlin.jvm.JvmName

// READ

@JvmName("getByteField")
fun DataBuffer.get(structOffset: Int, field: BufferStructField<Boolean>): Boolean =
    getBoolean(structOffset + field.offset)

@JvmName("getByteField")
fun DataBuffer.get(structOffset: Int, field: BufferStructField<Byte>): Byte =
    getInt8(structOffset + field.offset)

@JvmName("getShortField")
fun DataBuffer.get(structOffset: Int, field: BufferStructField<Short>): Short {
    return if (field.size == 1)
        get(structOffset + field.offset).toShort() else
        getInt16(structOffset + field.offset)
}

@JvmName("getIntField")
fun DataBuffer.get(structOffset: Int, field: BufferStructField<Int>): Int =
    getSizedAsInt32(structOffset + field.offset, sizeInBytes = field.size)

@JvmName("getLongField")
fun DataBuffer.get(structOffset: Int, field: BufferStructField<Long>): Long =
    getSizedAsInt64(structOffset + field.offset, sizeInBytes = field.size)

@JvmName("getStringField")
fun DataBuffer.get(structOffset: Int, field: BufferStructField<String>): String =
    getStringAscii(structOffset + field.offset, length = field.size)

// READ BACKWARDS

@JvmName("getByteFieldBackwards")
fun DataBuffer.getBackwards(structOffset: Int, field: BufferStructField<Boolean>): Boolean =
    getBoolean(structOffset + field.backwardOffset)

@JvmName("getByteFieldBackwards")
fun DataBuffer.getBackwards(structOffset: Int, field: BufferStructField<Byte>): Byte =
    getInt8(structOffset + field.backwardOffset)

@JvmName("getShortFieldBackwards")
fun DataBuffer.getBackwards(structOffset: Int, field: BufferStructField<Short>): Short {
    return if (field.size == 1)
        get(structOffset + field.backwardOffset).toShort() else
        getInt16(structOffset + field.backwardOffset)
}

@JvmName("getIntFieldBackwards")
fun DataBuffer.getBackwards(structOffset: Int, field: BufferStructField<Int>): Int =
    getSizedAsInt32(structOffset + field.backwardOffset, sizeInBytes = field.size)

@JvmName("getLongFieldBackwards")
fun DataBuffer.getBackwards(structOffset: Int, field: BufferStructField<Long>): Long =
    getSizedAsInt64(structOffset + field.backwardOffset, sizeInBytes = field.size)

@JvmName("getStringFieldBackwards")
fun DataBuffer.getBackwards(structOffset: Int, field: BufferStructField<String>): String =
    getStringAscii(structOffset + field.backwardOffset, length = field.size)

// WRITE

@JvmName("putBoolField")
fun OutputDataBuffer.put(structOffset: Int, field: BufferStructField<Boolean>, value: Boolean) {
    putBoolean(structOffset + field.offset, value)
}

@JvmName("putByteField")
fun OutputDataBuffer.put(structOffset: Int, field: BufferStructField<Byte>, value: Byte) {
    putInt8(structOffset + field.offset, value)
}

@JvmName("putShortField")
fun OutputDataBuffer.put(structOffset: Int, field: BufferStructField<Short>, value: Short) {
    if (field.size == 1) {
        set(structOffset + field.offset, value.toInt())
    } else {
        putInt16(structOffset + field.offset, value)
    }
}

@JvmName("putIntField")
fun OutputDataBuffer.put(structOffset: Int, field: BufferStructField<Int>, value: Int) {
    putSizedInt32(structOffset + field.offset, sizeInBytes = field.size, value)
}

@JvmName("putLongField")
fun OutputDataBuffer.put(structOffset: Int, field: BufferStructField<Long>, value: Long) {
    putSizedInt64(structOffset + field.offset, sizeInBytes = field.size, value)
}

@JvmName("putStringField")
fun OutputDataBuffer.put(structOffset: Int, field: BufferStructField<String>, value: String) {
    require(value.length == field.size)
    assert { value.all { it.code <= 127 } }
    putStringAscii(structOffset + field.offset, value)
}

// WRITE BACKWARD

@JvmName("putBoolFieldBackwards")
fun OutputDataBuffer.putBackwards(structOffset: Int, field: BufferStructField<Boolean>, value: Boolean) {
    putBoolean(structOffset + field.backwardOffset, value)
}

@JvmName("putByteFieldBackwards")
fun OutputDataBuffer.putBackwards(structOffset: Int, field: BufferStructField<Byte>, value: Byte) {
    putInt8(structOffset + field.backwardOffset, value)
}

@JvmName("putShortFieldBackwards")
fun OutputDataBuffer.putBackwards(structOffset: Int, field: BufferStructField<Short>, value: Short) {
    if (field.size == 1) {
        set(structOffset + field.backwardOffset, value.toInt())
    } else {
        putInt16(structOffset + field.backwardOffset, value)
    }
}

@JvmName("putIntFieldBackwards")
fun OutputDataBuffer.putBackwards(structOffset: Int, field: BufferStructField<Int>, value: Int) {
    putSizedInt32(structOffset + field.backwardOffset, sizeInBytes = field.size, value)
}

@JvmName("putLongFieldBackwards")
fun OutputDataBuffer.putBackwards(structOffset: Int, field: BufferStructField<Long>, value: Long) {
    putSizedInt64(structOffset + field.backwardOffset, sizeInBytes = field.size, value)
}

@JvmName("putStringFieldBackwards")
fun OutputDataBuffer.putBackwards(structOffset: Int, field: BufferStructField<String>, value: String) {
    require(value.length == field.size)
    assert { value.all { it.code <= 127 } }
    putStringAscii(structOffset + field.backwardOffset, value)
}