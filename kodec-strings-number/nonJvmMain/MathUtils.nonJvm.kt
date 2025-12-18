package io.kodec

internal actual fun multiplyHigh(x: Long, y: Long): Long {
    // Use technique from section 8-2 of Henry S. Warren, Jr.,
    // Hacker's Delight (2nd ed.) (Addison Wesley, 2013), 173-174.
    val x1 = x shr 32
    val x2 = x and 0xFFFFFFFFL
    val y1 = y shr 32
    val y2 = y and 0xFFFFFFFFL

    val z2 = x2 * y2
    val t = x1 * y2 + (z2 ushr 32)
    var z1 = t and 0xFFFFFFFFL
    val z0 = t shr 32
    z1 += x2 * y1

    return x1 * y1 + z0 + (z1 shr 32)
}

internal actual fun unsignedMultiplyHigh(x: Long, y: Long): Long {
    // Compute via multiplyHigh() to leverage the intrinsic
    var result: Long = multiplyHigh(x, y)
    result += (y and (x shr 63)) // equivalent to `if (x < 0) result += y;`
    result += (x and (y shr 63)) // equivalent to `if (y < 0) result += x;`
    return result
}