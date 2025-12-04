package io.kodec.text

import dev.dokky.pool.AbstractObjectPool
import karamel.utils.ThreadLocal

sealed class RandomAccessReaderCompanion<Reader: RandomAccessTextReader, Source: Any> {
    abstract fun startReadingFrom(input: Source, position: Int = 0): Reader

    protected abstract fun allocate(): Reader

    protected val threadLocal = ThreadLocal {
        object : AbstractObjectPool<Reader>(1..4) {
            override fun allocate() = this@RandomAccessReaderCompanion.allocate()
            override fun beforeRelease(value: Reader) {
                value.resetInput()
            }
        }
    }

    fun threadLocalPool(): AbstractObjectPool<Reader> = threadLocal.get()
}