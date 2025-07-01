package io.kodec.buffers

internal actual fun nativeByteOrder(): ByteOrder = when (java.nio.ByteOrder.nativeOrder()) {
    java.nio.ByteOrder.BIG_ENDIAN -> ByteOrder.BigEndian
    else -> ByteOrder.LittleEndian
}