package ru.leventov.psgs;

import gnu.trove.function.Consumer;
import gnu.trove.function.IntFunction;
import gnu.trove.map.hash.IntObjDHashMap;
import gnu.trove.map.hash.TIntObjHashMap;
import ru.leventov.psgs.index.NewBTreeIndex;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Path;

import static ru.leventov.psgs.util.Bits.unsignedInt;

public class NewGraph extends Graph {
    private TIntObjHashMap<Node> nodes = new IntObjDHashMap<>();

    public NewGraph() {
        nodeCount = 0;
        maxNodeIdBound = 0;
        minNodeIdBound = 1;
    }

    public static NewGraph create() {
        return new NewGraph();
    }

    @Override
    public long nodeCount() {
        return nodeCount;
    }

    @Override
    public Node getNode(int id) {
        if (id != 0) {
            return nodes.get(id);
        } else {
            throw new IllegalArgumentException("Node id couldn't be 0.");
        }
    }

    Node getNodeForChange(int id) {
        Node node = getNode(id);
        node.changed();
        return node;
    }

    @Override
    void nodeChanged(Node node) {
        // intentionally left blank
    }

    @Override
    public void addNode(Node node) {
        Graph nodeGraph = node.getGraph();
        if (nodeGraph == null && node.getId() == 0) {
            int id = (int) ++maxNodeIdBound;
            if (id == 0) {
                do {
                    id++;
                } while (nodes.containsKey(id));
            }
            nodes.put(id, node);
            node.addToGraph(id, this, getNodeClassId(node.getClass()));
            nodeCount++;
        } else {
            throw new IllegalArgumentException("Node couldn't be contained in 2 graphs simultaneously");
        }
    }

    @Override
    public Node getOrCreateNode(int id, final IntFunction<Node> producer) {
        if (id != 0) {
            return nodes.computeIfAbsent(id, new IntFunction<Node>() {
                @Override
                public Node apply(int id) {
                    Node node = producer.apply(id);
                    if (node.getGraph() == null && node.getId() == 0) {
                        node.addToGraph(id, NewGraph.this, getNodeClassId(node.getClass()));
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
            });
        } else {
            throw new IllegalArgumentException("Node id couldn't be 0.");
        }
    }

    @Override
    public void removeNode(Node node) {
        if (node.getGraph() != this)
            throw new IllegalArgumentException();
        if (nodes.justRemove(node.getId())) {
            node.removeFromGraph();
            nodeCount--;
        } else {
            throw new IllegalStateException("The graph must contain the node");
        }
    }

    @Override
    public Node removeNode(int nodeId) {
        Node node = nodes.remove(nodeId);
        if (node != null) {
            node.removeFromGraph();
            nodeCount--;
        }
        return node;
    }

    @Override
    public void forEachNode(Consumer<? super Node> action) {
        nodes.values().forEach(action);
    }

    public void write(Path dir, final ByteOrder byteOrder) throws IOException {
        try (NewGraphSerializationState serialization = new NewGraphSerializationState(dir, byteOrder)) {
            for (Node node : nodes.values()) {
                serialization.writeNode(node);
            }
        }
    }

    class NewGraphSerializationState extends SerializationState {

        NewGraphSerializationState(Path dir, ByteOrder byteOrder) throws IOException {
            super(dir, byteOrder, defaultFormat(), new NewBTreeIndex(byteOrder, 8));
        }

        @Override
        public void close() throws IOException {
            ((NewBTreeIndex) nodeIndex).write(nodeIndexDir(dir));
            writeMetadata();
            super.close();
        }
    }
}
