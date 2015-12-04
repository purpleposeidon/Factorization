package factorization.misc;

import factorization.aabbdebug.AabbDebugger;
import factorization.shared.Core;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IWorldAccess;

public class BlockUpdateDebugger implements IWorldAccess {
    @Override
    public void markBlockForUpdate(int x, int y, int z) {
        markBlockRangeForRenderUpdate(x, y, z, x, y, z);
    }

    @Override
    public void markBlockForRenderUpdate(int x, int y, int z) {
        markBlockRangeForRenderUpdate(x, y, z, x, y, z);
    }

    long last_time = -1;
    int count = 0;
    int the_alot = 64;
    @Override
    public void markBlockRangeForRenderUpdate(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        long now = Minecraft.getMinecraft().theWorld.getTotalWorldTime();
        if (now != last_time) {
            if (count > the_alot) {
                Core.logWarning("(It was %s total, btw)", count);
            }
            count = 0;
            last_time = now;
        }
        count++;
        if (count == the_alot) {
            Core.logWarning("Yikes! %s block render updates in a single tick! I'm not going to draw any more", count);
        } else if (count > the_alot) {
            return;
        }
        double l = -0.25, h = +1.25;
        AabbDebugger.addBox(new AxisAlignedBB(minX + l, minY + l, minZ + l, maxX + h, maxY + h, maxZ + h));
    }

    @Override public void playSound(String p_72704_1_, double p_72704_2_, double p_72704_4_, double p_72704_6_, float p_72704_8_, float p_72704_9_) { }
    @Override public void playSoundToNearExcept(EntityPlayer p_85102_1_, String p_85102_2_, double p_85102_3_, double p_85102_5_, double p_85102_7_, float p_85102_9_, float p_85102_10_) { }
    @Override public void spawnParticle(String p_72708_1_, double p_72708_2_, double p_72708_4_, double p_72708_6_, double p_72708_8_, double p_72708_10_, double p_72708_12_) { }
    @Override public void onEntityCreate(Entity p_72703_1_) { }
    @Override public void onEntityDestroy(Entity p_72709_1_) { }
    @Override public void playRecord(String p_72702_1_, int p_72702_2_, int p_72702_3_, int p_72702_4_) { }
    @Override public void broadcastSound(int p_82746_1_, int p_82746_2_, int p_82746_3_, int p_82746_4_, int p_82746_5_) { }
    @Override public void playAuxSFX(EntityPlayer p_72706_1_, int p_72706_2_, int p_72706_3_, int p_72706_4_, int p_72706_5_, int p_72706_6_) {}
    @Override public void destroyBlockPartially(int p_147587_1_, int p_147587_2_, int p_147587_3_, int p_147587_4_, int p_147587_5_) { }
    @Override public void onStaticEntitiesChanged() { }
}
