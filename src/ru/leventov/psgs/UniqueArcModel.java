package ru.leventov.psgs;

public abstract class UniqueArcModel<S extends Node, T extends Node, ED> extends UniqueEdgeModel<S, T, ED> {

    public UniqueArcModel(Graph graph) {
        super(graph);
    }

    @Override
    void putReverse(S source, int targetId, ED data) {}

    @Override
    void putReverse(S source, T target, ED data) {}

    @Override
    void removeReverse(S source, int targetId) {}

    @Override
    void removeReverse(S source, T target) {}

}
