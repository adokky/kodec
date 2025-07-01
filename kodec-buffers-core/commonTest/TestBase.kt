package io.kodec.buffers

import karamel.utils.enrichMessageOf

typealias BufferFactory = (ByteArray, Int, Int) -> Buffer

abstract class TestBase {
    protected fun testArray(): ByteArray = (1..9).map { (it * 11).toByte() }.toByteArray()

    private val factories = listOf<BufferFactory>(
        { a, s, e -> CustomBuffer(a, s, e) },
        { a, s, e -> a.asBuffer(s, e) }
    )

    protected fun test(body: (createBuffer: BufferFactory) -> Unit) {
        for ((i, factory) in factories.withIndex()) {
            enrichMessageOf<Throwable>({ "failed on $i" }) {
                body(factory)
            }
        }
    }

    protected operator fun BufferFactory.invoke(start: Int, end: Int) = this(testArray(), start, end)

    protected operator fun BufferFactory.invoke(arr: ByteArray) = this(arr, 0, arr.size)

    protected operator fun BufferFactory.invoke() = invoke(testArray())
}