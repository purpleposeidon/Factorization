package factorization.utiligoo;

import java.util.WeakHashMap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldSavedData;

public class GooData extends WorldSavedData {
    public GooData(String dataName) {
        super(dataName);
    }

    int dimensionId;
    int[] coords = new int[0];
    short lost = 0;
    
    int change_counts = 0;
    WeakHashMap<Entity, Integer> player_updates = new WeakHashMap<Entity, Integer>();
    
    @Override
    public void readFromNBT(NBTTagCompound tag) {
        if (!tag.hasKey("dimensionId")) {
            return;
        }
        dimensionId = tag.getInteger("dimensionId");
        coords = tag.getIntArray("coordData");
        lost = tag.getShort("lost");
    }
    
    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setString("mapname", mapName);
        tag.setInteger("dimensionId", dimensionId);
        tag.setIntArray("coordData", coords);
        tag.setShort("lost", lost);
    }
    
    @Override
    public void setDirty(boolean dirty) {
        super.setDirty(dirty);
        if (dirty) {
            change_counts++;
        }
    }
    
    boolean isPlayerOutOfDate(Entity player) {
        if (!(player instanceof EntityPlayer)) return false;
        Integer update_count = player_updates.get(player);
        if (update_count == null || update_count != change_counts) {
            player_updates.put(player, change_counts);
            return true;
        }
        return false;
    }
}