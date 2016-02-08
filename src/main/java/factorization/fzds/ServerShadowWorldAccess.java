package factorization.fzds;

import factorization.api.Coord;
import factorization.fzds.interfaces.IDimensionSlice;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IWorldAccess;
import net.minecraft.world.World;

public final class ServerShadowWorldAccess implements IWorldAccess {
    // "IWorldAccess" is poorly named; "IWorldWatcher" would be better.
    static final World world = DeltaChunk.getServerShadowWorld();

    @Override public void spawnParticle(int particleID, boolean ignoreRange, double xCoord, double yCoord, double zCoord, double xOffset, double yOffset, double zOffset, int... p_180442_15_) { }

    @Override public void onEntityAdded(Entity entityIn) { }

    @Override public void onEntityRemoved(Entity entityIn) { }

    @Override public void playSound(String sound, double var2, double var4, double var6, float var8, float var9) {
        // Doesn't seem to be used on the server
    }

    @Override
    public void playRecord(String name, BlockPos pos) {
        final Coord here = new Coord(world, pos);
        for (IDimensionSlice idc : DeltaChunk.getSlicesContainingPoint(here)) {
            idc.getRealWorld().playRecord(idc.shadow2real(pos), name);
        }
    }

    @Override
    public void playAuxSFX(EntityPlayer player, int soundType, BlockPos pos, int soundData) {
        final Coord here = new Coord(world, pos);
        for (IDimensionSlice idc : DeltaChunk.getSlicesContainingPoint(here)) {
            idc.getRealWorld().playAuxSFXAtEntity(player, soundType, idc.shadow2real(pos), soundData);
        }
    }

    @Override
    public void playSoundToNearExcept(EntityPlayer player, String sound, double x, double y, double z, float volume, float pitch) {
        final Coord here = new Coord(world, x, y, z);
        for (IDimensionSlice idc : DeltaChunk.getSlicesContainingPoint(here)) {
            Coord at = here.copy();
            idc.shadow2real(at);
            // TODO: The sound's position is adjusted based on the entity's location.
            // Meh.
            at.w.playSoundToNearExcept(player, sound, volume, pitch);
        }
    }


    @Override
    public void broadcastSound(int soundType, BlockPos pos, int type) {
        final Coord here = new Coord(world, pos);
        for (IDimensionSlice idc : DeltaChunk.getSlicesContainingPoint(here)) {
            Coord at = here.copy();
            idc.shadow2real(at);
            at.w.playBroadcastSound(soundType, pos, type);
        }
    }

    @Override
    public void markBlockRangeForRenderUpdate(int lx, int ly, int lz, int hx, int hy, int hz) {
        markBlocksForUpdate(lx, ly, lz, hx, hy, hz);
    }

    @Override
    public void markBlockForUpdate(BlockPos pos) {
        markBlocksForUpdate(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public void notifyLightSet(BlockPos pos) {
        markBlockForUpdate(pos);
    }

    void markBlocksForUpdate(int lx, int ly, int lz, int hx, int hy, int hz) {
        World realClientWorld = DeltaChunk.getClientRealWorld();
        Coord lower = new Coord(null, lx, ly, lz);
        Coord upper = new Coord(null, hx, hy, hz);
        for (IDimensionSlice idc : DeltaChunk.getSlicesInRange(realClientWorld, lx, ly, lz, hx, hy, hz)) {
            DimensionSliceEntity dse = (DimensionSliceEntity) idc;
            if (dse.getMinCorner().inside(lower, upper) || dse.getMaxCorner().inside(lower, upper)) {
                dse.blocksChanged(lx, ly, lz);
                dse.blocksChanged(hx, hy, hz);
            }
        }
    }

    @Override public void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress) { }
}