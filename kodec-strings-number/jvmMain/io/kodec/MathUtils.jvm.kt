package io.kodec

internal actual fun multiplyHigh(x: Long, y: Long): Long = Math.multiplyHigh(x, y)

internal actual fun unsignedMultiplyHigh(x: Long, y: Long): Long = JvmMathIntrinsics.unsignedMultiplyHigh(x, y)