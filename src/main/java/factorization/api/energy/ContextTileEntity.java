package factorization.api.energy;

import com.google.common.base.Objects;
import factorization.api.adapter.InterfaceAdapter;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ContextTileEntity implements IWorkerContext {
    public static final InterfaceAdapter<TileEntity, IWorker> adaptTileEntity = InterfaceAdapter.makeAdapter(IWorker.class);

    @Nonnull
    public final TileEntity te;
    @Nullable
    public final EnumFacing side;
    /** Must be null unless side isn't. Can't be parallel to side. */
    @Nullable
    public final EnumFacing edge;

    final IWorker<ContextTileEntity> cast;

    public ContextTileEntity(@Nonnull TileEntity te, @Nullable EnumFacing side, @Nullable EnumFacing edge) {
        this.te = te;
        this.side = side;
        this.edge = edge;
        // TODO: Forge capabilities?
        cast = adaptTileEntity.cast(te);
    }

    public ContextTileEntity(@Nonnull TileEntity te) {
        this(te, null, null);
    }

    @Override
    public IWorker.Accepted give(@Nonnull WorkUnit unit, boolean simulate) {
        return cast.accept(this, unit, simulate);
    }

    @Override
    public boolean isManageable() {
        World world = te.getWorld();
        if (world == null) {
            return FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER;
        }
        return !te.getWorld().isRemote;
    }

    @Override
    public boolean isValid() {
        return cast != null && !te.isInvalid();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContextTileEntity that = (ContextTileEntity) o;
        return Objects.equal(te, that.te) &&
                side == that.side &&
                edge == that.edge;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(te, side, edge);
    }
}
