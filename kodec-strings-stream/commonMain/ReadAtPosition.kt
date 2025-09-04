package io.kodec.text

inline fun <T: RandomAccessTextReader, R> T.readAtPosition(position: Int, read: T.() -> R): R {
    val old = this.position
    this.position = position
    try {
        return read()
    } finally {
        this.position = old
    }
}

inline fun <T: RandomAccessTextReader, R> T.readAtCurrentPosition(read: T.() -> R): R {
    val old = this.position
    try {
        return read()
    } finally {
        this.position = old
    }
}