package factorization.fzds;

import java.util.Iterator;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.IWorldAccess;
import net.minecraft.world.World;
import factorization.api.Coord;
import factorization.fzds.interfaces.IDeltaChunk;

public final class ShadowWorldAccess implements IWorldAccess {
    // "IWorldAccess" is poorly named; "IWorldWatcher" would be better.
    Coord center = new Coord(DeltaChunk.getClientShadowWorld(), 0, 0, 0);

    @Override public void spawnParticle(String var1, double var2, double var4, double var6, double var8, double var10, double var12) { }

    @Override public void onEntityCreate(Entity entity) { }

    @Override public void onEntityDestroy(Entity entity) { }

    @Override public void playSound(String var1, double var2, double var4, double var6, float var8, float var9) { }

    @Override public void playRecord(String var1, int var2, int var3, int var4) { }

    @Override public void playAuxSFX(EntityPlayer var1, int var2, int var3, int var4, int var5, int var6) { }

    @Override
    public void markBlockRangeForRenderUpdate(int lx, int ly, int lz, int hx, int hy, int hz) {
        markBlocksForUpdate(lx, ly, lz, hx, hy, hz);
    }

    @Override
    public void markBlockForUpdate(int x, int y, int z) {
        markBlocksForUpdate(x, y, z, x, y, z);
    }

    @Override
    public void markBlockForRenderUpdate(int x, int y, int z) {
        markBlocksForUpdate(x, y, z, x, y, z);
    }

    void markBlocksForUpdate(int lx, int ly, int lz, int hx, int hy, int hz) {
        World realClientWorld = DeltaChunk.getClientRealWorld();
        Coord lower = new Coord(null, lx, ly, lz);
        Coord upper = new Coord(null, hx, hy, hz);
        for (IDeltaChunk idc : DeltaChunk.getSlicesInRange(realClientWorld, lx, ly, lz, hx, hy, hz)) {
            DimensionSliceEntity dse = (DimensionSliceEntity) idc;
            if (dse.getCorner().inside(lower, upper) || dse.getFarCorner().inside(lower, upper)) {
                dse.blocksChanged(lx, ly, lz);
                dse.blocksChanged(hx, hy, hz);
            }
        }
    }

    @Override
    public void playSoundToNearExcept(EntityPlayer entityplayer, String s, double d0, double d1, double d2, float f, float f1) { }

    @Override public void destroyBlockPartially(int var1, int var2, int var3, int var4, int var5) { }

    @Override public void broadcastSound(int var1, int var2, int var3, int var4, int var5) { }

    @Override
    public void onStaticEntitiesChanged() {
        // TODO: Implement? We may just render them lame-style
    }
}