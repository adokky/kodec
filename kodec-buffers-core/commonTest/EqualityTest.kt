package io.kodec.buffers

import dev.adokky.eqtester.testEquality
import kotlin.test.Test

class EqualityTest: TestBase() {
    @Test
    fun equality() = test {
        testEquality {
            group { buffer() }
            group { buffer(byteArrayOf(-111, 0, 0, 3)) }
            group { buffer(byteArrayOf()) }
            group { buffer(2, 7) }
            group { buffer(2, 8) }
            group { buffer(3, 7) }
        }
    }
}