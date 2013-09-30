package ru.leventov.psgs;

import gnu.trove.function.IntConsumer;
import gnu.trove.function.IntObjConsumer;
import gnu.trove.function.IntObjPredicate;
import gnu.trove.function.IntPredicate;
import gnu.trove.map.IntKeyMapIterator;
import ru.leventov.psgs.io.DataWritable;

public interface NodeIdEdgeMap<ED> extends DataWritable {
	/** @return previous edge data, mapped for this nodeId in the map, or null */
	ED addEdgeTo(int nodeId, ED newData);

	/** @return true if mapping for the nodeId was NOT presented in the map */
    boolean justAddEdgeTo(int nodeId, ED edgeData);

	/** @return edge data, mapped for this nodeId in the map, or null */
    ED removeEdgeTo(int nodeId);

    /** @return true if the map contained the nodeId */
    boolean justRemoveEdgeTo(int nodeId);

    int size();

    boolean isEmpty();

    boolean containsNodeId(int nodeId);

    ED getEdgeData(int nodeId);

    /** Read-only iterator */
    IntKeyMapIterator<ED> iterator();

    void forEach(IntObjConsumer<? super ED> action);

    boolean testWhile(IntObjPredicate<? super ED> predicate);

    void forEachNodeId(IntConsumer action);

    boolean testNodeIdsWhile(IntPredicate predicate);
}
