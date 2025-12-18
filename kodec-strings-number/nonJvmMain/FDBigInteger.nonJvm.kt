package io.kodec

internal actual fun compareUnsigned(a: Int, b: Int): Int {
    return when {
        a == b -> 0
        (a xor Int.MIN_VALUE) < (b xor Int.MIN_VALUE) -> -1
        else -> 1
    }
}