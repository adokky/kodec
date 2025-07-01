package io.kodec

import io.kodec.buffers.Buffer
import karamel.utils.IndexOutOfBoundsException

internal class AsciiStringAsBuffer(
    string: CharSequence,
    start: Int,
    end: Int
): Buffer {
    var string: CharSequence = string
        private set
    var start: Int = start
        private set
    var end: Int = end
        private set

    constructor(string: CharSequence): this(string, 0, string.length)
    constructor(): this("")

    override val size: Int get() = end - start

    init {
        validate()
    }

    private fun validate() {
        require(start in 0..string.length) { "invalid offset: $start" }
        require(end >= start) { "invalid range: $start..<$end" }
    }

    fun setString(
        string: CharSequence,
        offset: Int = 0,
        end: Int = string.length
    ) {
        this.string = string
        this.start = offset
        this.end = end
        validate()
    }

    override fun get(pos: Int): Int {
        if (pos !in 0 ..< size) throw IndexOutOfBoundsException(pos, size)
        return string[start + pos].code
    }
}

internal class WholeAsciiStringAsBuffer(var string: CharSequence): Buffer {
    constructor(): this("")
    override val size: Int get() = string.length
    override fun get(pos: Int): Int = string[pos].code
}