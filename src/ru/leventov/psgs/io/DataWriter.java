package ru.leventov.psgs.io;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface DataWriter<D> {
	void writeData(DataOutput out, @NotNull D data);
	@NotNull
    D readData(DataInput in, @Nullable D dstData);
    int dataSize();
}
