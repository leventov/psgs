package ru.leventov.psgs;

import gnu.trove.TIterable;
import gnu.trove.TIterator;
import gnu.trove.function.*;
import gnu.trove.map.IntKeyMapIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.NoSuchElementException;

public class Edges<S extends Node, T extends Node, ED> implements TIterable<Edge<S, T, ED>> {

    private class EmptyIterator implements TIterator<Edge<S, T, ED>> {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public boolean tryAdvance() {
            return false;
        }

        @Override
        public Edge<S, T, ED> value() {
            throw new NoSuchElementException();
        }

        @Override
        public Edge<S, T, ED> next() {
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new NoSuchElementException();
        }
    }

    @NotNull
    private final EdgeModel<? super S, ? super T, ED> edgeModel;
    @NotNull
    private final S source;
    @Nullable
    private NodeIdEdgeMap<ED> map;
    @Nullable
    private MapEdge<S, T, ED> mapEdge;

    // source already set
    Edges(@NotNull EdgeModel<? super S, ? super T, ED> edgeModel, @NotNull S source,
          @Nullable NodeIdEdgeMap<ED> map) {
        this.edgeModel = edgeModel;
        this.source = source;
        this.map = map;
    }

    public EdgeModel<? super S, ? super T, ED> getModel() {
        return edgeModel;
    }

    @Nullable
    NodeIdEdgeMap<ED> getMap() {
        return map;
    }

    private static class IteratorEdge<S extends Node, T extends Node, ED>
            extends Edge<S, T, ED> implements TIterator<Edge<S, T, ED>> {
        private final IntKeyMapIterator<ED> impl;

        public IteratorEdge(EdgeModel<? super S, ? super T, ED> edgeModel, S source,
                            IntKeyMapIterator<ED> impl) {
            super(edgeModel, source);
            this.impl = impl;
        }

        @Override
        public ED setData(ED newData) {
            impl.setValue(newData);
            return super.setData(newData);
        }

        @Override
        public void justSetData(ED newData) {
            impl.setValue(newData);
            super.justSetData(newData);
        }

        @Override
        public boolean hasNext() {
            return impl.hasNext();
        }

        public boolean tryAdvance() {
            return impl.tryAdvance();
        }

        @Override
        public Edge<S, T, ED> value() {
            setTargetAndData(impl.intKey(), impl.value());
            return this;
        }

        @Override
        public Edge<S, T, ED> next() {
            if (tryAdvance()) {
                return value();
            } else {
                throw new NoSuchElementException();
            }
        }

        public void remove() {
            source.onChange();
            impl.remove();
            edgeModel.removeReverse(source, impl.intKey());
        }
    }

    @NotNull
    @Override
    public TIterator<Edge<S, T, ED>> iterator() {
        if (map != null) {
            return new IteratorEdge<>(edgeModel, source, map.iterator());
        } else {
            // noinspection unchecked
            return new EmptyIterator();
        }
    }

    private static abstract class ForEachEdge<S extends Node, T extends Node, ED>
            extends Edge<S, T, ED> implements IntObjConsumer<ED> {

        public ForEachEdge(EdgeModel<? super S, ? super T, ED> edgeModel, S source) {
            super(edgeModel, source);
        }

        @Override
        public ED setData(ED newData) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void justSetData(ED newData) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void forEach(final Consumer<? super Edge<S, T, ED>> action) {
        if (map != null) {
            map.forEach(new ForEachEdge<S, T, ED>(edgeModel, source) {
                @Override
                public void accept(int targetId, ED data) {
                    this.setTargetAndData(targetId, data);
                    action.accept(this);
                }
            });
        }
    }

    private static abstract class TestWhileEdge<S extends Node, T extends Node, ED>
            extends Edge<S, T, ED> implements IntObjPredicate<ED> {

        public TestWhileEdge(EdgeModel<? super S, ? super T, ED> edgeModel, S source) {
            super(edgeModel, source);
        }

        @Override
        public ED setData(ED newData) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void justSetData(ED newData) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public boolean testWhile(final Predicate<? super Edge<S, T, ED>> predicate) {
        return map == null || map.testWhile(new TestWhileEdge<S, T, ED>(edgeModel, source) {
            @Override
            public boolean test(int targetId, ED data) {
                this.setTargetAndData(targetId, data);
                return predicate.test(this);
            }
        });
    }

    public void forEachTarget(IntConsumer action) {
        if (map != null) {
            map.forEachNodeId(action);
        }
    }

    public boolean testTargetsWhile(IntPredicate predicate) {
        return map == null || map.testNodeIdsWhile(predicate);
    }

    public int count() {
        return map != null ? map.size() : 0;
    }

    public boolean add(int targetId, ED data) {
        if (map == null) {
            map = edgeModel.newMap(1);
        }
        if (map.justAddEdgeTo(targetId, data)) {
            source.onChange();
            edgeModel.putReverse(source, targetId, data);
            return true;
        } else {
            return false;
        }
    }

    public boolean add(T target, ED data) {
        edgeModel.checkSameGraph(target);
        if (map == null) {
            map = edgeModel.newMap(1);
        }
        if (map.justAddEdgeTo(target.getId(), data)) {
            source.onChange();
            edgeModel.putReverse(source, target, data);
            return true;
        } else {
            return false;
        }
    }

    void addReverse(int targetId, ED data) {
        if (map == null) {
            map = edgeModel.newMap(1);
        }
        if (map.justAddEdgeTo(targetId, data)) {
            source.onChange();
        }
    }

    public boolean addAll(Map<Integer, ED> targetsWithData) {
        if (targetsWithData.isEmpty())
            return false;
        if (map == null) {
            map = edgeModel.newMap(targetsWithData.size());
        }
        boolean changed = false;
        for (Map.Entry<Integer, ED> target : targetsWithData.entrySet()) {
            ED data = target.getValue();
            int targetId = target.getKey();
            if (map.justAddEdgeTo(targetId, data)) {
                edgeModel.putReverse(source, targetId, data);
                changed = true;
            }
        }
        if (changed) source.onChange();
        return changed;
    }

    public boolean addAllTargetObjects(Map<T, ED> targetsWithData) {
        if (targetsWithData.isEmpty())
            return false;
        if (map == null) {
            map = edgeModel.newMap(targetsWithData.size());
        }
        boolean changed = false;
        for (Map.Entry<T, ED> targetWithData : targetsWithData.entrySet()) {
            ED data = targetWithData.getValue();
            T target = targetWithData.getKey();
            edgeModel.checkSameGraph(target);
            if (map.justAddEdgeTo(target.getId(), data)) {
                edgeModel.putReverse(source, target, data);
                changed = true;
            }
        }
        if (changed) source.onChange();
        return changed;
    }

    private static class MapEdge<S extends Node, T extends Node, ED> extends Edge<S, T, ED> {
        private final NodeIdEdgeMap<ED> map;

        public MapEdge(EdgeModel<? super S, ? super T, ED> edgeModel, S source, NodeIdEdgeMap<ED> map) {
            super(edgeModel, source);
            this.map = map;
        }

        @Override
        public ED setData(ED newData) {
            map.justAddEdgeTo(getTargetId(), newData);
            return super.setData(newData);
        }

        @Override
        public void justSetData(ED newData) {
            map.justAddEdgeTo(getTargetId(), newData);
            super.justSetData(newData);
        }
    }

    @Nullable
    public Edge<S, T, ED> to(int targetId) {
        if (map != null) {
            ED data = map.getEdgeData(targetId);
            if (data != null) {
                MapEdge<S, T, ED> mapEdge = this.mapEdge;
                if (mapEdge == null) {
                    this.mapEdge = mapEdge = new MapEdge<>(edgeModel, source, map);
                }
                mapEdge.setTargetAndData(targetId, data);
                return mapEdge;
            }
        }
        return null;
    }

    public boolean isPresentTo(int targetId) {
        if (map != null) {
            if (map.getEdgeData(targetId) != null) {
                return true;
            }
        }
        return false;
    }

    public boolean justRemoveTo(int targetId) {
        if (map != null) {
            if (map.justRemoveEdgeTo(targetId)) {
                source.onChange();
                edgeModel.removeReverse(source, targetId);
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public boolean justRemoveTo(T target) {
        edgeModel.checkSameGraph(target);
        if (map != null) {
            if (map.justRemoveEdgeTo(target.getId())) {
                source.onChange();
                edgeModel.removeReverse(source, target);
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public void removeAll() {
        if (map != null) {
            if (!map.isEmpty()) {
                source.onChange();
                map.forEachNodeId(new IntConsumer() {
                    @Override
                    public void accept(int targetId) {
                        edgeModel.removeReverse(source, targetId);
                    }
                });
            }
            map = null;
            mapEdge = null;
        }
    }

    void removeReverse(int targetId) {
        // NPE will signify graph inconsistency
        if (map.justRemoveEdgeTo(targetId)) {
            source.onChange();
        }
    }
}
