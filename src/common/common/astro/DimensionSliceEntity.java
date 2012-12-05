package factorization.common.astro;

import net.minecraft.src.Entity;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.World;
import factorization.api.Coord;

public class DimensionSliceEntity extends Entity {
    Object renderInfo = null;
    int cell;
    public Coord hammerCell;
    
    public DimensionSliceEntity(World world) {
        super(world);
        ignoreFrustumCheck = true; //kinda lame; should give ourselves big bounding box
    }
    
    public DimensionSliceEntity(World world, int cell) {
        this(world);
        this.cell = cell;
    }
    
    @Override
    protected void entityInit() {
        // TODO Auto-generated method stub

    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        cell = tag.getInteger("cell");
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        tag.setInteger("cell", cell);
    }
    
    @Override
    public void onEntityUpdate() {
        super.onEntityUpdate();
    }

}
