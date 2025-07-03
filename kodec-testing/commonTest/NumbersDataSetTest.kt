package io.kodec

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class NumbersDataSetTest {
    @Test
    fun bytes() {
        val generated = NumbersDataSet.ints8.toHashSet()
        assertContains(generated, 0)
        assertContains(generated, Byte.MIN_VALUE)
        assertContains(generated, Byte.MAX_VALUE)
    }

    @Test
    fun shorts() {
        val generated = NumbersDataSet.ints16.toHashSet()
        assertContains(generated, 0)
        assertContains(generated, Short.MIN_VALUE)
        assertContains(generated, Short.MAX_VALUE)
    }

    @Test
    fun ints() {
        val generated = NumbersDataSet.ints32.toHashSet()
        assertContains(generated, 0)
        assertContains(generated, Int.MIN_VALUE)
        assertContains(generated, Int.MAX_VALUE)
    }

    @Test
    fun longs() {
        val generated = NumbersDataSet.ints64.toHashSet()
        assertContains(generated, 0)
        assertContains(generated, Long.MIN_VALUE)
        assertContains(generated, Long.MAX_VALUE)
    }

    @Test
    fun uint_24() {
        val generated = NumbersDataSet.uints24.toHashSet()
        assertContains(generated, 0)
        assertContains(generated, 0xFF_FF_FF)
        assertTrue(generated.all { it >= 0 })
    }

    @Test
    fun ulong_40() {
        val generated = NumbersDataSet.uints40.toHashSet()
        assertContains(generated, 0)
        assertContains(generated, 0xFF_FF_FF_FF_FFL)
        assertTrue(generated.all { it >= 0 })
    }

    @Test
    fun ulong_48() {
        val generated = NumbersDataSet.uints48.toHashSet()
        assertContains(generated, 0)
        assertContains(generated, 0xFF_FF_FF_FF_FFL)
        assertContains(generated, 0xFF_FF_FF_FF_FF_FFL)
        assertTrue(generated.all { it >= 0 })
    }

    @Test
    fun ulong_56() {
        val generated = NumbersDataSet.uints56.toHashSet()
        assertContains(generated, 0)
        assertContains(generated, 0xFF_FF_FF_FF_FFL)
        assertContains(generated, 0xFF_FF_FF_FF_FF_FFL)
        assertContains(generated, 0xFF_FF_FF_FF_FF_FF_FFL)
        assertTrue(generated.all { it >= 0 })
    }

    @Test
    fun floats() {
        val generated = NumbersDataSet.floats32.toHashSet()
        assertTrue(generated.size > 950)
        assertContains(generated, Float.MIN_VALUE)
        assertContains(generated, Float.MAX_VALUE)
        assertContains(generated, Float.POSITIVE_INFINITY)
        assertContains(generated, Float.NEGATIVE_INFINITY)
        assertContains(generated, Float.NaN)
        assertContains(generated, 0f)
    }

    @Test
    fun doubles() {
        val generated = NumbersDataSet.floats64.toHashSet()
        assertTrue(generated.size > 950)
        assertContains(generated, Double.MIN_VALUE)
        assertContains(generated, Double.MAX_VALUE)
        assertContains(generated, Double.POSITIVE_INFINITY)
        assertContains(generated, Double.NEGATIVE_INFINITY)
        assertContains(generated, Double.NaN)
        assertContains(generated, 0.0)
    }
}