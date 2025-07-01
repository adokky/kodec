package io.kodec.struct

import io.kodec.StringsDataSet
import io.kodec.buffers.ArrayDataBuffer
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class BufferStructTest {
    private object Person: BufferStruct() {
        val name = stringAscii(TEST_STRING_SIZE)
        val age = int16()
        val income = int64()
    }

    private val buffer = ArrayDataBuffer(1000)

    @Test
    fun randomized_read_write() {
        test(false)
        buffer.clear()
        test(true)
    }

    private fun test(bwd: Boolean) {
        val names = StringsDataSet.getAsciiData()
            .filter { it.length >= TEST_STRING_SIZE }
            .map { it.take(TEST_STRING_SIZE) }
            .take(100)
            .toList()

        val maxStructs = buffer.size / Person.getStructSize()

        data class PersonModel(val name: String, val age: Int, val income: Long) {
            constructor() : this(EMPTY_STRING, 0, 0)

            fun check(buffer: ArrayDataBuffer, offset: Int) {
                assertEquals(
                    name,
                    if (bwd) buffer.getBackwards(offset, Person.name)
                    else buffer.get(offset, Person.name)
                )
                assertEquals(
                    age,
                    if (bwd) buffer.getBackwards(offset, Person.age)
                    else buffer.get(offset, Person.age)
                )
                assertEquals(
                    income,
                    if (bwd) buffer.getBackwards(offset, Person.income)
                    else buffer.get(offset, Person.income)
                )
            }
        }

        val model = MutableList(maxStructs) { PersonModel() }

        repeat(100_000) {
            val person = PersonModel(
                name = names.random(),
                age = Random.nextInt(UShort.MAX_VALUE.toInt() + 1),
                income = Random.nextLong()
            )

            val index = Random.nextInt(maxStructs - 1)

            model[index] = person

            fun structOffset(index: Int) = (if (bwd) index + 1 else index) * Person.getStructSize()
            val offset = structOffset(index)

            if (bwd) buffer.putBackwards(offset, Person.name, person.name)
            else buffer.put(offset, Person.name, person.name)

            if (bwd) buffer.putBackwards(offset, Person.age, person.age)
            else buffer.put(offset, Person.age, person.age)

            if (bwd) buffer.putBackwards(offset, Person.income, person.income)
            else buffer.put(offset, Person.income, person.income)

            repeat(maxStructs) { index ->
                model[index].check(buffer, structOffset(index))
            }
        }
    }
}

private const val TEST_STRING_SIZE = 11

private val EMPTY_STRING = (1..TEST_STRING_SIZE).map { Char(0) }.joinToString("")