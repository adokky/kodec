package io.kodec.java

import io.kodec.buffers.ArrayBuffer
import io.kodec.buffers.asBuffer
import io.kodec.buffers.emptyByteArray
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BufferInputStreamTest {
    private val buf = byteArrayOf(-33, -22, -11, 11, 22, 33, 44, 55, -66, -77, -88)
        .asBuffer(start = 3, endExclusive = 8) as ArrayBuffer
    private val array = buf.toByteArray()

    @Test
    fun sequential_read() {
        val input = BufferInputStream(buf)

        repeat(5) { i ->
            assertEquals((i + 1) * 11, input.read())
        }
        assertEquals(-1, input.read())
        assertEquals(-1, input.read())
    }

    @Test
    fun read_array() {
        val input = BufferInputStream(buf)

        input.read()

        ByteArray(2).also { arr ->
            assertEquals(1, input.read(arr, 1, 1))
            assertContentEquals(byteArrayOf(0, 22), arr)
        }

        ByteArray(2).also { arr ->
            assertEquals(2, input.read(arr, 0, 2))
            assertContentEquals(byteArrayOf(33, 44), arr)
        }

        ByteArray(5).also { arr ->
            assertEquals(1, input.read(arr, 1, 4))
            assertContentEquals(byteArrayOf(0, 55, 0, 0, 0), arr)
        }

        ByteArray(2).also { arr ->
            assertEquals(-1, input.read(arr, 0, 2))
            assertContentEquals(byteArrayOf(0, 0), arr)
        }

        ByteArray(0).also { arr ->
            assertEquals(-1, input.read(arr, 0, 0))
            assertContentEquals(byteArrayOf(), arr)
        }
    }

    @Test
    fun read_whole_as_byte_array() {
        val input = BufferInputStream(buf)

        assertContentEquals(array, input.readAllBytes())
        assertContentEquals(emptyByteArray, input.readAllBytes())
        assertContentEquals(emptyByteArray, input.readAllBytes())
        assertEquals(-1, input.read())
    }

    @Test
    fun read_as_byte_array_from_middle() {
        val input = BufferInputStream(buf)

        input.read()
        input.read()
        input.read()

        assertContentEquals(byteArrayOf(44, 55), input.readAllBytes())
        assertEquals(-1, input.read())
        assertContentEquals(emptyByteArray, input.readAllBytes())
        assertContentEquals(emptyByteArray, input.readAllBytes())
    }

    @Test
    fun read_empty() {
        val input = BufferInputStream(emptyByteArray.asBuffer())

        assertEquals(-1, input.read())
        assertEquals(-1, input.read())

        assertContentEquals(emptyByteArray, input.readAllBytes())
        assertContentEquals(emptyByteArray, input.readAllBytes())

        ByteArray(2).also { arr ->
            assertEquals(-1, input.read(arr, 1, 1))
            assertContentEquals(ByteArray(2), arr)
        }

        ByteArray(2).also { arr ->
            assertEquals(-1, input.read(arr, 0, 2))
            assertContentEquals(ByteArray(2), arr)
        }

        assertEquals(0, input.skip(0))
        assertEquals(0, input.skip(1))
        assertEquals(0, input.skip(-123))

        assertEquals(-1, input.read())
    }

    @Test
    fun mark_begin() {
        val input = BufferInputStream(buf)
        assertTrue(input.markSupported())

        input.mark(10)

        assertContentEquals(array, input.readAllBytes())
        assertEquals(-1, input.read())

        input.reset()

        assertContentEquals(array, input.readAllBytes())
        assertEquals(-1, input.read())
    }

    @Test
    fun mark_middle() {
        val input = BufferInputStream(buf)

        assertEquals(11, input.read())
        input.mark(10)
        assertEquals(22, input.read())

        input.reset()
        assertEquals(22, input.read())
        assertEquals(33, input.read())
        assertEquals(44, input.read())

        input.reset()
        assertEquals(22, input.read())
        assertEquals(33, input.read())
    }

    @Test
    fun skip() {
        val input = BufferInputStream(buf)

        assertEquals(1, input.skip(1))
        assertEquals(22, input.read())
        assertEquals(2, input.skip(2))
        assertEquals(55, input.read())
        assertEquals(-1, input.read())
        assertEquals(0, input.skip(2))
        assertEquals(-1, input.read())
    }

    @Test
    fun skip_more_than_available() {
        val input = BufferInputStream(buf)

        assertEquals(5, input.skip(10))
        assertEquals(0, input.skip(10))
        assertEquals(-1, input.read())
    }

    @Test
    fun transfer_all() {
        val input = BufferInputStream(buf)
        val baos = ByteArrayOutputStream()

        assertEquals(5, input.transferTo(baos))
        assertContentEquals(array, baos.toByteArray())
    }

    @Test
    fun transfer_partial() {
        val input = BufferInputStream(buf, 2)
        val baos = ByteArrayOutputStream()

        assertEquals(3, input.transferTo(baos))
        assertContentEquals(byteArrayOf(33, 44, 55), baos.toByteArray())
    }

    @Test
    fun transfer_empty() {
        val input = BufferInputStream(emptyByteArray.asBuffer())
        val baos = ByteArrayOutputStream()

        assertEquals(0, input.transferTo(baos))
        assertContentEquals(byteArrayOf(), baos.toByteArray())
    }

    @Test
    fun starting_position_in_the_middle() {
        val input = BufferInputStream(buf, 3)

        assertEquals(44, input.read())
        assertEquals(55, input.read())
        assertEquals(-1, input.read())
    }

    @Test
    fun starting_position_at_the_end() {
        val input = BufferInputStream(buf, 5)

        assertEquals(-1, input.read())
    }
}