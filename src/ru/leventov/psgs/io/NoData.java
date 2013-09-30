package ru.leventov.psgs.io;

public final class NoData implements DataWritable {

    public static final NoData NO_DATA = new NoData();

    private NoData() {}

    @Override
    public void readData(DataInput in) {
    }

    @Override
    public void writeData(DataOutput out) {
    }
}
