package io.kodec.buffers

import karamel.utils.enrichMessageOf

typealias BufferFactory = (ByteArray, Int, Int) -> MutableBuffer

abstract class TestBase {
    protected fun testArray(): ByteArray = (1..9).map { (it * 11).toByte() }.toByteArray()

    private val factories = mapOf<String, BufferFactory>(
        "CustomBuffer" to { a, s, e -> SimpleBuffer(a, s, e) },
        "SimpleBuffer" to { a, s, e -> CustomBuffer(a, s, e) },
        "ArrayBuffer" to { a, s, e -> a.asArrayBuffer(s, e) },
    )

    protected fun test(body: TestScope.() -> Unit) {
        for ((name, factory) in factories) {
            enrichMessageOf<Throwable>({ "failed on $name" }) {
                TestScope(factory).body()
            }
        }
    }

    protected operator fun BufferFactory.invoke(start: Int, end: Int) = this(testArray(), start, end)

    protected operator fun BufferFactory.invoke(arr: ByteArray) = this(arr, 0, arr.size)

    protected operator fun BufferFactory.invoke() = invoke(testArray())

    inner class TestScope(private val factory: BufferFactory) {
        fun buffer(data: ByteArray, start: Int, end: Int): MutableBuffer = factory(data, start, end)

        fun buffer(start: Int, end: Int): MutableBuffer = factory(testArray(), start, end)

        fun buffer(data: ByteArray): MutableBuffer = factory(data, 0, data.size)

        fun buffer(): MutableBuffer = buffer(testArray())
    }
}