package factorization.flat;

import factorization.api.Coord;
import factorization.flat.api.Flat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IWorldAccess;
import net.minecraft.world.World;

public class FlatWorldEventListener implements IWorldAccess {
    final World world;

    public FlatWorldEventListener(World world) {
        this.world = world;
    }

    @Override
    public void markBlockForUpdate(BlockPos pos) {
        Coord at = new Coord(world, pos);
        if (!at.blockExists()) return;
        Flat.onBlockChanged(at);
    }

    @Override public void notifyLightSet(BlockPos pos) { }
    @Override public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) { }
    @Override public void playSound(String soundName, double x, double y, double z, float volume, float pitch) { }
    @Override public void playSoundToNearExcept(EntityPlayer except, String soundName, double x, double y, double z, float volume, float pitch) { }
    @Override public void spawnParticle(int particleID, boolean ignoreRange, double xCoord, double yCoord, double zCoord, double xOffset, double yOffset, double zOffset, int... parameters) { }
    @Override public void onEntityAdded(Entity entityIn) { }
    @Override public void onEntityRemoved(Entity entityIn) { }
    @Override public void playRecord(String recordName, BlockPos blockPosIn) { }
    @Override public void broadcastSound(int soundID, BlockPos pos, int data) { }
    @Override public void playAuxSFX(EntityPlayer player, int sfxType, BlockPos blockPosIn, int data) { }
    @Override public void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress) { }
}
