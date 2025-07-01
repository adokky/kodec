package io.kodec.buffers

private var rangeChecks = true

fun setRangeChecksEnabled(enabled: Boolean) {
    rangeChecks = enabled
}

internal actual val RANGE_CHECK_ENABLED: Boolean
    get() = rangeChecks