package ru.leventov.psgs.util;

public final class Bits {

    private Bits() {}

    public static int unsignedByte(byte value) {
        return value & 0xFF;
    }

    public static long unsignedInt(int value) {
        return value & ((1L << 32) - 1L);
    }

    public static int lowerPowerOf2(int value) {
        long power = 1;
        while (power <= value) power *= 2;
        return (int) (power >>> 1);
    }

    public static long upperPowerOf2(int value) {
        long power = 1;
        while (power < value) power *= 2;
        return power;
    }

    public static int roundUp2(int value) {
        return (value + 1) & ~1;
    }

    public static int roundUp4(int value) {
        return (value + 3) & ~3;
    }

    public static int roundUp8(int value) {
        return (value + 7) & ~7;
    }
}
