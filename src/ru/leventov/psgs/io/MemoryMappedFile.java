package ru.leventov.psgs.io;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;

import static java.nio.channels.FileChannel.MapMode.READ_ONLY;
import static java.nio.channels.FileChannel.MapMode.READ_WRITE;
import static ru.leventov.psgs.io.Files.openForReading;
import static ru.leventov.psgs.io.Files.openForUpdating;
import static ru.leventov.psgs.util.Bits.upperPowerOf2;
import static ru.leventov.psgs.util.ByteBuffers.slice;

public class MemoryMappedFile implements Closeable {
    private static final int BASIC_BUFFER_SIZE = 1 << 30;
    private static final int BUFFER_INDEX_SHIFT = 30;
    private static final int BUFFER_OFFSET_MASK = BASIC_BUFFER_SIZE - 1;
    public static final int MAX_CHUNK_SIZE_LIMIT = BASIC_BUFFER_SIZE / 8;

    private final FileChannel fileChannel;
    private final MappedByteBuffer[] mappedBuffers;
    private final int chunkSizeLimit;

    public MemoryMappedFile(Path file, ByteOrder byteOrder, int chunkSizeLimit, boolean readOnly) throws IOException {
        if (chunkSizeLimit <= 0 || chunkSizeLimit > MAX_CHUNK_SIZE_LIMIT ||
                upperPowerOf2(chunkSizeLimit) > MAX_CHUNK_SIZE_LIMIT)
            throw new IllegalArgumentException();
        fileChannel = readOnly ? openForReading(file) : openForUpdating(file);

        ArrayList<MappedByteBuffer> buffers = new ArrayList<>();
        this.chunkSizeLimit = chunkSizeLimit;
        chunkSizeLimit = (int) upperPowerOf2(chunkSizeLimit);
        int bufferSize = BASIC_BUFFER_SIZE + chunkSizeLimit;
        long pos = 0, size = fileChannel.size();
        FileChannel.MapMode mapMode = readOnly ? READ_ONLY : READ_WRITE;
        while (pos < size) {
            MappedByteBuffer mappedByteBuffer = fileChannel.map(mapMode, pos, Math.min(size - pos, bufferSize));
            mappedByteBuffer.order(byteOrder);
            buffers.add(mappedByteBuffer);
            pos += BASIC_BUFFER_SIZE;
        }
        mappedBuffers = buffers.toArray(new MappedByteBuffer[buffers.size()]);
    }

    public ByteBuffer locateChunk(long offset) {
        MappedByteBuffer buffer = mappedBuffers[(int) (offset >>> BUFFER_INDEX_SHIFT)];
        int bufferOffset = (int) (offset & BUFFER_OFFSET_MASK);
        buffer.position(bufferOffset);
        return slice(buffer);
    }

    @Override
    public void close() throws IOException {
        fileChannel.close();
    }
}
