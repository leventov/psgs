package ru.leventov.psgs.io;

import java.nio.ByteBuffer;

public interface DataOutput extends java.io.DataOutput {
    void write(int b);

    void write(byte[] b);

    void write(byte[] b, int off, int len);

    void write(ByteBuffer src);

    void writeBoolean(boolean v);

    void writeByte(int v);

    void writeShort(int v);

    void writeChar(int v);

    void writeInt(int v);

    void writeLong(long v);

    void writeFloat(float v);

    void writeDouble(double v);

    void writeBytes(String s);

    void writeChars(String s);

    void writeUTF(String s);
}
