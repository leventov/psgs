package ru.leventov.psgs;

public abstract class DirectedEdgeModel<
        THIS extends DirectedEdgeModel<THIS, REVERSED, S, T, ED>,
        REVERSED extends DirectedEdgeModel<REVERSED, THIS, T, S, ED>,
        S extends Node, T extends Node, ED>
        extends AbstractDirectedEdgeModel<REVERSED, S, T, ED> {

    public DirectedEdgeModel(Graph graph) {
        super(graph);
    }

    @Override
    void putReverse(S source, int targetId, ED data) {
        getNode(targetId).getEdges(getReverse()).addReverse(source.getId(), data);
    }

    @Override
    void putReverse(S source, T target, ED data) {
        target.getEdges(getReverse()).addReverse(source.getId(), data);
    }

    @Override
    void removeReverse(S source, int targetId) {
        getNode(targetId).getEdges(getReverse()).removeReverse(source.getId());
    }

    @Override
    void removeReverse(S source, T target) {
        target.getEdges(getReverse()).removeReverse(source.getId());
    }
}
