package ru.leventov.psgs;

public abstract class ArcModel<S extends Node, T extends Node, ED> extends EdgeModel<S, T, ED> {

    public ArcModel(Graph graph) {
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
