package ru.leventov.psgs;

abstract class AbstractDirectedEdgeModel<
        REVERSED extends AbstractEdgeModel<T, S, ED>,
        S extends Node, T extends Node, ED>
        extends EdgeModel<S, T, ED> {

    private final byte reverseId;
    private final REVERSED reverseModel;

    AbstractDirectedEdgeModel(Graph graph) {
        super(graph);
        reverseModel = newModel(getReverseClass(), graph);
        reverseId = reverseModel.getId();
    }

    protected abstract Class<REVERSED> getReverseClass();

    final byte getReverseId() {
        return reverseId;
    }

    final REVERSED getReverse() {
        return reverseModel;
    }
}
