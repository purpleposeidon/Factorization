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

public class WorldEntity extends Entity {
    WEWorld wew;
    Object worldRenderer = null, oldWorldRenderer = null;
    public WorldEntity(World world) {
        super(world);
        wew = new WEWorld(this);
        ignoreFrustumCheck = true; //kinda lame.
    }

    @Override
    protected void entityInit() {
        // TODO Auto-generated method stub

    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound var1) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound var1) {
        // TODO Auto-generated method stub

    }
    
    @Override
    public void onEntityUpdate() {
        super.onEntityUpdate();
        wew.tick();
    }

}
