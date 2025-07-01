package io.kodec.buffers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class HashCodeTest {
    @Test
    fun ensure_hash_code_matches_standard_java() {
        ArrayBufferPairs.equalBufferPairs().forEach {
            assertEquals(it.second.toByteArray().contentHashCode(), it.first.hashCode())
        }
    }

    @Test
    fun equal_array_must_have_equal_hash_codes() {
        ArrayBufferPairs.equalBufferPairs().forEach {
            assertEquals(it.first.hashCode(), it.second.hashCode())
        }
    }

    @Test
    fun different_arrays_must_have_different_hash_codes() {
        ArrayBufferPairs.notEqualBufferPairs().forEach {
            assertNotEquals(it.first.hashCode(), it.second.hashCode())
        }
    }
}