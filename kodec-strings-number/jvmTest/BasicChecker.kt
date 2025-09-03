package io.kodec

import kotlin.test.fail

open class BasicChecker {
    private var errors = 0

    open fun reset() {
        errors = 0
    }

    open fun addError(reason: String?): Boolean {
        ++errors
        if (reason != null) println(reason)
        return true
    }

    open fun addOnFail(expected: Boolean, reason: String?): Boolean {
        return if (expected) false else addError(reason)
    }

    open fun throwOnErrors() {
        if (errors > 0) fail("$errors errors found")
    }
}

open class DelegatingChecker(private val inner: BasicChecker): BasicChecker() {
    override fun reset() = inner.reset()

    override fun addError(reason: String?): Boolean =
        inner.addError(reason)

    override fun addOnFail(expected: Boolean, reason: String?): Boolean =
        inner.addOnFail(expected, reason)

    override fun throwOnErrors() = inner.throwOnErrors()
}

inline fun BasicChecker.test(body: () -> Unit) {
    reset()
    body()
    throwOnErrors()
}