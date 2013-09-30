package ru.leventov.psgs;

public abstract class DirectedReversedUniqueEdgeModel<
        THIS extends DirectedReversedUniqueEdgeModel<THIS, REVERSED, S, T, ED>,
        REVERSED extends UniqueDirectedEdgeModel<REVERSED, THIS, T, S, ED>,
        S extends Node, T extends Node, ED>
        extends AbstractDirectedEdgeModel<REVERSED, S, T, ED> {

    public DirectedReversedUniqueEdgeModel(Graph graph) {
        super(graph);
    }

    @Override
    void putReverse(S source, int targetId, ED data) {
        // noinspection unchecked
        T target = (T) getNode(targetId);
        UniqueEdge<T, S, ED> uniqueEdge = target.getUniqueEdge(getReverse(), true);
        int oldTargetId = uniqueEdge.getTargetId();
        if (oldTargetId != source.getId()) {
            if (oldTargetId != 0)
                getReverse().removeReverse(target, oldTargetId);
            uniqueEdge.setTargetAndData(source.getId(), data);
        }
    }

    @Override
    void putReverse(S source, T target, ED data) {
        UniqueEdge<T, S, ED> uniqueEdge = target.getUniqueEdge(getReverse(), true);
        int oldTargetId = uniqueEdge.getTargetId();
        if (oldTargetId != source.getId()) {
            target.onChange();
            if (oldTargetId != 0)
                getReverse().removeReverse(target, oldTargetId);
            uniqueEdge.setTargetAndData(source.getId(), data);
        }
    }

    @Override
    void removeReverse(S source, int targetId) {
        getNode(targetId).removeUniqueEdge(getReverseId());
    }

    @Override
    void removeReverse(S source, T target) {
        target.removeUniqueEdge(getReverseId());
    }
}
