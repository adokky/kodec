package io.kodec.buffers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SubBufferTest: TestBase() {
    @Test
    fun whole_buffer() {
        test {
            val buf = buffer()
            val sub1 = buf.subBuffer(0, buf.size)
            val sub2 = sub1.subBuffer(0, buf.size)
            assertEquals(buf, sub1)
            assertEquals(buf, sub2)
        }
    }

    @Test
    fun normal_operation() {
        test {
            val buf = buffer()

            val sub1 = buf.subBuffer(1, buf.size - 1)
            assertEquals(22, sub1[0])
            assertEquals(88, sub1[6])
            assertEquals(mutableBufferOf(22, 33, 44, 55, 66, 77, 88), sub1)

            val sub2 = sub1.subBuffer(1, sub1.size - 1)
            assertEquals(33, sub2[0])
            assertEquals(77, sub2[4])
            assertEquals(mutableBufferOf(33, 44, 55, 66, 77), sub2)
        }
    }

    @Test
    fun access_range_checks() {
        if (!RANGE_CHECK_ENABLED) return

        test {
            val buf = buffer()
            val sub1 = buf.subBuffer(1, buf.size - 1)
            val sub2 = sub1.subBuffer(1, sub1.size - 1)
            assertFailsWith<IndexOutOfBoundsException> { sub1[-1] }
            assertFailsWith<IndexOutOfBoundsException> { sub2[-1] }
            assertFailsWith<IndexOutOfBoundsException> { sub1[7] }
            assertFailsWith<IndexOutOfBoundsException> { sub2[5] }
        }
    }

    @Test
    fun instantiation_range_checks() {
        test {
            val buf = buffer()
            val sub1 = buf.subBuffer(1, buf.size - 1)
            assertFailsWith<IllegalArgumentException> { buf.subBuffer(1, buf.size + 1) }
            assertFailsWith<IllegalArgumentException> { buf.subBuffer(-1, buf.size) }
            assertFailsWith<IllegalArgumentException> { sub1.subBuffer(1, buf.size + 1) }
            assertFailsWith<IllegalArgumentException> { sub1.subBuffer(-1, buf.size) }
        }
    }
}