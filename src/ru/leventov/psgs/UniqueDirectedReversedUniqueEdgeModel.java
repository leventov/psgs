package ru.leventov.psgs;

public abstract class UniqueDirectedReversedUniqueEdgeModel<
        THIS extends UniqueDirectedReversedUniqueEdgeModel<THIS, REVERSED, S, T, ED>,
        REVERSED extends UniqueDirectedReversedUniqueEdgeModel<REVERSED, THIS, T, S, ED>,
        S extends Node, T extends Node, ED>
        extends AbstractUniqueDirectedEdgeModel<REVERSED, S, T, ED> {

    public UniqueDirectedReversedUniqueEdgeModel(Graph graph) {
        super(graph);
    }

    protected UniqueDirectedReversedUniqueEdgeModel(Graph graph, REVERSED reverseModel) {
        super(graph, reverseModel);
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
