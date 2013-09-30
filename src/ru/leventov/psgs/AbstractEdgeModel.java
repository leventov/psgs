package ru.leventov.psgs;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public abstract class AbstractEdgeModel<S extends Node, T extends Node, ED> {

    static <EM extends AbstractEdgeModel<?, ?, ?>> EM newModel(
            Class<EM> modelClass, Graph graph) {
        try {
            Constructor<EM> c = modelClass.getDeclaredConstructor(Graph.class);
            return c.newInstance(graph);
        } catch (NoSuchMethodException | InvocationTargetException |
                InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(
                    "Edge model class must have a public constructor with a solo Graph argument", e);
        }
    }

    private final Graph graph;
    private final byte id;

    AbstractEdgeModel(Graph graph) {
        this.graph = graph;
        id = graph.getEdgeModelId(getClass());
    }

    public final Graph getGraph() {
        return graph;
    }

    final Node getNode(int id) {
        return graph.getNodeForChange(id);
    }

    final byte getId() {
        return id;
    }

    final void checkSameGraph(Node node) {
        if (getGraph() == node.getGraph())
            return;
        throw new IllegalArgumentException("Node and edge model should be from the same graph");
    }

    abstract void putReverse(S source, int targetId, ED data);

    abstract void putReverse(S source, T target, ED data);

    abstract void removeReverse(S source, int targetId);

    abstract void removeReverse(S source, T target);

    abstract boolean isUnique();
}
