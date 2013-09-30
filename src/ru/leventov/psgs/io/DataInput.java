package ru.leventov.psgs.io;

import java.nio.ByteBuffer;

public interface DataInput extends java.io.DataInput {
    void readFully(byte[] b);

    void readFully(byte[] b, int off, int len);

    void readInto(ByteBuffer dst);

    int skipBytes(int n);

    boolean readBoolean();

    byte readByte();

    int readUnsignedByte();

    short readShort();

    int readUnsignedShort();

    char readChar();

    int readInt();

    long readUnsignedInt();

    long readLong();

    float readFloat();

    double readDouble();

    String readLine();

    String readUTF();
}
