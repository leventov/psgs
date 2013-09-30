package ru.leventov.psgs.index;

import ru.leventov.psgs.io.Files;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;

import static java.nio.file.Files.*;

public class NewBTreeIndex extends BTreeIndex {

    private final InMemoryStorage storage;
    public NewBTreeIndex(ByteOrder byteOrder, int valueLength) {
        storage = new InMemoryStorage(byteOrder, nativePageSize());
        bTree = new BTree(storage, valueLength);
    }

    public void write(Path dir) throws IOException {
        createDirectories(dir);
        try (SeekableByteChannel ch = Files.openForWriting(storageFile(dir))) {
            storage.write(ch);
        }
        writeMetadata(dir, bTree, storage);
    }
}
