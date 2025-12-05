package io.kodec.text

fun TextReader.readWholeString(builder: StringBuilder = StringBuilder()): String {
    val start = builder.length
    readCharsHeavyInline { c -> builder.append(c); true }
    return builder.substring(start)
}

fun TextReader.readStringUntil(
    ending: Char,
    includeEnding: Boolean = false,
    builder: StringBuilder = StringBuilder()
): String {
    val start = builder.length

    readCharsInline { c ->
        if (c == ending) {
            if (includeEnding) builder.append(c)
            false
        } else {
            builder.append(c)
            true
        }
    }

    return builder.substring(start)
}

/** Reads a string until [acceptChar] returns false or end of the stream is reached */
fun TextReader.readCharsWhile(acceptChar: (Char) -> Boolean) {
    readCharsHeavyInline(acceptChar)
}