package io.kodec

fun <T: String?> StringsTestFailHandler(actualStringStart: Int = -1, actualStringEnd: Int = -1): FailHandler<T> = {
    println("failed on string: '$expected'")

    val expectedBytes = expected!!.encodeToByteArray()

    fun ByteArray.contentToStringUnsigned(): String =
        joinToString(prefix = "[", postfix = "]") { it.toUByte().toString() }

    var encodedStringBytes: ByteArray? = encoded

    if (encoded != null) {
        encodedStringBytes = encoded.copyOfRange(
            actualStringStart.let { if (it == -1) 0 else actualStringStart },
            actualStringEnd.let { if (it == -1) encoded.size else actualStringEnd }
        )
    }

    if (encodedStringBytes.contentEquals(expectedBytes)) {
        println("encoding worked out correctly, problem in decoding")
    } else {
        println("expected: " + expectedBytes.contentToStringUnsigned())
        println("actual:   " + encodedStringBytes?.contentToStringUnsigned())
    }

    throw error
}