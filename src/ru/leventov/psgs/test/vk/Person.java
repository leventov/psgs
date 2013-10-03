package ru.leventov.psgs.test.vk;

import ru.leventov.psgs.Node;
import ru.leventov.psgs.io.DataInput;
import ru.leventov.psgs.io.DataOutput;
import ru.leventov.psgs.io.DataWritable;

public class Person extends Node {
    boolean source;
    private int friendCount;

    public Person() {
    }

    public Person(boolean source, int friendCount) {
        this.source = source;
        this.friendCount = friendCount;
    }

    public void source(int friendCount) {
        if (!source) {
            source = true;
            this.friendCount = friendCount;
            super.onChange();
        }
    }

    @Override
    public void readData(DataInput in) {
        source = in.readBoolean();
        friendCount = in.readInt();
    }

    @Override
    public void writeData(DataOutput out) {
        out.writeBoolean(source);
        out.writeInt(friendCount);
    }
}
