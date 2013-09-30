package ru.leventov.psgs.index;

import ru.leventov.psgs.io.Json;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

public class ExistingBTreeIndex extends BTreeIndex implements Closeable {

    private final HybridStorage storage;
    private final boolean readOnly;
    private final Path dir;

    public ExistingBTreeIndex(Path dir, boolean readOnly) throws IOException {
        Metadata metadata = Json.readJson(metadataFile(dir), Metadata.class);
        storage = new HybridStorage(storageFile(dir), metadata.storageMetadata, readOnly);
        bTree = metadata.bTree;
        bTree.setStorageAfterDeserialization(storage);
        this.dir = dir;
        this.readOnly = readOnly;
    }

    @Override
    public void close() throws IOException {
        if (!readOnly) {
            writeMetadata(dir, bTree, storage);
        }
        storage.close();
    }
}
