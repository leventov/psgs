package ru.leventov.psgs;

public abstract class UniqueDirectedEdgeModel<
        THIS extends UniqueDirectedEdgeModel<THIS, REVERSED, S, T, ED>,
        REVERSED extends DirectedReversedUniqueEdgeModel<REVERSED, THIS, T, S, ED>,
        S extends Node, T extends Node, ED>
        extends AbstractUniqueDirectedEdgeModel<REVERSED, S, T, ED> {

    public UniqueDirectedEdgeModel(Graph graph) {
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
