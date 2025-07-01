package io.kodec.struct

abstract class BufferStruct {
    private val allFields = ArrayList<BufferStructField<*>>()

    private fun autoOffset(): Int = allFields.lastOrNull()?.end ?: 0

    private var STRUCT_SIZE: Int = 0

    // STRUCT_SIZE it is not constant foldable.
    // To make it extra clear we expose it as a function
    fun getStructSize(): Int = STRUCT_SIZE

    val ALL_FIELDS: List<BufferStructField<*>> get() = allFields

    protected fun <T> field(offset: Int, size: Int): BufferStructField<T> {
        val f = BufferStructField<T>(offset, size)
        allFields.add(f)
        STRUCT_SIZE += size
        return f
    }

    protected fun <T> field(field: BufferStructField<T>): BufferStructField<T> {
        val copy = BufferStructField<T>(autoOffset(), field.size)
        allFields.add(copy)
        STRUCT_SIZE += copy.size
        return copy
    }

    protected fun bool(offset: Int = autoOffset()): BufferStructField<Boolean> = field(offset, 1)

    protected fun byte(offset: Int = autoOffset()): BufferStructField<Int> = field(offset, 1)

    protected fun short(offset: Int = autoOffset()): BufferStructField<Short> = short16(offset)
    protected fun short8(offset: Int = autoOffset()): BufferStructField<Short> = field(offset, 1)
    protected fun short16(offset: Int = autoOffset()): BufferStructField<Short> = field(offset, 2)

    protected fun int(offset: Int = autoOffset()): BufferStructField<Int> = int32(offset)
    protected fun int8(offset: Int = autoOffset()): BufferStructField<Int> = field(offset, 1)
    protected fun int16(offset: Int = autoOffset()): BufferStructField<Int> = field(offset, 2)
    protected fun int24(offset: Int = autoOffset()): BufferStructField<Int> = field(offset, 3)
    protected fun int32(offset: Int = autoOffset()): BufferStructField<Int> = field(offset, 4)
    protected fun int40(offset: Int = autoOffset()): BufferStructField<Long> = field(offset, 5)
    protected fun int48(offset: Int = autoOffset()): BufferStructField<Long> = field(offset, 6)
    protected fun int56(offset: Int = autoOffset()): BufferStructField<Long> = field(offset, 7)
    protected fun int64(offset: Int = autoOffset()): BufferStructField<Long> = field(offset, 8)

    protected fun long(offset: Int = autoOffset()): BufferStructField<Long> = long64(offset)
    protected fun long8(offset: Int = autoOffset()): BufferStructField<Long> = field(offset, 1)
    protected fun long16(offset: Int = autoOffset()): BufferStructField<Long> = field(offset, 2)
    protected fun long24(offset: Int = autoOffset()): BufferStructField<Long> = field(offset, 3)
    protected fun long32(offset: Int = autoOffset()): BufferStructField<Long> = field(offset, 4)
    protected fun long40(offset: Int = autoOffset()): BufferStructField<Long> = field(offset, 5)
    protected fun long48(offset: Int = autoOffset()): BufferStructField<Long> = field(offset, 6)
    protected fun long56(offset: Int = autoOffset()): BufferStructField<Long> = field(offset, 7)
    protected fun long64(offset: Int = autoOffset()): BufferStructField<Long> = field(offset, 8)

    protected fun stringAscii(size: Int, offset: Int = autoOffset()): BufferStructField<String> = field(offset, size)
}