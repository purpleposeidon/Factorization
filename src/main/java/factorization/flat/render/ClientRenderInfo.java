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
    boolean dirty = true;
    boolean entitySpawned = false;

    @Override
    public void markDirty(Coord at) {
        dirty = true;
        if (!entitySpawned && at != null) {
            Chunk chunk = at.getChunk();
            IExtraChunkData ecd = (IExtraChunkData) chunk;
            if (ecd.getFlatLayer().isEmpty()) {
                return;
            }
            entitySpawned = true;
            EntityHack hack = new EntityHack(chunk);
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

    public void update(Chunk chunk, FlatChunkLayer layer) {
        //if (NORELEASE.on && Core.dev_environ) dirty = true;
        if (!dirty) return;
        dirty = false;
        Drawer visitor = new Drawer(this);
        layer.iterate(visitor);
        visitor.finish();
    }
}
