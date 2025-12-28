package io.kodec


object NumbersCommon {
    fun requireUInt24Range(v: Int) {
        if (v ushr 24 != 0) valueIsOutOfRange(v, 0, UINT_24_MAX)
    }

    fun requireUInt40Range(v: Long) {
        if (v ushr 40 != 0L) valueIsOutOfRange(v, 0, UINT_40_MAX)
    }

    fun requireUInt48Range(v: Long) {
        if (v ushr 48 != 0L) valueIsOutOfRange(v, 0, UINT_48_MAX)
    }

    fun requireUInt56Range(v: Long) {
        if (v ushr 56 != 0L) valueIsOutOfRange(v, 0, UINT_56_MAX)
    }


    fun requireInt24Range(v: Int) {
        if (v !in INT_24_MIN..INT_24_MAX) valueIsOutOfRange(v, INT_24_MIN, INT_24_MAX)
    }

    fun requireInt40Range(v: Long) {
        if (v !in INT_40_MIN..INT_40_MAX) valueIsOutOfRange(v, INT_40_MIN, INT_40_MAX)
    }

    fun requireInt48Range(v: Long) {
        if (v !in INT_48_MIN..INT_48_MAX) valueIsOutOfRange(v, INT_48_MIN, INT_48_MAX)
    }

    fun requireInt56Range(v: Long) {
        if (v !in INT_56_MIN..INT_56_MAX) valueIsOutOfRange(v, INT_56_MIN, INT_56_MAX)
    }


    internal fun valueIsOutOfRange(v: Long, min: Long, max: Long): Nothing {
        throw IllegalArgumentException("value $v is out of range $min..$max")
    }

    internal fun valueIsOutOfRange(v: Int, min: Int, max: Int): Nothing {
        valueIsOutOfRange(v.toLong(), min.toLong(), max.toLong())
    }


    const val UINT_24_MAX: Int = 0.inv() ushr 8
    const val UINT_40_MAX: Long = 0L.inv() ushr 24
    const val UINT_48_MAX: Long = 0L.inv() ushr 16
    const val UINT_56_MAX: Long = 0L.inv() ushr 8

    const val INT_24_MAX: Int = 0.inv() ushr 9
    const val INT_40_MAX: Long = 0L.inv() ushr 25
    const val INT_48_MAX: Long = 0L.inv() ushr 17
    const val INT_56_MAX: Long = 0L.inv() ushr 9

    const val INT_24_MIN: Int = INT_24_MAX.inv()
    const val INT_40_MIN: Long = INT_40_MAX.inv()
    const val INT_48_MIN: Long = INT_48_MAX.inv()
    const val INT_56_MIN: Long = INT_56_MAX.inv()
}