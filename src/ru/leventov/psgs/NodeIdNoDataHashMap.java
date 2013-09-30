package ru.leventov.psgs;

import gnu.trove.TIntIterator;
import gnu.trove.function.IntConsumer;
import gnu.trove.function.IntObjConsumer;
import gnu.trove.function.IntObjPredicate;
import gnu.trove.function.IntPredicate;
import gnu.trove.map.IntKeyMapIterator;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.IntDHashSet;
import ru.leventov.psgs.io.DataInput;
import ru.leventov.psgs.io.DataOutput;
import ru.leventov.psgs.io.NoData;

public class NodeIdNoDataHashMap implements NodeIdEdgeMap<NoData> {
    private final int expectedSize;
    private final TIntSet idSet;

    public NodeIdNoDataHashMap(int expectedSize) {
        this.expectedSize = expectedSize;
        idSet = new IntDHashSet(expectedSize, NodeIdEdgeHashMap.DEFAULT_LOAD_FACTOR);
    }

    @Override
    public NoData addEdgeTo(int nodeId, NoData newData) {
        return idSet.add(nodeId) ? null : NoData.NO_DATA;
    }

    @Override
    public boolean justAddEdgeTo(int nodeId, NoData edgeData) {
        return idSet.add(nodeId);
    }

    @Override
    public NoData removeEdgeTo(int nodeId) {
        return idSet.remove(nodeId) ? NoData.NO_DATA : null;
    }

    @Override
    public boolean justRemoveEdgeTo(int nodeId) {
        return idSet.remove(nodeId);
    }

    @Override
    public int size() {
        return idSet.size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsNodeId(int nodeId) {
        return idSet.contains(nodeId);
    }

    @Override
    public NoData getEdgeData(int nodeId) {
        return idSet.contains(nodeId) ? NoData.NO_DATA : null;
    }

    @Override
    public IntKeyMapIterator<NoData> iterator() {
        return new IntKeyMapIterator<NoData>() {
            TIntIterator impl = idSet.iterator();
            public int intKey() {
                return impl.intValue();
            }

            @Override
            public boolean hasNext() {
                return impl.hasNext();
            }

            @Override
            public boolean tryAdvance() {
                return impl.tryAdvance();
            }

            @Override
            public Integer key() {
                return impl.value();
            }

            @Override
            public NoData value() {
                return NoData.NO_DATA;
            }

            @Override
            public void setValue(NoData value) {

            }

            @Override
            public void remove() {
                impl.remove();
            }
        };
    }

    @Override
    public void forEach(final IntObjConsumer<? super NoData> action) {
        idSet.forEach(new IntConsumer() {
            @Override
            public void accept(int targetId) {
                action.accept(targetId, NoData.NO_DATA);
            }
        });
    }

    @Override
    public boolean testWhile(final IntObjPredicate<? super NoData> predicate) {
        return idSet.testWhile(new IntPredicate() {
            @Override
            public boolean test(int targetId) {
                return predicate.test(targetId, NoData.NO_DATA);
            }
        });
    }

    @Override
    public void forEachNodeId(IntConsumer action) {
        idSet.forEach(action);
    }

    @Override
    public boolean testNodeIdsWhile(IntPredicate predicate) {
        return idSet.testWhile(predicate);
    }

    @Override
    public void readData(DataInput in) {
        int size = expectedSize;
        for (int i = size; i--> 0;) {
            idSet.add(in.readInt());
        }
        if (size() != size)
            throw new RuntimeException("Map data corrupted");
    }

    public void writeData(final DataOutput out) {
        idSet.forEach(new IntConsumer() {
            @Override
            public void accept(int nodeId) {
                out.writeInt(nodeId);
            }
        });
    }

}
