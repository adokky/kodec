package io.kodec.buffers

internal actual fun nativeByteOrder(): ByteOrder = when (java.nio.ByteOrder.nativeOrder()) {
    java.nio.ByteOrder.BIG_ENDIAN -> ByteOrder.BigEndian
    else -> ByteOrder.LittleEndian
}

fun ByteOrder.toJavaByteOrder(): java.nio.ByteOrder = when(this) {
    ByteOrder.BigEndian -> java.nio.ByteOrder.BIG_ENDIAN
    ByteOrder.LittleEndian -> java.nio.ByteOrder.LITTLE_ENDIAN
    ByteOrder.Native -> NativeByteOrder.toJavaByteOrder()
}