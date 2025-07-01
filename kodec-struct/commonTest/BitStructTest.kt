package io.kodec.struct

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BitStructTest {
    private object TestStruct: BitStruct(32) {
        val bool1 = bool(2)
        val bool2 = bool(1)

        val short1 = short(3)
        val short2 = short(2)

        val int1 = int(6)
        val int2 = int(3)
        val int3 = int(9)

        val long1 = long(2)
    }

    @Test
    fun int_struct() {
        val int = composeInt {
            TestStruct.bool1 set true
            TestStruct.short1 set 4
            TestStruct.int1 set 34
            TestStruct.int3 set 419
            TestStruct.long1 set 2
        }

        assertEquals(true, int.get(TestStruct.bool1))
        assertEquals(false, int.get(TestStruct.bool2))
        assertEquals(4, int.get(TestStruct.short1))
        assertEquals(0, int.get(TestStruct.short2))
        assertEquals(34, int.get(TestStruct.int1))
        assertEquals(0, int.get(TestStruct.int2))
        assertEquals(419, int.get(TestStruct.int3))
        assertEquals(2L, int.get(TestStruct.long1))
    }

    private object LongStruct: BitStruct(64) {
        val bool1 = bool(2)
        val bool2 = bool(1)

        val short1 = short(3)
        val short2 = short(2)

        val int1 = int(6)
        val int2 = int(3)
        val int3 = int(9)

        val long1 = long(23)
        val long2 = long(5)
        val long3 = long(9)
    }

    @Test
    fun long_struct() {
        val int = composeLong {
            LongStruct.bool1 set true
            LongStruct.short1 set 4
            LongStruct.int1 set 34
            LongStruct.int3 set 419
            LongStruct.long1 set 2
            LongStruct.long2 set 1
            LongStruct.long3 set 109
        }

        assertEquals(true, int.get(LongStruct.bool1))
        assertEquals(false, int.get(LongStruct.bool2))
        assertEquals(4, int.get(LongStruct.short1))
        assertEquals(0, int.get(LongStruct.short2))
        assertEquals(34, int.get(LongStruct.int1))
        assertEquals(0, int.get(LongStruct.int2))
        assertEquals(419, int.get(LongStruct.int3))
        assertEquals(2L, int.get(LongStruct.long1))
        assertEquals(1L, int.get(LongStruct.long2))
        assertEquals(109L, int.get(LongStruct.long3))
    }

    private fun testIntField(max: Int, set: BitStructWriteContext32.(value: Int) -> Unit) {
        composeIntSafe { set(0) }
        composeIntSafe { set(max) }
        assertFailsWith<IllegalArgumentException> { composeIntSafe { set(-1) } }
        assertFailsWith<IllegalArgumentException> { composeIntSafe { set(max + 1) } }
        assertFailsWith<IllegalArgumentException> { composeIntSafe { set(Int.MAX_VALUE) } }
    }

    private fun testLongField(max: Long, set: BitStructWriteContext64.(value: Long) -> Unit) {
        composeLongSafe { set(0) }
        composeLongSafe { set(max) }
        assertFailsWith<IllegalArgumentException> { composeLongSafe { set(-1) } }
        assertFailsWith<IllegalArgumentException> { composeLongSafe { set(max + 1) } }
        assertFailsWith<IllegalArgumentException> { composeLongSafe { set(Long.MAX_VALUE) } }
    }

    @Test
    fun int_struct_safe() {
        testIntField(7) { TestStruct.short1 set it.toShort() }
        testIntField(63) { TestStruct.int1 set it }
        testIntField(3) { TestStruct.long1 set it.toLong() }
    }

    @Test
    fun long_struct_safe() {
        testLongField(7) { LongStruct.short1 set it.toShort() }
        testLongField(63) { LongStruct.int1 set it.toInt() }
        testLongField(0L.inv() ushr (64 - LongStruct.long1.size)) { LongStruct.long1 set it }
        testLongField(31) { LongStruct.long2 set it }
    }
}