package factorization.fzds;

import factorization.api.Coord;
import factorization.fzds.interfaces.IDimensionSlice;
import factorization.util.NumUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DestroyBlockProgress;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.Vec3;
import net.minecraft.world.IWorldAccess;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;

class ShadowRenderGlobal implements IWorldAccess {
    /***
     * Inspired, obviously, by RenderGlobal.
     * The World has a list of IWorldAccess, which it passes various events to.  
     */
    private World realWorld;
    public ShadowRenderGlobal(World realWorld) {
        this.realWorld = realWorld;
    }
    //The coord arguments are always in shadowspace.


    @Override
    public void markBlockForUpdate(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        markBlockRangeForRenderUpdate(x, y, z, x, y, z);
    }

    @Override
    public void notifyLightSet(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        markBlockRangeForRenderUpdate(x, y, z, x, y, z);
    }

    @Override
    public void markBlockRangeForRenderUpdate(int var1, int var2, int var3,
            int var4, int var5, int var6) {
        markBlocksForUpdate(var1 - 1, var2 - 1, var3 - 1, var4 + 1, var5 + 1, var6 + 1);
    }
    
    void markBlocksForUpdate(int lx, int ly, int lz, int hx, int hy, int hz) {
        World realClientWorld = DeltaChunk.getClientRealWorld();
        for (IDimensionSlice idc : DeltaChunk.getSlicesInRange(realClientWorld, lx, ly, lz, hx, hy, hz)) {
            DimensionSliceEntity dse = (DimensionSliceEntity) idc;
            Coord near = dse.getMinCorner(), far = dse.getMaxCorner();
            if (NumUtil.intersect(near.x, far.x, lx, hx)
                    && NumUtil.intersect(near.y, far.y, ly, hy)
                    && NumUtil.intersect(near.z, far.z, lz, hz)) {
                RenderDimensionSliceEntity.markBlocksForUpdate(dse, lx, ly, lz, hx, hy, hz);
                dse.blocksChanged(lx, ly, lz);
                dse.blocksChanged(hx, hy, hz);
            }
        }
    }

    @Override
    public void playSound(String sound, double x, double y, double z, float volume, float pitch) {
        if (sound == null) return; // Odd. Has happened a few times tho.
        Vec3 realCoords = DeltaChunk.shadow2nearestReal(Minecraft.getMinecraft().thePlayer, new Vec3(x, y, z));
        if (realCoords == null) {
            return;
        }
        realWorld.playSound(realCoords.xCoord, realCoords.yCoord, realCoords.zCoord, sound, volume, pitch, false);
    }
    
    @Override
    public void playSoundToNearExcept(EntityPlayer entityplayer, String sound, double x, double y, double z, float volume, float pitch) {
        if (entityplayer != Minecraft.getMinecraft().thePlayer) {
            playSound(sound, x, y, z, volume, pitch);
        }
    }


    @Override
    public void playRecord(String recordName, BlockPos pos) {
        BlockPos realCoords = DeltaChunk.shadow2nearestReal(Minecraft.getMinecraft().thePlayer, pos);
        if (realCoords == null) {
            return;
        }
        HammerClientProxy.getRealRenderGlobal().playRecord(recordName, realCoords);
    }

    @Override
    public void broadcastSound(int soundType, BlockPos pos, int type) {
        final Coord here = new Coord(DeltaChunk.getClientShadowWorld(), pos);
        for (IDimensionSlice idc : DeltaChunk.getSlicesContainingPoint(here)) {
            Coord at = here.copy();
            idc.shadow2real(at);
            at.w.playBroadcastSound(soundType, pos, type);
        }
    }

    @Override
    public void playAuxSFX(EntityPlayer player, int soundType, BlockPos pos, int soundData) {
        final Coord here = new Coord(DeltaChunk.getClientShadowWorld(), pos);
        for (IDimensionSlice idc : DeltaChunk.getSlicesContainingPoint(here)) {
            Coord at = idc.shadow2real(here);
            at.w.playAuxSFXAtEntity(player, soundType, at.toBlockPos(), soundData);
        }
    }

    EnumParticleTypes[] particles = EnumParticleTypes.values();

    @Override
    public void spawnParticle(int particleID, boolean ignoreRange, double x, double y, double z, double vx, double vy, double vz, int... params) {
        Vec3 realCoords = DeltaChunk.shadow2nearestReal(Minecraft.getMinecraft().thePlayer, new Vec3(x, y, z));
        if (realCoords == null) {
            return;
        }
        // NORELEASE: Block break particles are still in need of fixing.
        x = realCoords.xCoord;
        y = realCoords.yCoord;
        z = realCoords.zCoord;
        if (!Hammer.proxy.isInShadowWorld()) {
            // Shouldn't happen
            realWorld.spawnParticle(particles[particleID], ignoreRange, x, y, z, vx, vy, vz, params);
        } else {
            Hammer.proxy.restoreRealWorld();
            realWorld.spawnParticle(particles[particleID], ignoreRange, x, y, z, vx, vy, vz, params);
            Hammer.proxy.setShadowWorld();
        }
    }

    @Override
    public void onEntityAdded(Entity entity) {
        HammerClientProxy.getRealRenderGlobal().onEntityAdded(entity);
    }

    @Override
    public void onEntityRemoved(Entity entity) {
        HammerClientProxy.getRealRenderGlobal().onEntityRemoved(entity);
    }

    HashMap<Integer, DestroyBlockProgress> damagedBlocks = new HashMap<Integer, DestroyBlockProgress>();

    @Override
    public void sendBlockBreakProgress(int breakerId, BlockPos pos, int damageProgress) {
        if (!(damageProgress >= 0 && damageProgress < 10)) {
            damagedBlocks.remove(Integer.valueOf(breakerId));
            return;
        }
        DestroyBlockProgress destroyblockprogress = (DestroyBlockProgress) damagedBlocks.get(Integer.valueOf(breakerId));

        if (destroyblockprogress == null || !destroyblockprogress.getPosition().equals(pos)) {
            destroyblockprogress = new DestroyBlockProgress(breakerId, pos);
            damagedBlocks.put(breakerId, destroyblockprogress);
        }

        destroyblockprogress.setPartialBlockDamage(damageProgress);
        destroyblockprogress.setCloudUpdateTick(tickTime);
    }
    
    int tickTime = 0;
    public void removeStaleDamage() {
        if (tickTime++ % 20 != 0) return;
        
        Iterator<DestroyBlockProgress> iterator = damagedBlocks.values().iterator();
        while (iterator.hasNext()) {
            DestroyBlockProgress destroyblockprogress = iterator.next();
            int createTime = destroyblockprogress.getCreationCloudUpdateTick();

            if (tickTime - createTime > 400) {
                iterator.remove();
            }
        }
    }

}