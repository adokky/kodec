package io.kodec.buffers

import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
internal actual fun nativeByteOrder(): ByteOrder {
    return if (Platform.isLittleEndian) ByteOrder.LittleEndian else ByteOrder.BigEndian
}