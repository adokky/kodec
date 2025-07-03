package io.kodec

@PublishedApi
internal object NumbersCommon {
    fun checkUInt24Range(v: Int) {
        if (v ushr 24 != 0) valueIsOutOfRange(v, 0, UINT_24_MAX)
    }

    fun checkUInt40Range(v: Long) {
        if (v ushr 40 != 0L) valueIsOutOfRange(v, 0, UINT_40_MAX)
    }

    fun checkUInt48Range(v: Long) {
        if (v ushr 48 != 0L) valueIsOutOfRange(v, 0, UINT_48_MAX)
    }

    fun checkUInt56Range(v: Long) {
        if (v ushr 56 != 0L) valueIsOutOfRange(v, 0, UINT_56_MAX)
    }


    fun checkInt24Range(v: Int) {
        if (v !in INT_24_MIN..INT_24_MAX) valueIsOutOfRange(v, INT_24_MIN, INT_24_MAX)
    }

    fun checkInt40Range(v: Long) {
        if (v !in INT_40_MIN..INT_40_MAX) valueIsOutOfRange(v, INT_40_MIN, INT_40_MAX)
    }

    fun checkInt48Range(v: Long) {
        if (v !in INT_48_MIN..INT_48_MAX) valueIsOutOfRange(v, INT_48_MIN, INT_48_MAX)
    }

    fun checkInt56Range(v: Long) {
        if (v !in INT_56_MIN..INT_56_MAX) valueIsOutOfRange(v, INT_56_MIN, INT_56_MAX)
    }


    fun valueIsOutOfRange(v: Long, min: Long, max: Long): Nothing {
        throw IllegalArgumentException("value $v is out of range $min..$max")
    }

    fun valueIsOutOfRange(v: Int, min: Int, max: Int): Nothing {
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

    const val INT_24_MIN: Int = 0.inv() shl 23
    const val INT_40_MIN: Long = 1L.inv() shl 39
    const val INT_48_MIN: Long = 1L.inv() shl 47
    const val INT_56_MIN: Long = 1L.inv() shl 55
}