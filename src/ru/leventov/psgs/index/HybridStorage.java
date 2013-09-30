package ru.leventov.psgs.index;

import gnu.trove.function.Consumer;
import gnu.trove.function.IntFunction;
import gnu.trove.map.IntKeyMapIterator;
import gnu.trove.map.hash.IntObjDHashMap;
import gnu.trove.map.hash.TIntObjHashMap;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;

import static java.nio.channels.FileChannel.MapMode.*;
import static ru.leventov.psgs.io.Files.*;
import static ru.leventov.psgs.util.Bits.lowerPowerOf2;
import static ru.leventov.psgs.util.ByteBuffers.slice;

public class HybridStorage implements Storage, Closeable {

    private final ByteOrder byteOrder;
    private final int pageSize;
    private final int mappedPageCount;
    private final int bufferIndexShift;
    private final int posInBufferMask;
    private final ArrayList<ByteBuffer> newPages = new ArrayList<>();
    private final TIntObjHashMap<ByteBuffer> cachedPages = new IntObjDHashMap<>();
    private final FileChannel fileChannel;
    private final MappedByteBuffer[] mappedBuffers;
    private final boolean readOnly;

    public HybridStorage(Path file, Metadata metadata, boolean readOnly)
            throws IOException {
        mappedPageCount = metadata.pageCount;
        this.byteOrder = metadata.byteOrder;
        this.pageSize = metadata.pageSize;

        int surelyPossibleArraySize = Integer.MAX_VALUE - 20;
        int maxPagesPerBuffer = lowerPowerOf2(surelyPossibleArraySize / pageSize);
        posInBufferMask = maxPagesPerBuffer - 1;
        int shift = 1;
        while ((1 << shift) != maxPagesPerBuffer) shift++;
        bufferIndexShift = shift;

        fileChannel = readOnly ? openForReading(file) : openForUpdating(file);
        ArrayList<MappedByteBuffer> buffers = new ArrayList<>();
        int bufferSize = maxPagesPerBuffer * pageSize;
        long pos = 0, size = fileChannel.size();
        FileChannel.MapMode mapMode = readOnly ? READ_ONLY : READ_WRITE;
        while (pos < size) {
            MappedByteBuffer mappedByteBuffer = fileChannel.map(mapMode, pos, Math.min(size - pos, bufferSize));
            mappedByteBuffer.order(byteOrder);
            buffers.add(mappedByteBuffer);
            pos += bufferSize;
        }
        mappedBuffers = buffers.toArray(new MappedByteBuffer[buffers.size()]);
        this.readOnly = readOnly;
    }

    private MappedByteBuffer locateMappedPage(int pageIndex) {
        MappedByteBuffer buffer = mappedBuffers[pageIndex >>> bufferIndexShift];
        buffer.clear();
        buffer.position((pageIndex & posInBufferMask) * pageSize);
        buffer.limit(buffer.position() + pageSize);
        return buffer;
    }

    @Override
    public int pageSize() {
        return pageSize;
    }

    @Override
    public ByteBuffer getPage(int pageIndex) {
        if (pageIndex < mappedPageCount) {
            ByteBuffer cachedPage = cachedPages.get(pageIndex);
            if (cachedPage != null) {
                return cachedPage;
            } else {
                return slice(locateMappedPage(pageIndex));
            }
        } else {
            return newPages.get(pageIndex - mappedPageCount);
        }
    }

    @Override
    public ByteBuffer cacheAndGetPage(int pageIndex) {
        if (pageIndex < mappedPageCount) {
            return cachedPages.computeIfAbsent(pageIndex, new IntFunction<ByteBuffer>() {
                @Override
                public ByteBuffer apply(int pageIndex) {
                    MappedByteBuffer mappedPage = locateMappedPage(pageIndex);
                    ByteBuffer cachedPage = allocatePage();
                    cachedPage.put(mappedPage);
                    return cachedPage;
                }
            });
        } else {
            return newPages.get(pageIndex - mappedPageCount);
        }
    }

    @Override
    public ByteBuffer allocateNewPage() {
        ByteBuffer newPage = allocatePage();
        newPages.add(newPage);
        return newPage;
    }

    private ByteBuffer allocatePage() {
        return ByteBuffer.allocate(pageSize).order(byteOrder);
    }

    @Override
    public int pageCount() {
        return mappedPageCount + newPages.size();
    }

    @Override
    public Metadata metadata() {
        Metadata metadata = new Metadata();
        metadata.pageCount = pageCount();
        metadata.pageSize = pageSize;
        metadata.byteOrder = byteOrder;
        return metadata;
    }

    @Override
    public void forEachPage(Consumer<ByteBuffer> action) {
        ByteBuffer page = allocatePage();
        for (int i = 0; i < mappedPageCount; i++) {
            ByteBuffer cachedPage = cachedPages.get(i);
            if (cachedPage != null) {
                action.accept(cachedPage);
            } else {
                action.accept(slice(locateMappedPage(i)));
            }
        }
        for (ByteBuffer newPage : newPages) {
            newPage.clear();
            action.accept(newPage);
        }
    }

    @Override
    public void close() throws IOException {
        if (!readOnly) {
            // flush cache
            for (IntKeyMapIterator<ByteBuffer> it = cachedPages.mapIterator(); it.tryAdvance();) {
                MappedByteBuffer mappedPage = locateMappedPage(it.intKey());
                ByteBuffer cachedPage = it.value();
                cachedPage.clear();
                mappedPage.put(cachedPage);
            }
            // append new pages to the end of the file
            fileChannel.position(mappedPageCount * pageSize);
            for (ByteBuffer newPage : newPages) {
                newPage.clear();
                fileChannel.write(newPage);
            }
        }
        fileChannel.close();
    }
}
