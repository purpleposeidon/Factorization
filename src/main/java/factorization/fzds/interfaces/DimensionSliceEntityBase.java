package factorization.fzds.interfaces;

import factorization.shared.EntityFz;
import net.minecraft.world.World;

public abstract class DimensionSliceEntityBase extends EntityFz implements IDimensionSlice, IFzdsShenanigans {
    public DimensionSliceEntityBase(World w) {
        super(w);
    }
}
