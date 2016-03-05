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

public class ContextTileEntity implements IContext {
    public static final InterfaceAdapter<TileEntity, IWorker> adaptTileEntity = InterfaceAdapter.makeAdapter(IWorker.class);

    @Nonnull
    public final TileEntity te;
    @Nullable
    public final EnumFacing side, edge;

    public ContextTileEntity(@Nonnull TileEntity te, @Nullable EnumFacing side, @Nullable EnumFacing edge) {
        this.te = te;
        this.side = side;
        this.edge = edge;
    }

    public ContextTileEntity(@Nonnull TileEntity te) {
        this(te, null, null);
    }

    @Override
    public IWorker.Accepted give(@Nonnull WorkUnit unit, boolean simulate) {
        IWorker cast = adaptTileEntity.cast(te);
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
        return !te.isInvalid();
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
