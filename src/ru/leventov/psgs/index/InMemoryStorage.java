package ru.leventov.psgs.index;

import gnu.trove.function.Consumer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;

public class InMemoryStorage implements Storage {
    private final ByteOrder byteOrder;
    private final int pageSize;
    private ArrayList<ByteBuffer> pages = new ArrayList<>();

    public InMemoryStorage(ByteOrder byteOrder, int pageSize) {
        this.byteOrder = byteOrder;
        this.pageSize = pageSize;
    }

    @Override
    public int pageSize() {
        return pageSize;
    }

    @Override
    public ByteBuffer getPage(int pageIndex) {
        return pages.get(pageIndex);
    }

    @Override
    public ByteBuffer cacheAndGetPage(int pageIndex) {
        return pages.get(pageIndex);
    }

    @Override
    public ByteBuffer allocateNewPage() {
        ByteBuffer newPage = ByteBuffer.allocate(pageSize);
        newPage.order(byteOrder);
        pages.add(newPage);
        return newPage;
    }

    @Override
    public int pageCount() {
        return pages.size();
    }

    @Override
    public Metadata metadata() {
        Metadata metadata = new Metadata();
        metadata.byteOrder = byteOrder;
        metadata.pageCount = pageCount();
        metadata.pageSize = pageSize;
        return metadata;
    }

    @Override
    public void forEachPage(Consumer<ByteBuffer> action) {
        for (ByteBuffer page : pages) {
            page.clear();
            action.accept(page);
        }
    }

    public void write(WritableByteChannel out) throws IOException {
        for (ByteBuffer page : pages) {
            page.clear();
            out.write(page);
        }
    }
}
