package io.kodec.buffers

import io.kodec.NumbersBigEndian
import io.kodec.NumbersLittleEndian

abstract class AbstractDataBufferLE: AbstractBuffer(), DataBuffer {
    override fun getInt16(pos: Int): Short = NumbersLittleEndian.getInt16 { offset -> get(pos + offset) }
    override fun getInt24(pos: Int): Int   = NumbersLittleEndian.getInt24 { offset -> get(pos + offset) }
    override fun getInt32(pos: Int): Int   = NumbersLittleEndian.getInt32 { offset -> get(pos + offset) }
    override fun getInt40(pos: Int): Long  = NumbersLittleEndian.getInt40 { offset -> get(pos + offset) }
    override fun getInt48(pos: Int): Long  = NumbersLittleEndian.getInt48 { offset -> get(pos + offset) }
    override fun getInt56(pos: Int): Long  = NumbersLittleEndian.getInt56 { offset -> get(pos + offset) }
    override fun getInt64(pos: Int): Long  = NumbersLittleEndian.getInt64 { offset -> get(pos + offset) }
}

abstract class AbstractDataBufferBE: AbstractBuffer(), DataBuffer {
    override fun getInt16(pos: Int): Short = NumbersBigEndian.getInt16 { offset -> get(pos + offset) }
    override fun getInt24(pos: Int): Int   = NumbersBigEndian.getInt24 { offset -> get(pos + offset) }
    override fun getInt32(pos: Int): Int   = NumbersBigEndian.getInt32 { offset -> get(pos + offset) }
    override fun getInt40(pos: Int): Long  = NumbersBigEndian.getInt40 { offset -> get(pos + offset) }
    override fun getInt48(pos: Int): Long  = NumbersBigEndian.getInt48 { offset -> get(pos + offset) }
    override fun getInt56(pos: Int): Long  = NumbersBigEndian.getInt56 { offset -> get(pos + offset) }
    override fun getInt64(pos: Int): Long  = NumbersBigEndian.getInt64 { offset -> get(pos + offset) }
}