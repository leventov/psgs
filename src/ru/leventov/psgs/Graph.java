package ru.leventov.psgs;

import gnu.trove.function.Consumer;
import gnu.trove.function.IntFunction;
import gnu.trove.map.hash.ObjLongDHashMap;
import ru.leventov.psgs.index.BTreeIndex;
import ru.leventov.psgs.io.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static ru.leventov.psgs.io.Files.openForWriting;
import static ru.leventov.psgs.util.Bits.roundUp4;
import static ru.leventov.psgs.util.Bits.unsignedByte;
import static ru.leventov.psgs.util.Bits.unsignedInt;

public abstract class Graph {
    private final ClassIds<AbstractEdgeModel> edgeModelClassIds = new ClassIds<>();
    private final ClassIds<Node> nodeClassIds = new ClassIds<>();

    long nodeCount, maxNodeIdBound, minNodeIdBound;

    public abstract long nodeCount();

    public abstract Node getNode(int id);

    public abstract void addNode(Node node);

    public abstract Node getOrCreateNode(int id, IntFunction<Node> producer);

    public abstract boolean isNodeIdUsed(int nodeId);

    public abstract void removeNode(Node node);

    public abstract Node removeNode(int nodeId);

    public abstract void forEachNode(Consumer<? super Node> action);

    /**
     * Call from edge models
     */
    abstract Node getNodeForChange(int id);

    /**
     * Call only from inside Node class!
     */
    abstract void nodeChanged(Node node);

    final byte getEdgeModelId(Class<? extends AbstractEdgeModel> edgeModelClass) {
        return edgeModelClassIds.getId(edgeModelClass);
    }

    final Class<? extends AbstractEdgeModel> getEdgeModelClass(byte id) {
        return edgeModelClassIds.getClass(id);
    }

    final byte getNodeClassId(Class<? extends Node> nodeClass) {
        return nodeClassIds.getId(nodeClass);
    }

    final Class<? extends Node> getNodeClass(byte id) {
        return nodeClassIds.getClass(id);
    }


    void init(Metadata metadata) throws DeserializationException {
        nodeCount = metadata.nodeCount;
        maxNodeIdBound = metadata.maxNodeIdBound;
        minNodeIdBound = metadata.minNodeIdBound;
        try {
            nodeClassIds.loadMap(metadata.nodeClasses);
            edgeModelClassIds.loadMap(metadata.edgeClasses);
        } catch (ClassNotFoundException e) {
            throw new DeserializationException(e);
        }
    }

    static final int CURRENT_PROTOCOL_VERSION = 1;

    static String defaultFormat() {
        return "full";
    }

    static class Metadata {
        final int protocolVersion = CURRENT_PROTOCOL_VERSION;
        String format;
        ByteOrder byteOrder;
        long nodeCount;
        long maxNodeIdBound;
        long minNodeIdBound;
        Map<String, Byte> nodeClasses;
        Map<String, Byte> edgeClasses;
        Stats stats;
    }

    static void write6BytesDataOffset(ByteBuffer out, long offset) {
        out.putChar((char) (offset >> 32));
        out.putInt((int) offset);
    }

    static long read6BytesDataOffset(ByteBuffer in) {
        long high = in.getChar();
        long low = unsignedInt(in.getInt());
        return (high << 32) | low;
    }

    static void writeWithSize(ByteArrayListDataOutput out, DataWritable writable) {
        out.skipBytes(4);
        int startPos = out.position();
        writable.writeData(out);
        int roundedEndPos = roundUp4(out.position());
        out.position(startPos - 4);
        out.writeInt(roundedEndPos - startPos);
        out.position(roundedEndPos);
    }

    static Path dataFile(Path dir) {
        return dir.resolve("data");
    }

    static Path metadataFile(Path dir) {
        return dir.resolve("metadata.json");
    }

    static Path nodeIndexDir(Path dir) {
        return dir.resolve("node-index");
    }

    abstract class SerializationState implements Closeable {
        static final int OUTPUT_BUFFER_INITIAL_CAPACITY = 32 * (1 << 20);

        final StatsAggregator statsAggregator = new StatsAggregator();
        final Metadata metadata = new Metadata();
        final FileChannel dataChannel;
        final Path dir;
        final ByteArrayListDataOutput dataOutput;
        private int startPos;

        BTreeIndex nodeIndex;
        private int adjCount;

        SerializationState(Path dir, ByteOrder byteOrder, String format, BTreeIndex nodeIndex) throws IOException {
            this.dir = dir;
            metadata.byteOrder = byteOrder;
            metadata.format = format;
            Files.createDirectories(dir);
            dataChannel = openForWriting(dataFile(dir));
            dataOutput = new ByteArrayListDataOutput(OUTPUT_BUFFER_INITIAL_CAPACITY, byteOrder);
            this.nodeIndex = nodeIndex;
        }

        void memorizePos() {
            dataOutput.skipBytes(4);
            startPos = dataOutput.position();
        }

        void writeSize() {
            ByteArrayListDataOutput out = dataOutput;
            int roundedEndPos = roundUp4(out.position());
            out.position(startPos - 4);
            out.writeInt(roundedEndPos - startPos);
            out.position(roundedEndPos);
        }

        void flush() throws IOException {
            if (dataOutput.position() > 0) {
                ByteBuffer buffer = dataOutput.getBuffer();
                buffer.flip();
                dataChannel.write(buffer);
                dataOutput.clear();
            }
        }

        void checkForFlush() throws IOException {
            if (dataOutput.position() >= OUTPUT_BUFFER_INITIAL_CAPACITY / 2) {
                ByteBuffer buffer = dataOutput.getBuffer();
                buffer.flip();
                dataChannel.write(buffer);
                dataOutput.clear();
            }
        }

        void writeNode(Node node) throws IOException {

            statsAggregator.aggregateNode(node.getClassId());

            long dataStartPos = dataChannel.position() + dataOutput.position();

            writeWithSize(dataOutput, node);
            checkForFlush();

            adjCount = 0;
            node.forEachAdjacent(new Node.EdgesConsumer() {
                @Override
                public void accept(byte modelId, Edges<?, ?, ?> edges) throws IOException {
                    int edgeCount = edges.count();
                    if (edgeCount > 0) {
                        statsAggregator.aggregateEdges(modelId, edgeCount);
                        adjCount++;

                        int edgeModelId = unsignedByte(modelId);
                        dataOutput.writeInt(edgeModelId);
                        memorizePos();
                        dataOutput.writeInt(edgeCount);
                        edges.getMap().writeData(dataOutput);
                        writeSize();
                        checkForFlush();
                    }
                }
            }, new Node.UniqueEdgeConsumer() {
                @Override
                public void accept(byte modelId, UniqueEdge<?, ?, ?> edge) throws IOException {
                    statsAggregator.aggregateUniqueEdge(modelId);
                    adjCount++;

                    int edgeModelId = unsignedByte(modelId);
                    dataOutput.writeInt(edgeModelId);
                    memorizePos();
                    dataOutput.writeInt(edge.getTargetId());
                    edge.writeData(dataOutput);
                    writeSize();
                    checkForFlush();
                }
            });

            ByteBuffer nodeEntry = nodeIndex.insert(node.getId());
            nodeEntry.put(node.getClassId());
            nodeEntry.put((byte) adjCount);
            write6BytesDataOffset(nodeEntry, dataStartPos);
        }

        void writeMetadata() throws IOException {
            metadata.nodeCount = nodeCount();
            metadata.maxNodeIdBound = maxNodeIdBound;
            metadata.minNodeIdBound = minNodeIdBound;
            metadata.nodeClasses = nodeClassIds.asMap();
            metadata.edgeClasses = edgeModelClassIds.asMap();
            metadata.stats = statsAggregator.countStats(nodeClassIds, edgeModelClassIds);
            Json.writeJson(metadataFile(dir), metadata);
        }

        @Override
        public void close() throws IOException {
            flush();
            dataChannel.close();
        }
    }

    static class Stats {
        Map<String, Long> nodeCounts;
        Map<String, EdgeStats> edgeStats;
        Map<String, Long> uniqueEdgeCounts;
    }

    static class EdgeStats {
        long nodes;
        long totalEdges;
        float averageEdgesPerNode;
    }

    static class StatsAggregator {
        long[] nodeCounts = new long[256];
        long[] edgeCounts = new long[256];
        long[] edgeTotals = new long[256];

        void aggregateNode(byte nodeClassId) {
            nodeCounts[unsignedByte(nodeClassId)]++;
        }

        void aggregateUniqueEdge(byte id) {
            edgeCounts[unsignedByte(id)]++;
        }

        void aggregateEdges(byte id, int edgeCount) {
            int intId = unsignedByte(id);
            edgeCounts[intId]++;
            edgeTotals[intId] += edgeCount;
        }

        Stats countStats(ClassIds<Node> nodeClassIds, ClassIds<AbstractEdgeModel> edgeModelClassIds) {
            Stats stats = new Stats();
            stats.nodeCounts = new ObjLongDHashMap<>();
            for (int i = 0; i < 256; i++) {
                if (nodeCounts[i] != 0) {
                    stats.nodeCounts.put(nodeClassIds.getClass(i).getName(), nodeCounts[i]);
                }
            }
            stats.edgeStats = new HashMap<>();
            stats.uniqueEdgeCounts = new ObjLongDHashMap<>();
            for (int i = 0; i < 256; i++) {
                if (edgeCounts[i] != 0) {
                    Class<?> cl = edgeModelClassIds.getClass(i);
                    if (EdgeModel.class.isAssignableFrom(cl)) {
                        EdgeStats edgeStats = new EdgeStats();
                        edgeStats.nodes = edgeCounts[i];
                        edgeStats.totalEdges = edgeTotals[i];
                        edgeStats.averageEdgesPerNode = (float) ((double) edgeTotals[i] / edgeCounts[i]);
                        stats.edgeStats.put(cl.getName(), edgeStats);
                    } else if (UniqueEdgeModel.class.isAssignableFrom(cl)) {
                        stats.uniqueEdgeCounts.put(cl.getName(), edgeCounts[i]);
                    } else {
                        throw new RuntimeException();
                    }
                }
            }
            return stats;
        }
    }
}
