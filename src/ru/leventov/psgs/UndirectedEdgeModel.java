package ru.leventov.psgs;

public abstract class UndirectedEdgeModel<N extends Node, ED> extends EdgeModel<N, N, ED> {

    public UndirectedEdgeModel(Graph graph) {
        super(graph);
    }

    @Override
    void putReverse(N source, int targetId, ED data) {
        getNode(targetId).getEdges(this).addReverse(source.getId(), data);
    }

    @Override
    void putReverse(N source, N target, ED data) {
        target.getEdges(this).addReverse(source.getId(), data);
    }

    @Override
    void removeReverse(N source, int targetId) {
        getNode(targetId).getEdges(this).removeReverse(source.getId());
    }

    @Override
    void removeReverse(N source, N target) {
        target.getEdges(this).removeReverse(source.getId());
    }

}
