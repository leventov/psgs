package ru.leventov.psgs.io;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import static ru.leventov.psgs.util.ByteBuffers.duplicate;

public final class ByteArrayListDataOutput implements DataOutput {
    private ByteBuffer buffer;

    public ByteArrayListDataOutput(int initialCapacity, ByteOrder byteOrder) {
        buffer = ByteBuffer.allocate(initialCapacity);
        buffer.order(byteOrder);
    }

    public void ensureCapacity(int additionalSize) {
        ByteBuffer oldBuffer = buffer;
        int minGrow = additionalSize - oldBuffer.remaining();
        if (minGrow > 0) {
            int grow = Math.max(oldBuffer.capacity() >> 1, minGrow);
            byte[] newBytes = Arrays.copyOf(oldBuffer.array(), oldBuffer.capacity() + grow);
            buffer = ByteBuffer.wrap(newBytes);
            buffer.position(oldBuffer.position());
            buffer.order(oldBuffer.order());
        }
    }

    public ByteBuffer getBuffer() {
        return duplicate(buffer);
    }

    public int position() {
        return buffer.position();
    }

    public void position(int position) {
        ensureCapacity(position - buffer.position());
        buffer.position(position);
    }

    public void clear() {
        buffer.clear();
    }

    public void skipBytes(int skip) {
        ensureCapacity(skip);
        buffer.position(buffer.position() + skip);
    }

    public void write(int b) {
        ensureCapacity(1);
        buffer.put((byte) b);
    }

    public void write(byte[] a) {
        ensureCapacity(a.length);
        buffer.put(a);
    }

    public void write(byte[] a, int off, int len) {
        ensureCapacity(len);
        buffer.put(a, off, len);
    }

    @Override
    public void write(ByteBuffer src) {
        ensureCapacity(src.remaining());
        buffer.put(src);
    }

    public void writeBoolean(boolean v) {
        ensureCapacity(1);
        buffer.put((byte) (v ? 1 : 0));
    }

    public void writeByte(int v) {
        ensureCapacity(1);
        buffer.put((byte) v);
    }

    public void writeShort(int v) {
        ensureCapacity(2);
        buffer.putShort((short) v);
    }

    public void writeChar(int v) {
        ensureCapacity(2);
        buffer.putChar((char) v);
    }

    public void writeInt(int v) {
        ensureCapacity(4);
        buffer.putInt(v);
    }

    public void writeLong(long v) {
        ensureCapacity(8);
        buffer.putLong(v);
    }

    public void writeFloat(float v) {
        ensureCapacity(4);
        buffer.putFloat(v);
    }

    public void writeDouble(double v) {
        ensureCapacity(8);
        buffer.putDouble(v);
    }

    public void writeBytes(String s) {
        int len = s.length();
        ensureCapacity(len);
        for (int i = 0; i < len; i++) {
            buffer.put((byte) s.charAt(i));
        }
    }

    public void writeChars(String s) {
        int len = s.length();
        ensureCapacity(len * 2);
        for (int i = 0; i < len; i++) {
            buffer.putChar(s.charAt(i));
        }

    }

    public void writeUTF(String s) {
        try {
            int sLen = s.length();
            int utfLen = 0;
            for (int i = 0; i < sLen; i++) {
                int c = s.charAt(i);
                if ((c >= 0x0001) && (c <= 0x007F)) {
                    utfLen++;
                } else if (c > 0x07FF) {
                    utfLen += 3;
                } else {
                    utfLen += 2;
                }
            }

            if (utfLen > 65535)
                throw new UTFDataFormatException("Encoded string is too long: " + utfLen + " bytes");

            ensureCapacity(utfLen + 2);

            buffer.put((byte) ((utfLen >>> 8) & 0xFF));
            buffer.put((byte) (utfLen & 0xFF));

            for (int i = 0; i < sLen; i++) {
                int c = s.charAt(i);
                if ((c >= 0x0001) && (c <= 0x007F)) {
                    buffer.put((byte) c);
                } else if (c > 0x07FF) {
                    buffer.put((byte) (0xE0 | ((c >> 12) & 0x0F)));
                    buffer.put((byte) (0x80 | ((c >>  6) & 0x3F)));
                    buffer.put((byte) (0x80 | (c & 0x3F)));
                } else {
                    buffer.put((byte) (0xC0 | ((c >>  6) & 0x1F)));
                    buffer.put((byte) (0x80 | (c & 0x3F)));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
