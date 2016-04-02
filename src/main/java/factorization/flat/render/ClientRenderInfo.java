package factorization.flat.render;

import com.google.common.collect.Lists;
import factorization.api.Coord;
import factorization.coremodhooks.IExtraChunkData;
import factorization.flat.FlatChunkLayer;
import factorization.flat.api.IFlatRenderInfo;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.world.chunk.Chunk;
import org.lwjgl.opengl.GL11;

import java.util.List;

public final class ClientRenderInfo implements IFlatRenderInfo {
    int dirty = Math.abs(~0);
    int entitySpawned = 0;
    final List<EntityHack> hacks = Lists.newArrayList();
    final Chunk chunk;

    public ClientRenderInfo(Chunk chunk) {
        this.chunk = chunk;
    }

    @Override
    public void markDirty(Coord at) {
        if (at == null) return;
        int slabY = at.y >> 4;
        if (slabY < 0 || slabY >= 16) return;
        int slabMask = 1 << slabY;
        dirty |= slabMask;
        if ((entitySpawned & slabMask) == 0) {
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
            hacks.add(hack);
        }
    }

    void discardList() {
        for (EntityHack hack : hacks) {
            if (hack.displayList != -1) {
                GLAllocation.deleteDisplayLists(hack.displayList);
                hack.displayList = -1;
            }
            hack.setDead();
        }
        hacks.clear();
    }

    @Override
    public void discard() {
        discardList();
    }

    public void discard(EntityHack hack) {
        if (hack.displayList != -1) {
            GLAllocation.deleteDisplayLists(hack.displayList);
            hack.displayList = -1;
        }
    }

    public void draw(EntityHack hack) {
        if (hack.displayList == -1) return;
        GL11.glCallList(hack.displayList);
    }

    public void update(EntityHack entity, Chunk chunk, FlatChunkLayer layer, int slabY) {
        int slabFlag = 1 << slabY;
        if ((dirty & slabFlag) == 0) return;
        dirty &= ~slabFlag;
        Drawer visitor = new Drawer(this);
        layer.iterateSlab(visitor, slabY);
        visitor.finish(entity);
    }

    public int getList(EntityHack hack) {
        if (hack.displayList == -1) {
            hack.displayList = GLAllocation.generateDisplayLists(1);
        }
        return hack.displayList;
    }
}
