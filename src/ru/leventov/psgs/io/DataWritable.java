package ru.leventov.psgs.io;

public interface DataWritable {
	void readData(DataInput in);
	void writeData(DataOutput out);
}
