package io.kodec.buffers

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EqualsRangeTest: TestBase() {
    @Test
    fun whole_range() {
        test { createBuffer ->
            assertTrue(createBuffer().equalsRange(createBuffer()))
        }
    }

    @Test
    fun whole_sub_range() {
        test { createBuffer ->
            assertTrue(createBuffer(2, 7).equalsRange(createBuffer(2, 7)))
        }
    }

    @Test
    fun empty_buffers() {
        test { createBuffer ->
            assertTrue(createBuffer(emptyByteArray).equalsRange(createBuffer(emptyByteArray)))
            assertTrue(createBuffer(2, 2).equalsRange(createBuffer(3, 3)))
        }
    }

    @Test
    fun sub_ranges() {
        test { createBuffer ->
            assertTrue(createBuffer(2, 7).equalsRange(createBuffer(1, 5), thisOffset = 1, otherOffset = 2, size = 2))
            assertFalse(createBuffer(2, 7).equalsRange(createBuffer(1, 5), thisOffset = 2, otherOffset = 1, size = 2))
            assertFalse(createBuffer(2, 7).equalsRange(createBuffer(1, 5), thisOffset = 1, otherOffset = 1, size = 2))
        }
    }

    @Test
    fun omit_size() {
        test { createBuffer ->
            assertTrue(createBuffer(2, 7).equalsRange(createBuffer(1, 5), thisOffset = 1, otherOffset = 2))
            assertFalse(createBuffer(2, 7).equalsRange(createBuffer(1, 5), thisOffset = 2, otherOffset = 1))
            assertFalse(createBuffer(2, 7).equalsRange(createBuffer(1, 5), thisOffset = 1, otherOffset = 1))
        }
    }

    @Test
    fun invalid_inputs() {
        test { createBuffer ->
            assertFailsWith<IllegalArgumentException> {
                createBuffer(2, 7).equalsRange(createBuffer(1, 5), size = -1)
            }

            assertFailsWith<IllegalArgumentException> {
                createBuffer(2, 7).equalsRange(createBuffer(1, 5), otherOffset = -1)
            }

            assertFailsWith<IllegalArgumentException> {
                createBuffer(2, 7).equalsRange(createBuffer(1, 5), thisOffset = -1)
            }

            assertFailsWith<IllegalArgumentException> {
                createBuffer(2, 7).equalsRange(createBuffer(1, 5), otherOffset = 4, size = 1)
            }

            assertFailsWith<IllegalArgumentException> {
                createBuffer(2, 7).equalsRange(createBuffer(1, 5), thisOffset = 6, size = 1)
            }
        }
    }
}