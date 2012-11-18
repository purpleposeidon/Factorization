package factorization.common.astro;

import java.util.Iterator;

import factorization.common.Core;

import net.minecraft.src.BlockEventData;
import net.minecraft.src.Entity;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.Packet54PlayNoteBlock;
import net.minecraft.src.PlayerManager;
import net.minecraft.src.SpawnerAnimals;
import net.minecraft.src.World;
import net.minecraft.src.WorldServer;

public class DimensionSliceEntity extends Entity {
    Object renderInfo = null;
    int cell;
    
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
