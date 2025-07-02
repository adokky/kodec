package io.kodec.buffers

import io.kodec.NumbersBigEndian
import io.kodec.NumbersLittleEndian

abstract class AbstractOutputDataBufferLE: OutputDataBuffer {
    override fun putInt16(pos: Int, v: Short): Int =
        NumbersLittleEndian.putInt16(v) { offset, byte -> set(pos + offset, byte) }
    override fun putInt24(pos: Int, v: Int): Int  =
        NumbersLittleEndian.putInt24(v) { offset, byte -> set(pos + offset, byte) }
    override fun putInt32(pos: Int, v: Int): Int =
        NumbersLittleEndian.putInt32(v) { offset, byte -> set(pos + offset, byte) }
    override fun putInt40(pos: Int, v: Long): Int =
        NumbersLittleEndian.putInt40(v) { offset, byte -> set(pos + offset, byte) }
    override fun putInt48(pos: Int, v: Long): Int =
        NumbersLittleEndian.putInt48(v) { offset, byte -> set(pos + offset, byte) }
    override fun putInt56(pos: Int, v: Long): Int =
        NumbersLittleEndian.putInt56(v) { offset, byte -> set(pos + offset, byte) }
    override fun putInt64(pos: Int, v: Long): Int =
        NumbersLittleEndian.putInt64(v) { offset, byte -> set(pos + offset, byte) }
}

abstract class AbstractOutputDataBufferBE: OutputDataBuffer {
    override fun putInt16(pos: Int, v: Short): Int =
        NumbersBigEndian.putInt16(v) { offset, byte -> set(pos + offset, byte) }
    override fun putInt24(pos: Int, v: Int): Int  =
        NumbersBigEndian.putInt24(v) { offset, byte -> set(pos + offset, byte) }
    override fun putInt32(pos: Int, v: Int): Int =
        NumbersBigEndian.putInt32(v) { offset, byte -> set(pos + offset, byte) }
    override fun putInt40(pos: Int, v: Long): Int =
        NumbersBigEndian.putInt40(v) { offset, byte -> set(pos + offset, byte) }
    override fun putInt48(pos: Int, v: Long): Int =
        NumbersBigEndian.putInt48(v) { offset, byte -> set(pos + offset, byte) }
    override fun putInt56(pos: Int, v: Long): Int =
        NumbersBigEndian.putInt56(v) { offset, byte -> set(pos + offset, byte) }
    override fun putInt64(pos: Int, v: Long): Int =
        NumbersBigEndian.putInt64(v) { offset, byte -> set(pos + offset, byte) }
}