package io.kodec.buffers

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EqualsRangeTest: TestBase() {
    @Test
    fun whole_range() {
        test {
            assertTrue(buffer().equalsRange(buffer()))
        }
    }

    @Test
    fun whole_sub_range() {
        test {
            assertTrue(buffer(2, 7).equalsRange(buffer(2, 7)))
        }
    }

    @Test
    fun empty_buffers() {
        test {
            assertTrue(buffer(emptyByteArray).equalsRange(buffer(emptyByteArray)))
            assertTrue(buffer(2, 2).equalsRange(buffer(3, 3)))
        }
    }

    @Test
    fun sub_ranges() {
        test {
            assertTrue(buffer(2, 7).equalsRange(buffer(1, 5), thisOffset = 1, otherOffset = 2, size = 2))
            assertFalse(buffer(2, 7).equalsRange(buffer(1, 5), thisOffset = 2, otherOffset = 1, size = 2))
            assertFalse(buffer(2, 7).equalsRange(buffer(1, 5), thisOffset = 1, otherOffset = 1, size = 2))
        }
    }

    @Test
    fun omit_size() {
        test {
            assertTrue(buffer(2, 7).equalsRange(buffer(1, 5), thisOffset = 1, otherOffset = 2))
            assertFalse(buffer(2, 7).equalsRange(buffer(1, 5), thisOffset = 2, otherOffset = 1))
            assertFalse(buffer(2, 7).equalsRange(buffer(1, 5), thisOffset = 1, otherOffset = 1))
        }
    }

    @Test
    fun invalid_inputs() {
        test {
            assertFailsWith<IllegalArgumentException> {
                buffer(2, 7).equalsRange(buffer(1, 5), size = -1)
            }

            assertFailsWith<IllegalArgumentException> {
                buffer(2, 7).equalsRange(buffer(1, 5), otherOffset = -1)
            }

            assertFailsWith<IllegalArgumentException> {
                buffer(2, 7).equalsRange(buffer(1, 5), thisOffset = -1)
            }

            assertFailsWith<IllegalArgumentException> {
                buffer(2, 7).equalsRange(buffer(1, 5), otherOffset = 4, size = 1)
            }

            assertFailsWith<IllegalArgumentException> {
                buffer(2, 7).equalsRange(buffer(1, 5), thisOffset = 6, size = 1)
            }
        }
    }
}