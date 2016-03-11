package factorization.flat.render;

import factorization.api.Coord;
import factorization.coremodhooks.IExtraChunkData;
import factorization.flat.FlatChunkLayer;
import factorization.flat.api.Flat;
import factorization.flat.api.IFlatRenderInfo;
import factorization.shared.Core;
import factorization.util.NORELEASE;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import org.lwjgl.opengl.GL11;

public final class ClientRenderInfo implements IFlatRenderInfo {
    int dirty = ~0;
    int entitySpawned = 0;

    @Override
    public void markDirty(Coord at) {
        if (at == null) return;
        int slabY = at.y >> 4;
        if (slabY < 0 || slabY >= 16) return;
        int slabMask = 1 << slabY;
        dirty |= slabMask;
        if ((entitySpawned & slabMask) == 0) {
            Chunk chunk = at.getChunk();
            IExtraChunkData ecd = (IExtraChunkData) chunk;
            FlatChunkLayer flatLayer = ecd.getFlatLayer();
            if (flatLayer.isEmpty()) {
                return;
            }
            if (flatLayer.slabIndex(slabY).set == 0) {
                return;
            }
            entitySpawned |= slabMask;
            EntityHack hack = new EntityHack(chunk, slabY);
            at.w.spawnEntityInWorld(hack);
        }
    }

    int displayList = -1;

    void discardList() {
        if (displayList == -1) return;
        GLAllocation.deleteDisplayLists(displayList);
    }

    @Override
    public void discard() {
        discardList();
    }

    int getList() {
        if (displayList == -1) {
            displayList = GLAllocation.generateDisplayLists(1);
        }
        return displayList;
    }

    public void draw() {
        if (displayList == -1) return;
        GL11.glCallList(displayList);
    }

    public void update(Chunk chunk, FlatChunkLayer layer, int slabY) {
        int slabFlag = 1 << slabY;
        if ((dirty & slabFlag) == 0) return;
        dirty &= ~slabFlag;
        Drawer visitor = new Drawer(this);
        layer.iterateSlab(visitor, slabY);
        visitor.finish();
    }
}
