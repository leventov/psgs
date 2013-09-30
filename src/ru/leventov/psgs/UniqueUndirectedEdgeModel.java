package ru.leventov.psgs;

public abstract class UniqueUndirectedEdgeModel<N extends Node, ED> extends UniqueEdgeModel<N, N, ED> {

    public UniqueUndirectedEdgeModel(Graph graph) {
        super(graph);
    }

    @Override
    void putReverse(N source, int targetId, ED data) {
        // noinspection unchecked
        N target = (N) getNode(targetId);
        UniqueEdge<N, N, ED> reverseEdge = target.getUniqueEdge(this, true);
        int oldTargetId = reverseEdge.getTargetId();
        if (oldTargetId != source.getId()) {
            if (oldTargetId != 0)
                removeReverse(target, oldTargetId);
            reverseEdge.setTargetAndData(source.getId(), data);
        }
    }

    @Override
    void putReverse(N source, N target, ED data) {
        UniqueEdge<N, N, ED> reverseEdge = target.getUniqueEdge(this, true);
        int oldTargetId = reverseEdge.getTargetId();
        if (oldTargetId != source.getId()) {
            target.onChange();
            if (oldTargetId != 0)
                removeReverse(target, oldTargetId);
            reverseEdge.setTargetAndData(source.getId(), data);
        }
    }

    @Override
    void removeReverse(N source, int targetId) {
        getNode(targetId).removeUniqueEdge(getId());
    }

    @Override
    void removeReverse(N source, N target) {
        target.removeUniqueEdge(getId());
    }
}
