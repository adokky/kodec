package io.kodec.struct

import karamel.utils.asLong
import karamel.utils.toBoolean
import karamel.utils.toInt
import karamel.utils.toLong

// READ

private fun Long.getBits(field: BitStructField<*>): Long = (this ushr field.offset) and field.mask64

fun Long.get(field: BitStructField<Boolean>): Boolean = getBits(field).toBoolean()
fun Long.get(field: BitStructField<Byte>): Byte = getBits(field).toByte()
fun Long.get(field: BitStructField<Short>): Short = getBits(field).toShort()
fun Long.get(field: BitStructField<Int>): Int = getBits(field).toInt()
fun Long.get(field: BitStructField<Long>): Long = getBits(field)

private fun Int.getBits(field: BitStructField<*>): Int = (this ushr field.offset) and field.mask32

fun Int.get(field: BitStructField<Boolean>): Boolean = getBits(field).toBoolean()
fun Int.get(field: BitStructField<Byte>): Byte = getBits(field).toByte()
fun Int.get(field: BitStructField<Short>): Short = getBits(field).toShort()
fun Int.get(field: BitStructField<Int>): Int = getBits(field)
fun Int.get(field: BitStructField<Long>): Long = getBits(field).asLong()


// WRITE

private fun Int.orInt(field: BitStructField<*>, value: Int): Int = this or (value shl field.offset)

private fun Int.orIntSafe(field: BitStructField<*>, value: Int): Int {
    val v = value and field.mask32
    require(v == value) { "value $value is out of range 0..${field.mask32}" }
    return orInt(field, v)
}

fun Int.or(field: BitStructField<Boolean>, value: Boolean): Int = orInt(field, value.toInt())
fun Int.or(field: BitStructField<Byte>,    value: Byte   ): Int = orInt(field, value.toInt() and field.mask32)
fun Int.or(field: BitStructField<Short>,   value: Short  ): Int = orInt(field, value.toInt() and field.mask32)
fun Int.or(field: BitStructField<Int>,     value: Int    ): Int = orInt(field, value and field.mask32)
fun Int.or(field: BitStructField<Long>,    value: Long   ): Int = orInt(field, value.toInt() and field.mask32)

fun Int.orSafe(field: BitStructField<Byte>,    value: Byte ): Int = orIntSafe(field, value.toInt())
fun Int.orSafe(field: BitStructField<Short>,   value: Short): Int = orIntSafe(field, value.toInt())
fun Int.orSafe(field: BitStructField<Int>,     value: Int  ): Int = orIntSafe(field, value)
fun Int.orSafe(field: BitStructField<Long>,    value: Long ): Int = orIntSafe(field, value.toInt())

private fun Long.orLong(field: BitStructField<*>, value: Long): Long = this or (value shl field.offset)

private fun Long.orLongSafe(field: BitStructField<*>, value: Long): Long {
    val v = value and field.mask64
    require(v == value) { "value $value is out of range 0..${field.mask64}" }
    return orLong(field, v)
}

fun Long.or(field: BitStructField<Boolean>, value: Boolean): Long = orLong(field, value.toLong())
fun Long.or(field: BitStructField<Byte>,    value: Byte   ): Long = orLong(field, value.toLong() and field.mask64)
fun Long.or(field: BitStructField<Short>,   value: Short  ): Long = orLong(field, value.toLong() and field.mask64)
fun Long.or(field: BitStructField<Int>,     value: Int    ): Long = orLong(field, value.toLong() and field.mask64)
fun Long.or(field: BitStructField<Long>,    value: Long   ): Long = orLong(field, value and field.mask64)

fun Long.orSafe(field: BitStructField<Byte>,    value: Byte ): Long = orLongSafe(field, value.toLong())
fun Long.orSafe(field: BitStructField<Short>,   value: Short): Long = orLongSafe(field, value.toLong())
fun Long.orSafe(field: BitStructField<Int>,     value: Int  ): Long = orLongSafe(field, value.toLong())
fun Long.orSafe(field: BitStructField<Long>,    value: Long ): Long = orLongSafe(field, value)