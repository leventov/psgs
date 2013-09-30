package ru.leventov.psgs;

import gnu.trove.function.*;
import gnu.trove.map.*;
import gnu.trove.map.hash.IntIntDHashMap;
import ru.leventov.psgs.io.*;
import ru.leventov.psgs.util.ByteBuffers;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class NodeIdEdgeHashMap<ED> implements NodeIdEdgeMap<ED> {

	public static final float DEFAULT_LOAD_FACTOR = 0.8f;

    private TIntIntMap idPosMap;

	// Methods keep buffer limit = size, don't keep buffer position.
	private ByteBufferDataIO edgeDataBufferIO;
	private DataWriter<ED> edgeDataWriter;

    private int edgeDataBufferCapacity;
    private int edgeDataBufferSize;

	public NodeIdEdgeHashMap(int expectedSize, DataWriter<ED> edgeDataWriter) {
        idPosMap = new IntIntDHashMap(expectedSize, DEFAULT_LOAD_FACTOR) {
            public int getNoEntryValue() {
                return -1;
            }
        };
        this.edgeDataWriter = edgeDataWriter;
        ByteBuffer edgeDataBuffer = ByteBuffer.allocate(expectedSize * edgeDataWriter.dataSize());
        edgeDataBuffer.order(ByteOrder.nativeOrder());
        edgeDataBufferIO = new ByteBufferDataIO(edgeDataBuffer);
        edgeDataBufferSize = 0;
        edgeDataBufferCapacity = expectedSize;
    }

    public void readData(DataInput in) {
		int size = edgeDataBufferCapacity;
		int entrySize = edgeDataWriter.dataSize();
        ByteOrder byteOrder = readOrder(in);
        ByteBuffer edgeDataBuffer = edgeDataBufferIO.getBuffer();
        edgeDataBuffer.order(byteOrder);
        byte[] bufferBackingArray = edgeDataBuffer.array();
        int bufferOffset = edgeDataBuffer.arrayOffset();
		for (int i = 0; i < size; i++) {
			int nodeId = in.readInt();
            idPosMap.put(nodeId, i);
            in.readFully(bufferBackingArray, bufferOffset, entrySize);
            bufferOffset += entrySize;
		}
        if (size() != size)
            throw new RuntimeException("Map data corrupted");
        edgeDataBufferSize = size;
	}

    private static ByteOrder readOrder(DataInput in) {
        return in.readInt() == 0 ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
    }

    private static void writeOrder(DataOutput output, ByteOrder order) {
        output.writeInt(order.equals(ByteOrder.BIG_ENDIAN) ? 0 : -1);
    }

    public int size() {
        return idPosMap.size();
    }


    public boolean isEmpty() {
        return idPosMap.isEmpty();
    }


    public boolean containsNodeId(int nodeId) {
		return idPosMap.containsKey(nodeId);
	}

    public ED getEdgeData(int nodeId) {
		int pos = idPosMap.get(nodeId);
		if (pos >= 0) {
            return getData(pos, null);
		} else {
            return null;
        }
	}

	private ED getData(int pos, ED targetData) {
        edgeDataBufferIO.getBuffer().position(pos * edgeDataWriter.dataSize());
        return edgeDataWriter.readData(edgeDataBufferIO, targetData);
	}

	private void setData(int pos, ED data) {
        edgeDataBufferIO.getBuffer().position(pos * edgeDataWriter.dataSize());
		edgeDataWriter.writeData(edgeDataBufferIO, data);
	}

    private void growEdgeDataBuffer() {
        int oldCapacity = edgeDataBufferCapacity;
        int newCapacity = oldCapacity + (oldCapacity >> 1);
        ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity * edgeDataWriter.dataSize());
        ByteBuffer edgeDataBuffer = edgeDataBufferIO.getBuffer();
        edgeDataBuffer.position(0);
        newBuffer.put(edgeDataBuffer);
        edgeDataBufferIO = new ByteBufferDataIO(newBuffer);
        edgeDataBufferCapacity = newCapacity;
    }

    @Override
    public ED addEdgeTo(int nodeId, ED newData) {
        int newPos = edgeDataBufferSize;
        int pos = idPosMap.putIfAbsent(nodeId, newPos);
        if (pos >= 0) {
            ED prevData = getData(pos, null);
            setData(pos, newData);
            return prevData;
        } else {
            edgeDataBufferSize++;
            if (newPos >= edgeDataBufferCapacity)
                growEdgeDataBuffer();
            setData(newPos, newData);
            return null;
        }
    }

    @Override
    public boolean justAddEdgeTo(int nodeId, ED edgeData) {
        int newPos = edgeDataBufferSize;
        int pos = idPosMap.putIfAbsent(nodeId, newPos);
        if (pos < 0) {
            edgeDataBufferSize++;
            if (newPos >= edgeDataBufferCapacity)
                growEdgeDataBuffer();
            setData(newPos, edgeData);
            return true;
        } else {
            setData(pos, edgeData);
            return false;
        }
    }

    private void removeDataAt(final int posToRemove) {
        final int lastPos = --edgeDataBufferSize;
        int posCountToShift = lastPos - posToRemove;
        if (posCountToShift > 0) {
            int entrySize = edgeDataWriter.dataSize();
            ByteBuffers.shiftWithin(edgeDataBufferIO.getBuffer(),
                    posToRemove * entrySize, posCountToShift * entrySize, entrySize);
            idPosMap.replaceAll(new IntIntToIntFunction() {
                public int applyAsInt(int id, int pos) { return pos != lastPos ? pos : posToRemove; }
            });
        }
    }

    public ED removeEdgeTo(int nodeId) {
		int pos = idPosMap.remove(nodeId);
		ED prevData = null;
		if (pos >= 0) {
			prevData = getData(pos, null);
			removeDataAt(pos);
		}
		return prevData;
	}

	public boolean justRemoveEdgeTo(int nodeId) {
        int pos = idPosMap.remove(nodeId);
		if (pos >= 0) {
			removeDataAt(pos);
			return true;
		}
		return false;
	}

	public void forEach(final IntObjConsumer<? super ED> action) {
        idPosMap.forEach(new IntIntConsumer() {
            ED data = null;
            public void accept(int nodeId, int pos) {
                action.accept(nodeId, data = getData(pos, data));
            }
        });
	}

    @Override
    public boolean testWhile(final IntObjPredicate<? super ED> predicate) {
        return idPosMap.testWhile(new IntIntPredicate() {
            ED data = null;
            public boolean test(int nodeId, int pos) {
                return predicate.test(nodeId, data = getData(pos, data));
            }
        });
    }

    @Override
    public void forEachNodeId(IntConsumer action) {
        idPosMap.keySet().forEach(action);
    }

    @Override
    public boolean testNodeIdsWhile(IntPredicate predicate) {
        return idPosMap.keySet().testWhile(predicate);
    }

    public void writeData(DataOutput out) {
		final int entrySize = edgeDataWriter.dataSize();
        ByteBuffer edgeDataBuffer = edgeDataBufferIO.getBuffer();
        writeOrder(out, edgeDataBuffer.order());
        byte[] bufferBackingArray = edgeDataBuffer.array();
        int bufferOffset = edgeDataBuffer.arrayOffset();
        for (IntIntMapIterator it = idPosMap.mapIterator(); it.tryAdvance(); ) {
            int nodeId = it.intKey();
            out.writeInt(nodeId);
            out.write(bufferBackingArray, bufferOffset, entrySize);
            bufferOffset += entrySize;
        }
	}

    public IntKeyMapIterator<ED> iterator() {
		return new NodeIdEdgeIterator();
	}

	class NodeIdEdgeIterator implements IntKeyMapIterator<ED> {
		private final IntIntMapIterator idPosIterator;
        private final ByteBuffer dataBuffer;
		private ED edgeData;
		private boolean edgeDataActual;

		NodeIdEdgeIterator() {
			idPosIterator = idPosMap.mapIterator();
            dataBuffer = edgeDataBufferIO.getBuffer();
			edgeDataActual = false;
		}

		public int intKey() {
			return idPosIterator.intKey();
		}

		public ED value() {
			if (!edgeDataActual) {
				locateBuffer();
				edgeData = edgeDataWriter.readData(edgeDataBufferIO, edgeData);
				edgeDataActual = true;
			}
			return edgeData;
		}

		public void setValue(ED val) {
            locateBuffer();
			edgeDataWriter.writeData(edgeDataBufferIO, val);
			edgeData = val;
		}

        public boolean hasNext() {
            return idPosIterator.hasNext();
        }

        public boolean tryAdvance() {
            return idPosIterator.tryAdvance();
        }

        public Integer key() {
            return idPosIterator.key();
        }

        public void remove() {
            int pos = idPosIterator.intValue();
            idPosIterator.remove();
            removeDataAt(pos);
        }

        private void locateBuffer() {
            dataBuffer.position(idPosIterator.intValue() * edgeDataWriter.dataSize());
		}
	}
}
