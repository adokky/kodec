package io.kodec.buffers

object ReadBufferFunctions {
    val SingleByte = listOf(
        DataBuffer::get,
        DataBuffer::getInt8,
        DataBuffer::getBoolean
    )

    val MultiByte: List<(DataBuffer, Int) -> Any> = listOf(
        DataBuffer::getInt16,
        DataBuffer::getInt24,
        DataBuffer::getInt32,
        DataBuffer::getInt48,
        DataBuffer::getInt64,
        DataBuffer::getFloat32,
        DataBuffer::getFloat64,
        DataBuffer::getChar
    )

    val All = SingleByte + MultiByte
}