package ru.leventov.psgs.io;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

public final class Files {
    private Files() {}

    public static FileChannel openForUpdating(Path file) throws IOException {
        return FileChannel.open(file, READ, WRITE);
    }

    public static FileChannel openForWriting(Path file) throws IOException {
        return FileChannel.open(file, CREATE, WRITE);
    }

    public static FileChannel openForReading(Path file) throws IOException {
        return FileChannel.open(file, StandardOpenOption.READ);
    }

    public static void removeDir(Path dir) {
        File folder = dir.toFile();
        File[] files = folder.listFiles();
        if(files!=null) { //some JVMs return null for empty dirs
            for(File f: files) {
                if(f.isDirectory()) {
                    removeDir(f.toPath());
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }
}
