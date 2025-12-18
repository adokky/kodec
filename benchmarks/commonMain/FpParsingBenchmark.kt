package io.kodec

import kotlinx.benchmark.*

@Warmup(200, time = 200, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
@Measurement(1000, time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
class FpParsingBenchmark {
    val data = FpNumbersTestData.loadFromResource("fp-numbers.bin")
        ?: throw IllegalArgumentException("test data not found")

    @Benchmark
    fun floats(): Float {
        var result = 0.0f
        data.iterateFloatNumbers { start, end ->
            result += parseFloat(start, end).floatValue()
        }
        return result
    }

    @Benchmark
    fun doubles(): Double {
        var result = 0.0
        data.iterateDoubleNumbers { start, end ->
            result += parseDouble(start, end).doubleValue()
        }
        return result
    }
}

