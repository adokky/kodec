package io.kodec.text

sealed class AbstractSubString(hashCode: Int = 0): Comparable<AbstractSubString>, AutoCloseable {
    @PublishedApi internal var cachedString: String? = null
        private set
    @PublishedApi internal var cachedHashCode: Int = if (hashCode == 0) 0 else hashCode + 1
        private set

    abstract val start: Int
    abstract val end: Int

    val sourceLength: Int get() = end - start

    abstract fun copy(): AbstractSubString

    protected abstract fun iterateChars(body: (Char) -> Unit)

    protected open fun asString(): String = buildString(sourceLength) {
        this@AbstractSubString.forEach { append(it) }
    }

    fun forEach(body: (Char) -> Unit) {
        val cached = cachedString
        if (cached != null) {
            cached.forEach(body)
        } else {
            iterateChars(body)
        }
    }

    open fun toBoolean(): Boolean = toString().toBoolean()

    open fun toByte(): Byte {
        val v = toLong()
        checkRange(v, Byte.MIN_VALUE.toLong(), Byte.MAX_VALUE.toLong())
        return v.toByte()
    }

    open fun toShort(): Short {
        val v = toLong()
        checkRange(v, Short.MIN_VALUE.toLong(), Short.MAX_VALUE.toLong())
        return v.toShort()
    }

    open fun toInt(): Int {
        val v = toLong()
        checkRange(v, Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())
        return v.toInt()
    }

    private fun checkRange(v: Long, min: Long, max: Long) {
        if (v !in min..max) throw TextDecodingException("number $v is out of accepted bounds [$min, $max]")
    }

    open fun toLong(): Long = toString().toLong()

    open fun toFloat(): Float = toString().toFloat()

    open fun toDouble(): Double = toString().toDouble()

    open fun toChar(): Char {
        val s = toString()
        require(s.length == 1) { "'$this' can not be converted to Char" }
        return s[start]
    }

    final override fun toString(): String = cachedString ?: (asString().also {
        cachedString = it
        if (cachedHashCode == 0) cachedHashCode = it.hashCode() + 1
    })

    protected abstract fun computeHashCode(): Int // = toString().hashCode() + 1

    /** The hash code is always `toString().hashCode() + 1` */
    final override fun hashCode(): Int {
        if (cachedHashCode == 0) cachedHashCode = computeHashCode()
        return cachedHashCode
    }

    override fun equals(other: Any?): Boolean {
        if (other !is AbstractSubString) return false
        if (notEqualHashCode(other)) return false
        if (other::class === this::class && other.sourceLength != sourceLength) return false
        return toString() == other.toString()
    }

    override fun compareTo(other: AbstractSubString): Int {
        return toString().compareTo(other.toString())
    }

    protected fun notEqualHashCode(other: AbstractSubString): Boolean {
        val c0 = cachedHashCode
        val c1 = other.cachedHashCode
        return c0 * c1 != 0 && c0 != c1
    }

    internal fun resetCache(hashCode: Int = 0) {
        cachedHashCode = if (hashCode == 0) 0 else (hashCode + 1)
        cachedString = null
    }
}