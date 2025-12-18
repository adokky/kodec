package io.kodec

actual fun readResource(name: String): ByteArray? {
    return FpParsingBenchmark::class.java.classLoader.getResourceAsStream(name)?.readAllBytes()
}