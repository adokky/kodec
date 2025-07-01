package io.kodec.struct

abstract class BitStruct(val maxSize: Int) {
    init {
        require(maxSize >= 1)
    }

    private val allFields = ArrayList<BitStructField<*>>()
    private var actualSize: Int = 0

    // STRUCT_SIZE it is not constant-foldable.
    // To make it extra clear we expose it as a function
    fun getStructSize(): Int = actualSize

    val ALL_FIELDS: List<BitStructField<*>> get() = allFields

    private fun autoOffset(): Int = allFields.lastOrNull()?.end ?: 0

    protected fun <T> field(size: Int, offset: Int): BitStructField<T> {
        val field = BitStructField<T>(offset = offset, size = size)
        add(field)
        return field
    }

    protected fun <T> field(prototype: BitStructField<T>): BitStructField<T> {
        val field = BitStructField<T>(offset = autoOffset(), size = prototype.size)
        add(field)
        return field
    }

    private fun add(field: BitStructField<*>) {
        allFields.add(field)

        val newSize = actualSize + field.size
        if (newSize > maxSize) throw IllegalArgumentException("actualSize=$newSize > maxSize=$maxSize")
        actualSize = newSize
    }

    protected fun bool(size: Int = 1, offset: Int = autoOffset()): BitStructField<Boolean> = field(size, offset)
    protected fun byte(size: Int = 8, offset: Int = autoOffset()): BitStructField<Int> = field(size, offset)
    protected fun short(size: Int = 16, offset: Int = autoOffset()): BitStructField<Short> = field(size, offset)
    protected fun int(size: Int, offset: Int = autoOffset()): BitStructField<Int> = field(size, offset)
    protected fun long(size: Int, offset: Int = autoOffset()): BitStructField<Long> = field(size, offset)
}