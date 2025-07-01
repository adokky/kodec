package io.kodec

class DiagnosticContext<T>(
    val expected: T,
    val actual: T?,
    val encoded: ByteArray?,
    val error: Throwable
)

typealias FailHandler<T> = DiagnosticContext<T>.() -> Unit