package io.kodec

import java.io.File

fun main() {
    val file = File("commonMainRes/fp-numbers.bin")
    file.createNewFile()
    file.writeBytes(FpNumbersTestData.generate().toByteArray())
}