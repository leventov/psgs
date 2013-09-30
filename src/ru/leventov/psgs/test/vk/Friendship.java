package ru.leventov.psgs.test.vk;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.leventov.psgs.*;
import ru.leventov.psgs.io.DataInput;
import ru.leventov.psgs.io.DataOutput;
import ru.leventov.psgs.io.DataWriter;
import ru.leventov.psgs.io.NoData;

public class Friendship extends ArcModel<Person, Person, NoData> implements DataWriter<NoData> {

    public Friendship(Graph graph) {
        super(graph);
    }

    @Override
    public int dataSize() {
        return 0;
    }

    @NotNull
    @Override
    protected NodeIdEdgeMap<NoData> newMap(int expectedSize) {
        return new NodeIdNoDataHashMap(expectedSize);
    }

    @NotNull
    @Override
    protected NodeIdEdgeMap<NoData> newMapForDeserialization(int size) {
        return new NodeIdNoDataHashMap(size);
    }

    @Override
    public void writeData(DataOutput out, @NotNull NoData data) {
    }

    @NotNull
    @Override
    public NoData readData(DataInput in, @Nullable NoData dstData) {
        return NoData.NO_DATA;
    }
}
