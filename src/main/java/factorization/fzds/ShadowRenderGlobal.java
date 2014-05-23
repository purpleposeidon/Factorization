package factorization.fzds;

import java.util.Iterator;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraft.world.IWorldAccess;
import net.minecraft.world.World;
import factorization.api.Coord;
import factorization.fzds.api.IDeltaChunk;
import factorization.shared.FzUtil;

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
    public void markBlockForUpdate(int var1, int var2, int var3) {
        markBlocksForUpdate(var1 - 1, var2 - 1, var3 - 1, var1 + 1, var2 + 1, var3 + 1);
    }

    @Override
    public void markBlockForRenderUpdate(int var1, int var2, int var3) {
        markBlocksForUpdate(var1 - 1, var2 - 1, var3 - 1, var1 + 1, var2 + 1, var3 + 1);
    }

    @Override
    public void markBlockRangeForRenderUpdate(int var1, int var2, int var3,
            int var4, int var5, int var6) {
        markBlocksForUpdate(var1 - 1, var2 - 1, var3 - 1, var4 + 1, var5 + 1, var6 + 1);
    }
    
    void markBlocksForUpdate(int lx, int ly, int lz, int hx, int hy, int hz) {
        //Could this be more efficient?
        World realClientWorld = DeltaChunk.getClientRealWorld();
        Iterator<IDeltaChunk> it = DeltaChunk.getSlices(realClientWorld).iterator();
        while (it.hasNext()) {
            DimensionSliceEntity dse = (DimensionSliceEntity) it.next();
            if (dse.isDead) {
                it.remove(); //shouldn't happen. Keeping it anyways.
                continue;
            }
            
            Coord near = dse.getCorner(), far = dse.getFarCorner();
            if (FzUtil.intersect(near.x, far.x, lx, hx)
                    && FzUtil.intersect(near.y, far.y, ly, hy)
                    && FzUtil.intersect(near.z, far.z, lz, hz)) {
                RenderDimensionSliceEntity.markBlocksForUpdate((DimensionSliceEntity) dse, lx, ly, lz, hx, hy, hz);
                dse.blocksChanged(lx, ly, lz);
                dse.blocksChanged(hx, hy, hz);
            }
        }
    }

    @Override
    public void playSound(String sound, double x, double y, double z, float volume, float pitch) {
        Vec3 realCoords = DeltaChunk.shadow2nearestReal(Minecraft.getMinecraft().thePlayer, x, y, z);
        if (realCoords == null) {
            return;
        }
        realWorld.playSound(realCoords.xCoord, realCoords.yCoord, realCoords.zCoord, sound, volume, pitch, false);
    }
    
    @Override
    public void playSoundToNearExcept(EntityPlayer entityplayer, String s, double d0, double d1, double d2, float f, float f1) { }

    @Override
    public void spawnParticle(String particle, double x, double y, double z, double vx, double vy, double vz) {
        Vec3 realCoords = DeltaChunk.shadow2nearestReal(Minecraft.getMinecraft().thePlayer, x, y, z);
        if (realCoords == null) {
            return;
        }
        realWorld.spawnParticle(particle, realCoords.xCoord, realCoords.yCoord, realCoords.zCoord, vx, vy, vz);
    }
    
    @Override
    public void onEntityCreate(Entity entity) {
        Minecraft.getMinecraft().renderGlobal.onEntityCreate(entity);
    }
    
    @Override
    public void onEntityDestroy(Entity entity) {
        Minecraft.getMinecraft().renderGlobal.onEntityDestroy(entity);
    }

    @Override
    public void playRecord(String recordName, int x, int y, int z) {
        Vec3 realCoords = DeltaChunk.shadow2nearestReal(Minecraft.getMinecraft().thePlayer, x, y, z);
        if (realCoords == null) {
            return;
        }
        Minecraft.getMinecraft().renderGlobal.playRecord(recordName, (int)realCoords.xCoord, (int)realCoords.yCoord, (int)realCoords.zCoord);
    }

    @Override
    public void broadcastSound(int var1, int var2, int var3, int var4,
            int var5) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void playAuxSFX(EntityPlayer var1, int var2, int var3, int var4,
            int var5, int var6) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void destroyBlockPartially(int var1, int var2, int var3,
            int var4, int var5) {
        // TODO Auto-generated method stub
        //Prooooobably not.
    }
    
}