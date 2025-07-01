package io.kodec.buffers

internal actual val RANGE_CHECK_ENABLED: Boolean
    get() = System.getProperty("io.kodec.rangeChecks")?.toBoolean() ?: true