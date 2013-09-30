package ru.leventov.psgs;

import org.jetbrains.annotations.NotNull;
import ru.leventov.psgs.io.DataOutput;

public final class UniqueEdge<S extends Node, T extends Node, ED>
        extends Edge<S, T, ED> {

    UniqueEdge(UniqueEdgeModel<? super S, ? super T, ED> edgeModel, S source) {
        super(edgeModel, source);
    }

    public UniqueEdgeModel<? super S, ? super T, ED> getModel() {
        // noinspection unchecked
        return (UniqueEdgeModel<? super S, ? super T, ED>) edgeModel;
    }

    public void setTo(int targetId, ED data) {
        source.onChange();
        int oldTargetId = getTargetId();
        if (oldTargetId != 0)
            edgeModel.removeReverse(source, oldTargetId);
        this.setTargetAndData(targetId, data);
        edgeModel.putReverse(source, targetId, data);
    }

    public void setTo(@NotNull T target, ED data) {
        edgeModel.checkSameGraph(target);
        source.onChange();
        int oldTargetId = getTargetId();
        if (oldTargetId != 0)
            edgeModel.removeReverse(source, oldTargetId);
        this.setTargetAndData(target.getId(), data);
        edgeModel.putReverse(source, target, data);
    }

    final void writeData(DataOutput out) {
        getModel().writeData(out, getData());
    }
}
