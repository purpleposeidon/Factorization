package factorization.misc;

import factorization.aabbdebug.AabbDebugger;
import factorization.shared.Core;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IWorldAccess;

public class BlockUpdateDebugger implements IWorldAccess {
    @Override
    public void markBlockForUpdate(BlockPos pos) {
        markBlockRangeForRenderUpdate(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public void notifyLightSet(BlockPos pos) {
        markBlockForUpdate(pos);
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
    @Override public void spawnParticle(int particleID, boolean ignoreRange, double xCoord, double yCoord, double zCoord, double xOffset, double yOffset, double zOffset, int... p_180442_15_) { }
    @Override public void onEntityAdded(Entity entityIn) { }
    @Override public void onEntityRemoved(Entity entityIn) { }
    @Override public void playRecord(String recordName, BlockPos blockPosIn) { }
    @Override public void broadcastSound(int p_180440_1_, BlockPos p_180440_2_, int p_180440_3_) { }
    @Override public void playAuxSFX(EntityPlayer player, int sfxType, BlockPos blockPosIn, int p_180439_4_) { }
    @Override public void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress) { }
}
