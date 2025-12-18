package io.kodec

internal actual fun multiplyHigh(x: Long, y: Long): Long = Math.multiplyHigh(x, y)

// TODO JVM 18 has an intrinsic
internal actual fun unsignedMultiplyHigh(x: Long, y: Long): Long = unsignedMultiplyHighCommon(x, y)