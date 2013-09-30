package ru.leventov.psgs;

import org.jetbrains.annotations.Nullable;
import ru.leventov.psgs.io.DataWriter;

public abstract class UniqueEdgeModel<S extends Node, T extends Node, ED>
        extends AbstractEdgeModel<S, T, ED> implements DataWriter<ED> {

    UniqueEdgeModel(Graph graph) {
        super(graph);
    }

    @Nullable
    public final UniqueEdge<S, T, ED> from(S source) {
        checkSameGraph(source);
        return source.getUniqueEdge(this, false);
    }

    public final boolean isUnique() {
        return true;
    }

    public final boolean isPresentFrom(S source) {
        checkSameGraph(source);
        return source.hasUniqueClass(getId());
    }

    public final boolean removeFrom(S source) {
        checkSameGraph(source);
        int targetId = source.removeUniqueEdge(getId());
        if (targetId != 0) {
            removeReverse(source, targetId);
            return true;
        } else {
            return false;
        }
    }

    public final void set(S source, int targetId, ED data) {
        checkSameGraph(source);
        source.getUniqueEdge(this, true).setTo(targetId, data);
    }

    public final void set(S source, T target, ED data) {
        checkSameGraph(source);
        checkSameGraph(target);
        source.getUniqueEdge(this, true).setTo(target, data);
    }
}
