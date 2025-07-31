package io.kodec

import karamel.utils.unsafeCast
import kotlin.jvm.JvmField
import kotlin.jvm.JvmName

// For now, this all lives here in the somewhat unrelated string-ascii module.
// Until I have better ideas on where to move it.
// Package will stay the same anyway.

/**
 * Used as allocation-free way of handling decoding errors.
 *
 *      var result = reader.readUnsignedLong(pos, onFail = reader.errorContainer.prepare())
 *      reader.errorContainer.consumeError {
 *          // consumeError is inline function so we can freely assign
 *          // outer variables inside it without risk of allocation
 *          result = fallbackResult
 *      }
 *
 * If error is considered unrecoverable (or we simply does not
 * care about performance), we can allocate the handler right
 * at the call site and throw an exception inside it:
 *
 *      reader.readUnsignedLong(pos,
 *          onFail = { err: IntegerParsingError ->
 *              when(err) {
 *                  MalformedNumber -> throw NumberFormatException()
 *                  Overflow -> throw IllegalArgumentException(
 *                      "unsigned integer overflow")
 *              }
 *          }
 *      )
 */
fun interface DecodingErrorHandler<in T: Any> {
    operator fun invoke(cause: T)

    companion object {
        @JvmField
        val Ignore: DecodingErrorHandler<Any> = DecodingErrorHandler {}
    }
}

/**
 * Used as preallocated temporary storage for a single error.
 *
 * Example:
 *
 *     val container = ErrorContainer()
 *     val result = decodeSomething(onError = container.prepare())
 *     container.consumeError { cause ->
 *         // handle error
 *     }
 */
class ErrorContainer<E : Any>: DecodingErrorHandler<E>, AutoCloseable {
    @PublishedApi internal var cause: E? = null

    override fun invoke(cause: E) {
        this.cause = cause
    }

    fun <E : Any> prepare(): ErrorContainer<E> {
        close()
        return this.unsafeCast()
    }

    fun consumeError(): E? = cause?.also { close() }

    override fun close() { cause = null }

    fun isEmpty(): Boolean = cause == null

    inline fun consumeError(body: (E) -> Unit) {
        cause?.let {
            cause = null
            body(it)
        }
    }
}

@JvmName("handleAnyThrowable")
operator fun DecodingErrorHandler<Any>.invoke(throwable: Throwable): Unit =
    invoke(throwable.cause ?: throwable)

@JvmName("handleThrowable")
operator fun <T: Throwable> DecodingErrorHandler<T>.invoke(throwable: T): Unit =
    invoke(throwable)

@JvmName("handleAnyDecodingError")
operator fun DecodingErrorHandler<Any>.invoke(cause: DecodingErrorWithMessage): Unit =
    invoke(cause)

@JvmName("handleDecodingError")
operator fun <T: DecodingErrorWithMessage> DecodingErrorHandler<T>.invoke(cause: T): Unit =
    invoke(cause)

interface DecodingErrorWithMessage {
    val message: String
}