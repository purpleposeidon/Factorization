package factorization.api.energy;

import com.google.common.base.Objects;
import factorization.api.Coord;
import factorization.api.adapter.InterfaceAdapter;
import net.minecraft.block.Block;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ContextBlock implements IWorkerContext {
    public static final InterfaceAdapter<Block, IWorker> adaptBlock = InterfaceAdapter.makeAdapter(IWorker.class);

    public final Coord at;
    @Nullable
    public final EnumFacing side;
    /** Must be null unless side isn't. Can't be parallel to side. */
    @Nullable
    public final EnumFacing edge;

    final IWorker<ContextBlock> cast;

    public ContextBlock(Coord at, @Nullable EnumFacing side, @Nullable EnumFacing edge) {
        this.at = at;
        this.side = side;
        this.edge = edge;
        // TODO: Forge capabilities?
        cast = adaptBlock.cast(at.getBlock());
    }

    @Override
    public IWorker.Accepted give(@Nonnull WorkUnit unit, boolean simulate) {
        return cast.accept(this, unit, simulate);
    }

    @Override
    public boolean isManageable() {
        return false;
    }

    @Override
    public boolean isValid() {
        return cast != null && at.blockExists();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContextBlock that = (ContextBlock) o;
        return Objects.equal(at, that.at) &&
                side == that.side &&
                edge == that.edge;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(at, side, edge);
    }

}
