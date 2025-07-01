package io.kodec

import kotlin.test.Test
import kotlin.test.assertTrue

class NumbersDataSetTest {
    @Test
    fun bytes() {
        val generated = NumbersDataSet.getInts8().take(1000).toHashSet()
        assertTrue(generated.containsAll(listOf(
            Byte.MIN_VALUE,
            Byte.MAX_VALUE,
            0
        )))
    }

    @Test
    fun shorts() {
        val generated = NumbersDataSet.getInts16().take(1000).toHashSet()
        assertTrue(generated.containsAll(listOf(
            Short.MIN_VALUE,
            Short.MAX_VALUE,
            0
        )))
    }

    @Test
    fun ints() {
        val generated = NumbersDataSet.getInts32().take(1000).toHashSet()
        assertTrue(generated.containsAll(listOf(
            Int.MIN_VALUE,
            Int.MAX_VALUE,
            0
        )))
    }

    @Test
    fun longs() {
        val generated = NumbersDataSet.getInts64().take(1000).toHashSet()
        assertTrue(generated.containsAll(listOf(
            Long.MIN_VALUE,
            Long.MAX_VALUE,
            0L
        )))
    }

    @Test
    fun ints_24() {
        val generated = NumbersDataSet.getInts24().take(1000).toHashSet()
        assertTrue(generated.containsAll(listOf(0xFF_FF_FF, 0)))
        assertTrue(generated.all { it >= 0 })
    }

    @Test
    fun floats() {
        val generated = NumbersDataSet.getFloat32().take(1000).toHashSet()
        assertTrue(generated.size > 950)
        assertTrue(generated.containsAll(listOf(
            Float.MIN_VALUE,
            Float.MAX_VALUE,
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            Float.NaN,
            0f
        )))
    }

    @Test
    fun doubles() {
        val generated = NumbersDataSet.getFloat64().take(1000).toHashSet()
        assertTrue(generated.size > 950)
        assertTrue(generated.containsAll(listOf(
            Double.MIN_VALUE,
            Double.MAX_VALUE,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.NaN,
            0.0
        )))
    }
}