package ru.leventov.psgs.index;

import gnu.trove.function.IntObjConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.leventov.psgs.io.Json;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class BTreeIndex {
    static int nativePageSize() {
        return 4096;
    }

    static class Metadata {
        BTree bTree;
        Storage.Metadata storageMetadata;
        Metadata() {}
        Metadata(BTree bTree, Storage.Metadata storageMetadata) {
            this.bTree = bTree;
            this.storageMetadata = storageMetadata;
        }
    }

    static Path storageFile(Path dir) {
        return dir.resolve("pages");
    }

    static Path metadataFile(Path dir) {
        return dir.resolve("metadata.json");
    }

    BTree bTree;

    @Nullable
    public ByteBuffer get(int key) {
        return bTree.get(key);
    }

    @NotNull
    public ByteBuffer insert(int key) {
        return bTree.insert(key);
    }

    public void forEachEntry(IntObjConsumer<ByteBuffer> action) {
        bTree.forEachEntry(action);
    }

    static void writeMetadata(Path dir, BTree bTree, Storage storage) throws IOException {
        bTree.countStats();
        Json.writeJson(metadataFile(dir), new Metadata(bTree, storage.metadata()));
    }

    public static void copy(Path srcDir, Path dstDir) throws IOException {
        Files.createDirectories(dstDir);
        Files.copy(metadataFile(srcDir), metadataFile(dstDir));
        Files.copy(storageFile(srcDir), storageFile(dstDir));
    }
}
