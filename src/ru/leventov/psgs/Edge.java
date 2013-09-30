package ru.leventov.psgs;

public abstract class Edge<S extends Node, T extends Node, ED> {
    final AbstractEdgeModel<? super S, ? super T, ED> edgeModel;
    final S source;
    private int targetId;
    private ED data;

    public Edge(AbstractEdgeModel<? super S, ? super T, ED> edgeModel, S source) {
        this.edgeModel = edgeModel;
        this.source = source;
    }

    void setTargetAndData(int targetId, ED data) {
        this.targetId = targetId;
        this.data = data;
    }

    public final S getSource() {
        return source;
    }

    public final int getSourceId() {
        return source.getId();
    }

    public final T getTarget() {
        // noinspection unchecked
        return (T) source.getGraph().getNode(targetId);
    }

    public final int getTargetId() {
        return targetId;
    }

    public final ED getData() {
        return data;
    }

    public ED setData(ED newData) {
        ED prevData = data;
        justSetData(newData);
        return prevData;
    }

    public void justSetData(ED newData) {
        source.onChange();
        data = newData;
        edgeModel.putReverse(source, targetId, newData);
    }
}
