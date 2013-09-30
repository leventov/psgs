package ru.leventov.psgs.index;

import gnu.trove.function.Consumer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public interface Storage {
    static class Metadata {
        ByteOrder byteOrder;
        int pageSize;
        int pageCount;
    }

    int pageSize();
    ByteBuffer getPage(int pageIndex);
    ByteBuffer cacheAndGetPage(int pageIndex);
    ByteBuffer allocateNewPage();
    int pageCount();
    Metadata metadata();
    void forEachPage(Consumer<ByteBuffer> action);
}
