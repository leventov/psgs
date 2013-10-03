package ru.leventov.psgs;

import gnu.trove.function.Consumer;
import gnu.trove.function.IntFunction;
import gnu.trove.function.IntObjConsumer;
import gnu.trove.map.hash.IntObjDHashMap;
import gnu.trove.map.hash.TIntObjHashMap;
import org.jetbrains.annotations.Nullable;
import ru.leventov.psgs.index.BTreeIndex;
import ru.leventov.psgs.index.ExistingBTreeIndex;
import ru.leventov.psgs.io.*;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.Map;

import static java.nio.file.Files.createDirectories;
import static ru.leventov.psgs.AbstractEdgeModel.newModel;
import static ru.leventov.psgs.util.Bits.unsignedByte;
import static ru.leventov.psgs.util.Bits.unsignedInt;
import static ru.leventov.psgs.util.ByteBuffers.skip;

public class ExistingGraph extends Graph implements Closeable {
    private static final Node REMOVED = new Node() {
        @Override
        public void readData(DataInput in) {
        }

        @Override
        public void writeData(DataOutput out) {
        }
    };

    private static <T> TIntObjHashMap<T> createMap() {
        return new IntObjDHashMap<>(10, 0.8f);
    }

    public static ExistingGraph openForReading(Path dir) throws IOException, DeserializationException  {
        ExistingGraph graph = new ExistingGraph();
        graph.readOnly = true;
        Metadata metadata = Json.readJson(metadataFile(dir), Metadata.class);
        graph.commonInit(metadata);
        graph.nodeIndex = new ExistingBTreeIndex(nodeIndexDir(dir), true);
        graph.data = new MemoryMappedFile(dataFile(dir), metadata.byteOrder,
                MemoryMappedFile.MAX_CHUNK_SIZE_LIMIT, true);
        return graph;
    }

    public static ExistingGraph copyForUpdating(Path srcDir, Path dstDir) throws IOException, DeserializationException {
        Files.removeDir(dstDir);
        createDirectories(dstDir);
        BTreeIndex.copy(nodeIndexDir(srcDir), nodeIndexDir(dstDir));
        ExistingGraph graph = new ExistingGraph();
        graph.readOnly = false;
        graph.dstDir = dstDir;
        Metadata metadata = Json.readJson(metadataFile(srcDir), Metadata.class);
        graph.commonInit(metadata);
        graph.nodeIndex = new ExistingBTreeIndex(nodeIndexDir(dstDir), false);
        graph.data = new MemoryMappedFile(dataFile(srcDir), metadata.byteOrder,
                MemoryMappedFile.MAX_CHUNK_SIZE_LIMIT, true);
        return graph;
    }

    private boolean readOnly;
    private ByteOrder byteOrder;
    private Path dstDir;

    private ExistingBTreeIndex nodeIndex;
    private MemoryMappedFile data;

    private boolean[] edgeModelUnique = new boolean[256];
    private UniqueEdgeModel[] uniqueEdgeModels = new UniqueEdgeModel[256];
    private EdgeModel[] edgeModels = new EdgeModel[256];

    private static final int NODE_CACHE_SIZE = 1 << 18;
    private static int cacheIndex(int id) {
        int x = id;
        x ^= (x >>> 16);
        x *= 0x85ebca6b;
        x ^= x >>> 13;
        x *= 0xc2b2ae35;
        x ^= x >>> 16;
        return x & (NODE_CACHE_SIZE - 1);
    }
    private Node[] loadedNodes = new Node[NODE_CACHE_SIZE];

    @Nullable
    private TIntObjHashMap<Node> newNodes = null;

    private ExistingGraph() {}

    private void commonInit(Metadata metadata) throws IOException, DeserializationException {
        // read metadata


        // pre checks
        if (metadata.protocolVersion > CURRENT_PROTOCOL_VERSION) {
            throw new DeserializationException(
                    "Highest supported version of protocol is " + CURRENT_PROTOCOL_VERSION + ", " +
                    metadata.protocolVersion + " found.");
        }
        if (!metadata.format.equals(defaultFormat())) {
            throw new DeserializationException(
                    "Only \"" + defaultFormat() + "\" serialization format is currently supported, " +
                    metadata.format + " found.");
        }

       init(metadata);
    }

    @Override
    void init(Metadata metadata) throws DeserializationException {
        super.init(metadata);
        byteOrder = metadata.byteOrder;
        try {
            for (Map.Entry<String, Byte> e : metadata.edgeClasses.entrySet()) {
                Class<? extends AbstractEdgeModel<?, ?, ?>> modelClass =
                        (Class<? extends AbstractEdgeModel<?, ?, ?>>) Class.forName(e.getKey());
                int modelId = unsignedByte(e.getValue());
                if (UniqueEdgeModel.class.isAssignableFrom(modelClass)) {
                    edgeModelUnique[modelId] = true;
                    uniqueEdgeModels[modelId] = (UniqueEdgeModel) newModel(modelClass, this);
                } else {
                    edgeModels[modelId] = (EdgeModel) newModel(modelClass, this);
                }
            }
        } catch (ClassNotFoundException e) {
            throw new DeserializationException(e);
        }
    }

    @Override
    public long nodeCount() {
        return nodeCount;
    }

    @Override
    public Node getNode(int nodeId) {
        if (nodeId == 0)
            throw new IllegalArgumentException("Node id couldn't be 0.");

        Node node = getNew(nodeId);
        if (node == REMOVED) return null;
        if (node != null) return node;

        node = getLoaded(nodeId);
        if (node != null) return node;

        ByteBuffer descriptor = nodeIndex.get(nodeId);
        if (descriptor == null) return null;

        node = loadNode(nodeId, descriptor);

        putLoaded(nodeId, node);
        return node;
    }

    private Node loadNode(int nodeId, ByteBuffer descriptor) {
        byte nodeClassId = descriptor.get();
        int adjCount = unsignedByte(descriptor.get());
        long dataOffset = read6BytesDataOffset(descriptor);

        Node node = Node.newNode(getNodeClass(nodeClassId));
        node.addToGraph(nodeId, this, nodeClassId);

        ByteBuffer dataBuffer = data.locateChunk(dataOffset);
        DataInput dataIn = new ByteBufferDataIO(dataBuffer);

        int nodeDataSize = dataBuffer.getInt();
        int nodeDataStartPos = dataBuffer.position();
        node.readData(dataIn);
        dataBuffer.position(nodeDataStartPos + nodeDataSize);

        for (int i = 0; i < adjCount; i++) {
            int edgeModelId = dataBuffer.getInt();
            int edgeDataSize = dataBuffer.getInt();
            int edgeDataStartPos = dataBuffer.position();
            if (!edgeModelUnique[edgeModelId]) {
                int size = dataBuffer.getInt();
                EdgeModel edgeModel = edgeModels[edgeModelId];
                NodeIdEdgeMap adjacentMap = edgeModel.newMapForDeserialization(size);
                adjacentMap.readData(dataIn);
                Edges edges = new Edges(edgeModel, node, adjacentMap);
                node.addEdges((byte) edgeModelId, edges);
            } else {
                int targetId = dataBuffer.getInt();
                UniqueEdgeModel uniqueEdgeModel = uniqueEdgeModels[edgeModelId];
                UniqueEdge uniqueEdge = new UniqueEdge(uniqueEdgeModel, node);
                Object data = uniqueEdgeModel.readData(dataIn, null);
                uniqueEdge.setTargetAndData(targetId, data);
                node.addUniqueEdge((byte) edgeModelId, uniqueEdge);
            }
            dataBuffer.position(edgeDataStartPos + edgeDataSize);
        }
        return node;
    }

    private Node getLoaded(int id) {
        Node node = loadedNodes[cacheIndex(id)];
        if (node != null && node.getId() == id) return node;
        return null;
    }

    private void putLoaded(int id, Node node) {
        loadedNodes[cacheIndex(id)] = node;
    }

    private Node removeLoaded(int nodeId) {
        int index = cacheIndex(nodeId);
        Node node = loadedNodes[index];
        if (node != null && node.getId() == nodeId) {
            loadedNodes[index] = null;
            return node;
        } else {
            return null;
        }
    }

    private Node getNew(int id) {
        return newNodes != null ? newNodes.get(id) : null;
    }

    @Override
    public void addNode(Node node) {
        Graph nodeGraph = node.getGraph();
        if (nodeGraph == null && node.getId() == 0) {
            TIntObjHashMap<Node> newNodes = getNewNodesForInsert();
            int id = (int) ++maxNodeIdBound;
            Node n;
            if (id == 0) {
                do {
                    id++;
                    n = newNodes.get(id);
                } while (n != null && n != REMOVED);
            }
            newNodes.put(id, node);
            node.addToGraph(id, this, getNodeClassId(node.getClass()));
            nodeCount++;
        } else {
            throw new IllegalArgumentException("Node couldn't be contained in 2 graphs simultaneously");
        }
    }

    @Override
    public Node getOrCreateNode(int id, IntFunction<Node> producer) {
        Node node = getNode(id);
        if (node != null) return node;

        node = producer.apply(id);
        if (node.getGraph() == null && node.getId() == 0) {
            TIntObjHashMap<Node> newNodes = getNewNodesForInsert();
            newNodes.put(id, node);
            node.addToGraph(id, this, getNodeClassId(node.getClass()));
            if (maxNodeIdBound != 0) {
                maxNodeIdBound = Math.max(maxNodeIdBound, unsignedInt(id));
                minNodeIdBound = Math.min(minNodeIdBound, unsignedInt(id));
            } else {
                maxNodeIdBound = unsignedInt(id);
                minNodeIdBound = unsignedInt(id);
            }
            nodeCount++;
            return node;
        } else {
            throw new IllegalArgumentException("Node couldn't be contained in 2 graphs simultaneously");
        }
    }

    @Override
    public boolean isNodeIdUsed(int nodeId) {
        if (nodeId == 0)
            throw new IllegalArgumentException("Node id couldn't be 0.");

        Node node = getNew(nodeId);
        if (node == REMOVED) return false;
        if (node != null) return true;

        node = getLoaded(nodeId);
        if (node != null) return true;

        ByteBuffer descriptor = nodeIndex.get(nodeId);
        return descriptor != null;
    }

    @Override
    public void removeNode(Node node) {
        if (node.getGraph() != this)
            throw new IllegalArgumentException();
        int id = node.getId();
        boolean maybeInLoaded = true;
        if (newNodes != null) {
            Node removedNew = newNodes.remove(id);
            if (removedNew != null && removedNew != REMOVED) {
                node.removeFromGraph();
                nodeCount--;
                return;
            } else if (removedNew == REMOVED) {
                maybeInLoaded = false;
            }
        }
        if (maybeInLoaded && removeLoaded(id) != null) {
            node.removeFromGraph();
            nodeCount--;
            TIntObjHashMap<Node> newNodes = getNewNodesForInsert();
            newNodes.put(id, REMOVED);
        } else {
            throw new IllegalStateException("The graph must contain the node");
        }
    }

    private TIntObjHashMap<Node> getNewNodesForInsert() {
        if (newNodes == null) newNodes = createMap();
        return newNodes;
    }

    @Override
    public Node removeNode(int nodeId) {
        Node node = getNode(nodeId);
        if (node != null) {
            removeNode(node);
        }
        return node;
    }

    @Override
    public void forEachNode(final Consumer<? super Node> action) {
        for (Node node : loadedNodes) {
            if (node != null)
                action.accept(node);
        }
        nodeIndex.forEachEntry(new IntObjConsumer<ByteBuffer>() {
            @Override
            public void accept(int nodeId, ByteBuffer descriptor) {
                if (getLoaded(nodeId) == null && (newNodes == null || !newNodes.containsKey(nodeId))) {
                    action.accept(loadNode(nodeId, descriptor));
                }
            }
        });
        if (newNodes != null) {
            newNodes.values().forEach(new Consumer<Node>() {
                @Override
                public void accept(Node node) {
                    if (node != REMOVED) action.accept(node);
                }
            });
        }
    }

    private void serialize() throws IOException {
        try (ExistingGraphSerializationState serializationState = new ExistingGraphSerializationState()) {
            nodeIndex.forEachEntry(new IntObjConsumer<ByteBuffer>() {
                @Override
                public void accept(int nodeId, ByteBuffer descriptor) {
                    try {
                        if (newNodes == null || !newNodes.containsKey(nodeId)) {
                            serializationState.moveNode(descriptor);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            System.out.printf("%2.1f%% moved nodes\n", (100.0 * serializationState.movedNodes) / nodeCount());
            if (newNodes != null) {
                newNodes.values().forEach(new Consumer<Node>() {
                    @Override
                    public void accept(Node node) {
                        try {
                            if (node != REMOVED) serializationState.writeNode(node);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
            long newNodesCount = nodeCount() - serializationState.movedNodes;
            System.out.printf("%2.1f%% new nodes\n", (100.0 * newNodesCount) / nodeCount());
            System.out.println();
        }
    }

    @Override
    Node getNodeForChange(int nodeId) {
        if (nodeId == 0)
            throw new IllegalArgumentException("Node id couldn't be 0.");

        Node node = getNew(nodeId);
        if (node == REMOVED) return null;
        if (node != null) {
            node.changed();
            return node;
        }

        node = removeLoaded(nodeId);
        if (node != null) {
            TIntObjHashMap<Node> newNodes = getNewNodesForInsert();
            newNodes.put(nodeId, node);
            node.changed();
            return node;
        }

        ByteBuffer descriptor = nodeIndex.get(nodeId);
        if (descriptor == null) return null;

        node = loadNode(nodeId, descriptor);

        TIntObjHashMap<Node> newNodes = getNewNodesForInsert();
        newNodes.put(nodeId, node);
        node.changed();
        return node;
    }

    @Override
    void nodeChanged(Node node) {
        int nodeId = node.getId();
        if (removeLoaded(nodeId) != null) {
            TIntObjHashMap<Node> newNodes = getNewNodesForInsert();
            newNodes.put(nodeId, node);
        }
    }

    class ExistingGraphSerializationState extends SerializationState {
        long movedNodes = 0;
        ExistingGraphSerializationState() throws IOException {
            super(dstDir, byteOrder, defaultFormat(), ExistingGraph.this.nodeIndex);
        }

        void moveNode(ByteBuffer descriptor) throws IOException {
            movedNodes++;
            byte nodeClassId = descriptor.get();
            statsAggregator.aggregateNode(nodeClassId);
            int adjCount = unsignedByte(descriptor.get());
            long dataOffset = read6BytesDataOffset(descriptor);

            long newDataOffset = dataChannel.position();
            descriptor.position(descriptor.position() - 6);
            write6BytesDataOffset(descriptor, newDataOffset);

            ByteBuffer dataBuffer = data.locateChunk(dataOffset);
            int dataStartPos = dataBuffer.position();
            int nodeDataSize = dataBuffer.getInt();
            skip(dataBuffer, nodeDataSize);

            for (int i = 0; i < adjCount; i++) {
                int moveStartPos = dataBuffer.position();
                int edgeModelId = dataBuffer.getInt();
                int edgeDataSize = dataBuffer.getInt();
                if (edgeModelUnique[edgeModelId]) {
                    statsAggregator.aggregateUniqueEdge((byte) edgeModelId);
                } else {
                    int edgeCount = dataBuffer.getInt();
                    statsAggregator.aggregateEdges((byte) edgeModelId, edgeCount);
                }
                dataBuffer.position(moveStartPos + 8 + edgeDataSize);
            }

            dataBuffer.limit(dataBuffer.position());
            dataBuffer.position(dataStartPos);
            dataChannel.write(dataBuffer);
        }

        @Override
        public void close() throws IOException {
            ExistingGraph.this.nodeIndex.close();
            writeMetadata();
            super.close();
        }
    }

    @Override
    public void close() throws IOException {
        if (!readOnly) {
            serialize();
        }
        data.close();
    }
}
