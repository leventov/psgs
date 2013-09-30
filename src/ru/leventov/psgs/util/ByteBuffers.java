package ru.leventov.psgs.util;

import java.nio.ByteBuffer;

public final class ByteBuffers {

    private ByteBuffers() {}

    public static void copy(ByteBuffer src, int srcPos, ByteBuffer dst, int dstPos, int length) {
        src.position(srcPos);
        src.limit(srcPos + length);
        dst.position(dstPos);
        dst.limit(dstPos + length);
        dst.put(src);
        src.limit(src.capacity());
        dst.limit(dst.capacity());
    }

    public static void shiftWithin(ByteBuffer buffer, int pos, int length, int shift) {
        copy(buffer, pos, buffer.duplicate(), pos + shift, length);
    }

    /**
     * .slice() made right - preserving byte order
     */
    public static ByteBuffer slice(ByteBuffer buffer) {
        return buffer.slice().order(buffer.order());
    }

    /**
     * .duplicate() made right - preserving byte order
     */
    public static ByteBuffer duplicate(ByteBuffer buffer) {
        return buffer.duplicate().order(buffer.order());
    }

    public static void skip(ByteBuffer buffer, int skip) {
        buffer.position(buffer.position() + skip);
    }

    public static byte[] asArray(ByteBuffer buffer) {
        byte[] a = new byte[buffer.remaining()];
        buffer.get(a);
        return a;
    }
}
