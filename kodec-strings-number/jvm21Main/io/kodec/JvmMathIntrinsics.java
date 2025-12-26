package io.kodec;

import java.lang.Math;

public class JvmMathIntrinsics {
    public static long unsignedMultiplyHigh(long x, long y) {
        return Math.unsignedMultiplyHigh(x, y);
    }
}