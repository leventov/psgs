package ru.leventov.psgs;

import org.jetbrains.annotations.NotNull;
import ru.leventov.psgs.io.DataWriter;

public abstract class EdgeModel<S extends Node, T extends Node, ED>
        extends AbstractEdgeModel<S, T, ED> implements DataWriter<ED> {

    EdgeModel(Graph graph) {
        super(graph);
    }

    public final boolean isUnique() {
        return false;
    }

    @NotNull
    public final <S_SUB extends S, T_SUB extends T> Edges<S_SUB, T_SUB, ED> from(S_SUB source) {
        checkSameGraph(source);
        return source.getEdges(this);
    }

    @NotNull
    protected NodeIdEdgeMap<ED> newMap(int expectedSize) {
        return new NodeIdEdgeHashMap<>(expectedSize, this);
    }

    @NotNull
    protected NodeIdEdgeMap<ED> newMapForDeserialization(int size) {
        return new NodeIdEdgeHashMap<>(size, this);
    }
}
