package ru.leventov.psgs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.leventov.psgs.io.DataInput;
import ru.leventov.psgs.io.DataOutput;
import ru.leventov.psgs.io.DataWritable;

import java.io.IOException;
import java.util.Arrays;

public abstract class Node implements DataWritable {

    public static Node newNode(Class<?> nodeClass) {
        try {
            return (Node) nodeClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Node class must have a public no-args constructor", e);
        }
    }

	private int id;
	private Graph graph;
    private byte classId;
    private boolean changed;

	private byte cachedEdgeModelId;
	private Edges<?, ?, ?> cachedEdges;

    private byte cachedUniqueEdgeModelId;
    private UniqueEdge<? extends Node, ? extends Node, ?> cachedUniqueEdge;

    private byte[] modelIds;

    private Object[] adjacent;

	void addToGraph(int id, Graph graph, byte classId) {
        this.id = id;
        this.graph = graph;
        this.classId = classId;
    }

	void setAdjacent(byte[] classIds, Object[] adjacent) {
        this.modelIds = classIds;
        this.adjacent = adjacent;
	}

	public final int getId() {
		return id;
	}

	public final Graph getGraph() {
		return graph;
	}

    byte getClassId() {
        return classId;
    }

    static interface EdgesConsumer {
        void accept(byte modelId, Edges<?, ?, ?> edges) throws IOException;
    }

    static interface UniqueEdgeConsumer {
        void accept(byte modelId, UniqueEdge<?, ?, ?> edge) throws IOException;
    }

    void forEachAdjacent(EdgesConsumer edgesAction,
                         UniqueEdgeConsumer uniqueEdgeAction) throws IOException {
        byte[] modelIds = this.modelIds;
        if (modelIds == null) {
            if (cachedEdgeModelId != 0)
                edgesAction.accept(cachedEdgeModelId, cachedEdges);
            if (cachedUniqueEdgeModelId != 0)
                uniqueEdgeAction.accept(cachedUniqueEdgeModelId, cachedUniqueEdge);
            return;
        }
        Object[] adjacent = this.adjacent;
        for (int i = modelIds.length; i-- > 0;) {
            Object adj = adjacent[i];
            if (adj.getClass() == Edges.class) {
                edgesAction.accept(modelIds[i], (Edges<?, ?, ?>) adj);
            } else {
                uniqueEdgeAction.accept(modelIds[i], (UniqueEdge<?, ?, ?>) adj);
            }
        }
    }

    @Override
    public void readData(DataInput in) {
    }

    @Override
    public void writeData(DataOutput out) {
    }

    /**
     * Call from edges and edge models
     */
    protected final void onChange() {
        if (!changed) {
            graph.nodeChanged(this);
            changed = true;
        }
    }

    /**
     * Call only from Graph subclasses
     */
    final void changed() {
        changed = true;
    }

    void removeFromGraph() {
        byte[] modelIds = this.modelIds;
        if (modelIds == null) {
            if (cachedEdgeModelId != 0)
                cachedEdges.removeAll();
            if (cachedUniqueEdgeModelId != 0)
                // noinspection unchecked
                ((UniqueEdgeModel) cachedUniqueEdge.getModel()).removeReverse(this, cachedUniqueEdge.getTargetId());
            return;
        }
        Object[] adjacent = this.adjacent;
        for (int i = modelIds.length; i-- > 0;) {
            Object adj = adjacent[i];
            if (adj.getClass() == Edges.class) {
                ((Edges<?, ?, ?>) adj).removeAll();
            } else {
                UniqueEdge edge = (UniqueEdge) adj;
                // noinspection unchecked
                edge.getModel().removeReverse(this, edge.getTargetId());
            }
        }
        this.modelIds = null;
        this.adjacent = null;
        cachedEdgeModelId = 0;
        cachedEdges = null;
        cachedUniqueEdgeModelId = 0;
        cachedUniqueEdge = null;

        // id is kept to prevent adding to another graph
        // id = 0;
        graph = null;
        classId = 0;
    }

    @NotNull
    <S extends Node, T extends Node, ED>
    Edges<S, T, ED> getEdges(EdgeModel<? super S, ? super T, ED> edgeModel) {
        edgeModel.checkSameGraph(this);
        byte edgeModelId = edgeModel.getId();
        if (cachedEdgeModelId == edgeModelId) {
            // noinspection unchecked
            return (Edges<S, T, ED>) cachedEdges;
        } else {
            int modelIndex = modelIds != null ? indexOf(modelIds, edgeModelId) : -1;
            Edges<S, T, ED> edges;
            if (modelIndex >= 0) {
                // noinspection unchecked
                edges = (Edges<S, T, ED>) adjacent[modelIndex];
            } else {
                // noinspection unchecked
                edges = new Edges<>(edgeModel, (S) this, null);
                addEdges(edgeModelId, edges);
            }
            cachedEdgeModelId = edgeModelId;
            cachedEdges = edges;
            return edges;
        }
    }

    @Nullable
    <S extends Node, T extends Node, ED> UniqueEdge<S, T, ED> getUniqueEdge(
            UniqueEdgeModel<S, T, ED> edgeModel, boolean create) {
        byte uniqueEdgeModelId = edgeModel.getId();
        if (cachedUniqueEdgeModelId == uniqueEdgeModelId) {
            // noinspection unchecked
            return (UniqueEdge<S, T, ED>) cachedUniqueEdge;
        } else {
            int modelIndex = indexOf(modelIds, uniqueEdgeModelId);
            if (modelIndex >= 0) {
                cachedUniqueEdgeModelId = uniqueEdgeModelId;
                // noinspection unchecked
                UniqueEdge<S, T, ED> edge = (UniqueEdge<S, T, ED>) adjacent[modelIndex + 1];
                cachedUniqueEdge = edge;
                return edge;
            }
            if (create) {
                onChange();
                // noinspection unchecked
                UniqueEdge<S, T, ED> edge = new UniqueEdge<>(edgeModel, (S) this);
                addUniqueEdge(uniqueEdgeModelId, edge);
                return edge;
            }
            return null;
        }
    }

    boolean hasUniqueClass(byte uniqueEdgeModelId) {
        return cachedUniqueEdgeModelId == uniqueEdgeModelId || indexOf(modelIds, uniqueEdgeModelId) >= 0;
    }
    
    void addEdges(byte edgeModelId, Edges<?, ?, ?> edges) {
        if (modelIds != null || cachedEdgeModelId != 0)
            appendModel(edgeModelId, edges);
        cachedEdgeModelId = edgeModelId;
        cachedEdges = edges;
    }

    void addUniqueEdge(byte uniqueEdgeModelId, UniqueEdge<? extends Node, ? extends Node, ?> edge) {
        if (modelIds != null || cachedUniqueEdgeModelId != 0)
            appendModel(uniqueEdgeModelId, edge);
        cachedUniqueEdgeModelId = uniqueEdgeModelId;
        cachedUniqueEdge = edge;
    }

    private void appendModel(byte newModelId, Object value) {
        byte[] modelIds = this.modelIds;
        if (modelIds != null) {
            int modelCount = modelIds.length;
            byte[] newModelIds = Arrays.copyOf(modelIds, modelCount + 1);
            newModelIds[modelCount] = newModelId;
            this.modelIds = newModelIds;
            Object[] newAdjacent = Arrays.copyOf(adjacent, modelCount + 1);
            newAdjacent[modelCount] = value;
            adjacent = newAdjacent;
        } else if (cachedUniqueEdgeModelId != 0) {
            if (cachedEdgeModelId != 0) {
                this.modelIds = new byte[] {cachedEdgeModelId, cachedUniqueEdgeModelId, newModelId};
                adjacent = new Object[] {cachedEdges, cachedUniqueEdge, value};
            } else {
                this.modelIds = new byte[] {cachedUniqueEdgeModelId, newModelId};
                adjacent = new Object[] {cachedUniqueEdge, value};
            }
        } else if (cachedEdgeModelId != 0) {
            this.modelIds = new byte[] {cachedEdgeModelId, newModelId};
            adjacent = new Object[] {cachedEdges, value};
        }
    }

    int removeUniqueEdge(byte uniqueEdgeModelId) {
        byte[] modelIds = this.modelIds;
        Object edge = null;
        if (modelIds != null) {
            if (modelIds.length != 1) {
                edge = removeAdjacentByModelId(uniqueEdgeModelId);
            } else if (modelIds[0] == uniqueEdgeModelId) {
                edge = adjacent[1];
                this.modelIds = null;
                this.adjacent = null;
            }
        }
        if (cachedUniqueEdgeModelId == uniqueEdgeModelId) {
            cachedUniqueEdgeModelId = 0;
            edge = cachedUniqueEdge;
            cachedUniqueEdge = null;
        }
        if (edge != null) {
            onChange();
            return ((Edge) edge).getTargetId();
        } else {
            return 0;
        }
    }

    @Nullable
    private Object removeAdjacentByModelId(byte modelId) {
        byte[] modelIds = this.modelIds;
        int modelIndex = indexOf(modelIds, modelId);
        if (modelIndex < 0)
            return null;

        int modelCount = modelIds.length;
        byte[] newModelIds = new byte[modelCount - 1];
        System.arraycopy(modelIds, 0, newModelIds, 0, modelCount - 1);
        System.arraycopy(modelIds, modelIndex + 1, newModelIds, modelIndex, modelCount - modelIndex - 1);
        this.modelIds = newModelIds;

        Object[] adjacent = this.adjacent;
        Object[] newAdjacent = new Object[modelCount - 1];
        System.arraycopy(adjacent, 0, newAdjacent, 0, modelIndex - 1);
        System.arraycopy(adjacent, modelIndex + 1, newAdjacent, modelIndex, modelCount - modelIndex - 1);
        this.adjacent = newAdjacent;

        return adjacent[modelIndex];
    }

    private static int indexOf(byte[] modelIds, byte modelId) {
        int i = modelIds.length - 1;
        while (i >= 0 && modelIds[i] != modelId) i--;
        return i;
    }
}