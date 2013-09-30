package ru.leventov.psgs.io;

import ru.leventov.psgs.util.Bits;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.nio.ByteBuffer;

public class ByteBufferDataIO implements DataOutput, DataInput {
	private final ByteBuffer buffer;

	public ByteBufferDataIO(ByteBuffer buffer) {
		this.buffer = buffer;
	}

	public ByteBuffer getBuffer() {
		return buffer;
	}

	// DataOutput methods

	@Override
    public void write(int b) {
		buffer.put((byte) b);
	}

	@Override
    public void write(byte[] b) {
		buffer.put(b);
	}

	@Override
    public void write(byte[] b, int off, int len) {
		buffer.put(b, off, len);
	}

    @Override
    public void write(ByteBuffer src) {
        buffer.put(src);
    }

    @Override
    public void writeBoolean(boolean v) {
		buffer.put((byte) (v ? 1 : 0));
	}

	@Override
    public void writeByte(int v) {
		buffer.put((byte) v);
	}

	@Override
    public void writeShort(int v) {
		buffer.putShort((short) v);
	}

	@Override
    public void writeChar(int v) {
		buffer.putChar((char) v);
	}

	@Override
    public void writeInt(int v) {
		buffer.putInt(v);
	}

	@Override
    public void writeLong(long v) {
		buffer.putLong(v);
	}

	@Override
    public void writeFloat(float v) {
		buffer.putFloat(v);
	}

	@Override
    public void writeDouble(double v) {
		buffer.putDouble(v);
	}

	@Override
    public void writeBytes(String s) {
		int len = s.length();
		for (int i = 0; i < len; i++) {
			buffer.put((byte) s.charAt(i));
		}
	}

	@Override
    public void writeChars(String s) {
		int len = s.length();
		for (int i = 0; i < len; i++) {
			buffer.putChar(s.charAt(i));
		}
	}

	@Override
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

	// DataInput methods

	@Override
    public void readFully(byte[] b) {
		buffer.get(b);
	}

	@Override
    public void readFully(byte[] b, int off, int len) {
		buffer.get(b, off, len);
	}

    @Override
    public void readInto(ByteBuffer dst) {
        int pos = buffer.position();
        int limit = buffer.limit();
        int remaining = Math.min(limit - pos, dst.remaining());
        buffer.limit(pos + remaining);
        dst.put(buffer);
        buffer.limit(limit);
    }

    @Override
    public int skipBytes(int n) {
		int pos = buffer.position();
		buffer.position(Math.min(pos + n, buffer.limit()));
		return buffer.position() - pos;
	}

	@Override
    public boolean readBoolean() {
		return buffer.get() != 0;
	}

	@Override
    public byte readByte() {
		return buffer.get();
	}

	@Override
    public int readUnsignedByte() {
		return buffer.get() & 0xFF;
	}

	@Override
    public short readShort() {
		return buffer.getShort();
	}

	@Override
    public int readUnsignedShort() {
		return buffer.getShort() & 0xFFFF;
	}

	@Override
    public char readChar() {
		return buffer.getChar();
	}

	@Override
    public int readInt() {
		return buffer.getInt();
	}

    @Override
    public long readUnsignedInt() {
        return Bits.unsignedInt(buffer.getInt());
    }

    @Override
    public long readLong() {
		return buffer.getLong();
	}

	@Override
    public float readFloat() {
		return buffer.getFloat();
	}

	@Override
    public double readDouble() {
		return buffer.getDouble();
	}

	public String readLine() {
		int lim = buffer.limit();
		int pos = buffer.position();
		if (pos == lim)
			return null;
		StringBuilder line = new StringBuilder();
		for (; pos < lim; pos++) {
			byte b = buffer.get(pos);
			if (b == '\n') {
				break;
			} else if (b == '\r') {
				if (pos < lim - 1 && buffer.get(pos + 1) == '\n') {
					pos++;
				}
				break;
			} else {
				line.append((char) b);
			}
		}
		buffer.position(pos);
		return line.toString();
	}

	@Override
    public String readUTF() {
        try {
            return DataInputStream.readUTF(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
