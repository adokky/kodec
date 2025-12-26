package io.kodec;

// comes along with JVM 21 std-lib implementation from jvm21Main source set
public class JvmMathIntrinsics {
    public static long unsignedMultiplyHigh(long x, long y) {
        return MathUtilsKt.unsignedMultiplyHighCommon(x, y);
    }
}