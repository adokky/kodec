package io.kodec.buffers

enum class ByteOrder {
    BigEndian,
    LittleEndian,
    Native
}

internal expect fun nativeByteOrder(): ByteOrder

val NativeByteOrder: ByteOrder = nativeByteOrder().also {
    require(it != ByteOrder.Native)
}
