package io.kodec.text

// TODO store character count
abstract class AbstractSubString(hashCode: Int = 0): Comparable<AbstractSubString> {
    abstract val start: Int
    abstract val end: Int

    val sourceLength: Int get() = end - start

    protected abstract fun asString(): String
    abstract fun copy(): AbstractSubString

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

    private var asString: String? = null

    final override fun toString(): String = asString ?: (asString().also {
        asString = it
        if (cachedHashCode == 0) cachedHashCode = it.hashCode()
    })

    protected open fun computeHashCode(): Int = toString().hashCode()

    private var cachedHashCode: Int = hashCode

    final override fun hashCode(): Int {
        if (cachedHashCode == 0) cachedHashCode = computeHashCode()
        return cachedHashCode
    }

    override fun equals(other: Any?): Boolean {
        if (other !is AbstractSubString) return false
        if (fastNonEqualityCheck(other)) return false
        if (other::class === this::class && other.sourceLength != sourceLength) return false
        return toString() == other.toString()
    }

    override fun compareTo(other: AbstractSubString): Int {
        return toString().compareTo(other.toString())
    }

    protected fun fastNonEqualityCheck(other: AbstractSubString): Boolean =
        cachedHashCode * other.cachedHashCode != 0 &&
        cachedHashCode != other.cachedHashCode

    internal fun resetCache(hashCode: Int = 0) {
        cachedHashCode = hashCode
        asString = null
    }
}

fun AbstractSubString.debugDescription(): String = "$start ..< $end, data: '${toString()}'"

abstract class AbstractMutableSubString(hashCode: Int = 0): AbstractSubString(hashCode) {
    abstract fun clear()
}