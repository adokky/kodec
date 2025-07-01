package io.kodec.struct

class BitStructWriteContext32 @PublishedApi internal constructor(
    @PublishedApi internal var asInt: Int,
    private val rangeChecks: Boolean
) {
    infix fun BitStructField<Boolean>.set(value: Boolean) {
        asInt = asInt.or(this, value)
    }
    infix fun BitStructField<Byte>.set(value: Byte) {
        asInt = if (rangeChecks) asInt.orSafe(this, value) else (asInt.or(this, value))
    }
    infix fun BitStructField<Short>.set(value: Short) {
        asInt = if (rangeChecks) asInt.orSafe(this, value) else (asInt.or(this, value))
    }
    infix fun BitStructField<Int>.set(value: Int) {
        asInt = if (rangeChecks) asInt.orSafe(this, value) else (asInt.or(this, value))
    }
    infix fun BitStructField<Long>.set(value: Long) {
        asInt = if (rangeChecks) asInt.orSafe(this, value) else (asInt.or(this, value))
    }
}

class BitStructWriteContext64 @PublishedApi internal constructor(
    @PublishedApi internal var asLong: Long,
    private val rangeChecks: Boolean
) {
    infix fun BitStructField<Boolean>.set(value: Boolean) {
        asLong = asLong.or(this, value)
    }
    infix fun BitStructField<Byte>.set(value: Byte) {
        asLong = if (rangeChecks) asLong.orSafe(this, value) else asLong.or(this, value)
    }
    infix fun BitStructField<Short>.set(value: Short) {
        asLong = if (rangeChecks) asLong.orSafe(this, value) else asLong.or(this, value)
    }
    infix fun BitStructField<Int>.set(value: Int) {
        asLong = if (rangeChecks) asLong.orSafe(this, value) else asLong.or(this, value)
    }
    infix fun BitStructField<Long>.set(value: Long) {
        asLong = if (rangeChecks) asLong.orSafe(this, value) else asLong.or(this, value)
    }
}

inline fun composeInt(body: BitStructWriteContext32.() -> Unit): Int =
    BitStructWriteContext32(0, rangeChecks = false).apply(body).asInt

inline fun composeLong(body: BitStructWriteContext64.() -> Unit): Long =
    BitStructWriteContext64(0, rangeChecks = false).apply(body).asLong

inline fun composeIntSafe(body: BitStructWriteContext32.() -> Unit): Int =
    BitStructWriteContext32(0, rangeChecks = true).apply(body).asInt

inline fun composeLongSafe(body: BitStructWriteContext64.() -> Unit): Long =
    BitStructWriteContext64(0, rangeChecks = true).apply(body).asLong