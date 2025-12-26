package io.kodec

import kotlin.text.Charsets.ISO_8859_1

internal actual fun ByteArray.toAsciiString(size: Int, stringBuilder: StringBuilder?): String =
    String(this, 0, size, ISO_8859_1)