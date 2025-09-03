package io.kodec

internal actual fun ByteArray.toAsciiString(size: Int, stringBuilder: StringBuilder?): String {
    val sb = stringBuilder ?: StringBuilder(size)
    repeat(size) { i ->
        sb.append(this[i].toInt().toChar())
    }
    return sb.toString()
}