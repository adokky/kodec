package io.kodec

import io.kodec.buffers.Buffer
import karamel.utils.ThreadLocal
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * A converter which can process an ASCII `String` representation
 * of a single or double precision floating point value into a
 * `float` or a `double`.
 */
internal object FloatingDecimalParsing {
    // bit mask and size of ByteArray buffer
    const val MAX_DIGITS: Int = 0.inv().ushr(32 - 7)

    @JvmStatic
    fun parseDouble(s: String): Double = prepareDouble(s).doubleValue()

    @JvmStatic
    fun parseFloat(s: String): Float = prepareFloat(s).floatValue()

    @JvmStatic
    fun parseDouble(s: String, start: Int, endExclusive: Int = s.length): Double =
        prepareDouble(s, start, endExclusive).doubleValue()

    @JvmStatic
    fun parseFloat(s: String, start: Int, endExclusive: Int = s.length): Float =
        prepareFloat(s, start, endExclusive).floatValue()

    @JvmStatic
    fun prepareDouble(
        s: CharSequence,
        onFormatError: DecodingErrorHandler<String> = DEFAULT_ERROR_HANDLER
    ): StringToFpConverter {
        val cache = TL_CACHE.get()
        cache.asciiWholeBuffer.string = s
        return prepareDouble(cache.asciiToBinaryBuffer, cache.asciiWholeBuffer, onFormatError = onFormatError)
    }

    @JvmStatic
    fun prepareDouble(
        s: CharSequence,
        start: Int, endExclusive: Int,
        onFormatError: DecodingErrorHandler<String> = DEFAULT_ERROR_HANDLER
    ): StringToFpConverter {
        val cache = TL_CACHE.get()
        cache.asciiBuffer.setString(s, start, endExclusive)
        return prepareDouble(cache.asciiToBinaryBuffer, cache.asciiBuffer, onFormatError = onFormatError)
    }

    @JvmStatic
    fun prepareFloat(
        s: CharSequence,
        onFormatError: DecodingErrorHandler<String> = DEFAULT_ERROR_HANDLER
    ): StringToFpConverter {
        val cache = TL_CACHE.get()
        cache.asciiWholeBuffer.string = s
        return prepareFloat(cache.asciiToBinaryBuffer, cache.asciiWholeBuffer, onFormatError = onFormatError)
    }

    @JvmStatic
    fun prepareFloat(
        s: CharSequence,
        start: Int, endExclusive: Int,
        onFormatError: DecodingErrorHandler<String> = DEFAULT_ERROR_HANDLER
    ): StringToFpConverter {
        val cache = TL_CACHE.get()
        cache.asciiBuffer.setString(s, start, endExclusive)
        return prepareFloat(cache.asciiToBinaryBuffer, cache.asciiBuffer, onFormatError = onFormatError)
    }

    @JvmStatic
    fun prepareDouble(
        input: Buffer,
        start: Int = 0,
        endExclusive: Int = input.size,
        onFormatError: DecodingErrorHandler<String> = DEFAULT_ERROR_HANDLER
    ): StringToFpConverter = prepareDouble(
        TL_CACHE.get().asciiToBinaryBuffer,
        input,
        start,
        endExclusive,
        onFormatError
    )

    @JvmStatic
    fun prepareFloat(
        input: Buffer,
        start: Int = 0,
        endExclusive: Int = input.size,
        onFormatError: DecodingErrorHandler<String> = DEFAULT_ERROR_HANDLER
    ): StringToFpConverter = prepareFloat(
        TL_CACHE.get().asciiToBinaryBuffer,
        input,
        start,
        endExclusive,
        onFormatError
    )

    private fun prepareDouble(
        buffer: ASCIIToBinaryBuffer,
        input: Buffer,
        start: Int = 0,
        endExclusive: Int = input.size,
        onFormatError: DecodingErrorHandler<String> = DEFAULT_ERROR_HANDLER
    ): StringToFpConverter = ASCIIToBinaryBuffer.parse(
        buffer = buffer,
        ix = ASCIIToBinaryBuffer.BINARY_64_IX,
        input = input,
        offset = start,
        end = endExclusive,
        onFormatError = onFormatError
    )

    private fun prepareFloat(
        buffer: ASCIIToBinaryBuffer,
        input: Buffer,
        start: Int = 0,
        endExclusive: Int = input.size,
        onFormatError: DecodingErrorHandler<String> = DEFAULT_ERROR_HANDLER
    ): StringToFpConverter = ASCIIToBinaryBuffer.parse(
        buffer = buffer,
        ix = ASCIIToBinaryBuffer.BINARY_32_IX,
        input = input,
        offset = start,
        end = endExclusive,
        onFormatError = onFormatError
    )

    @JvmField
    internal val DEFAULT_ERROR_HANDLER = DecodingErrorHandler { message: String ->
        throw NumberFormatException(message)
    }

    private class ThreadLocalCache {
        val asciiToBinaryBuffer = ASCIIToBinaryBuffer()
        val asciiWholeBuffer = WholeAsciiStringAsBuffer()
        val asciiBuffer = AsciiStringAsBuffer()
    }

    private val TL_CACHE = ThreadLocal { ThreadLocalCache() }
}