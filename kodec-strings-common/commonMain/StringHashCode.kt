package io.kodec

import kotlin.jvm.JvmStatic

object StringHashCode {
    @JvmStatic fun init(): Int = 0
    @JvmStatic fun next(previous: Int, charCode: Int): Int = 31 * previous + charCode
    @JvmStatic fun next(previous: Int, char: Char): Int = next(previous, char.code)
}