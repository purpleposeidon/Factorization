package factorization.common.astro;

import static org.lwjgl.opengl.GL11.glCallList;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glRotatef;
import static org.lwjgl.opengl.GL11.glScalef;
import static org.lwjgl.opengl.GL11.glTranslatef;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.src.Chunk;
import net.minecraft.src.ChunkCoordinates;
import net.minecraft.src.Entity;
import net.minecraft.src.GLAllocation;
import net.minecraft.src.Render;
import net.minecraft.src.RenderHelper;
import net.minecraft.src.RenderManager;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntityRenderer;
import net.minecraft.src.World;
import net.minecraft.src.WorldRenderer;

import org.lwjgl.util.glu.GLU;

import cpw.mods.fml.common.IScheduledTickHandler;
import cpw.mods.fml.common.TickType;
import factorization.common.Core;


public class RenderWorldEntity extends Render implements IScheduledTickHandler {
    void checkGLError(String op) {
        int var2 = glGetError();

        if (var2 != 0)
        {
            String var3 = GLU.gluErrorString(var2);
            System.out.println("########## GL ERROR ##########");
            System.out.println("@ " + op);
            System.out.println(var2 + ": " + var3);
        }
    }
    
    HashMap<WorldEntity, Long> displayListUseTracker = new HashMap();
    long megatickCount = 0;
    
    void removeOldWorldRender(WorldEntity we) {
        if (we.oldWorldRenderer != null) {
            int displayList = ((WorldRenderer)we.oldWorldRenderer).getGLCallListForPass(0);
            GLAllocation.deleteDisplayLists(displayList);
            displayListUseTracker.remove(displayList);
            we.oldWorldRenderer = null;
        }
    }
    
    
    int nest = 0;
    @Override
    public void doRender(Entity ent, double x, double y, double z, float yaw, float partialTicks) {
        //XXX TODO: We don't need an old world renderer; we can just use WorldRenderer.markDirty()
        WorldEntity we = (WorldEntity) ent;
        if (nest == 0) {
            Core.profileStart("fzwe");
            we.renderCounts++;
            if (we.renderCounts >= 60 || we.isDead) {
                we.discardRenderer();
                we.renderCounts = 0;
            }
        }
        nest++;
        try {
            World subWorld = we.worldObj; // we.wew;
            WorldRenderer wr = (WorldRenderer) we.worldRenderer;
            checkGLError("FZWE before render");
            removeOldWorldRender(we);
            if (we.isDead) {
                return;
            }
            if (wr == null && !we.isDead) {
                int chunkDisplayList = GLAllocation.generateDisplayLists(3);
                checkGLError("FZWE list alloc");
                wr = new WorldRenderer(subWorld, subWorld.loadedTileEntityList, 0, 0, 0, chunkDisplayList);
                wr.needsUpdate = true;
                wr.updateRenderer();
                we.worldRenderer = wr;
                checkGLError("FZWE build");
                we.worldRenderer = wr;
            }
            float s = 1F/2F;
            displayListUseTracker.put(we, megatickCount);
            glPushMatrix();
            glTranslatef((float)x, (float)y, (float)z);
            glRotatef(-10, 0, 1, 0);
            glScalef(s, s, s);
            glColor3f(1, 1, 1);
            wr.isInFrustum = true;
            RenderHelper.disableStandardItemLighting();
            for (int pass = 0; pass < 2; pass++) {
                int displayList = wr.getGLCallListForPass(pass);
                if (displayList >= 0) {
                    loadTexture("/terrain.png");
                    glCallList(displayList);
                }
            }
            RenderHelper.enableStandardItemLighting();
            
            //glRotatef(-45, 1, 0, 0);
            glTranslatef((float)-x, (float)-y, (float)-z);
            glTranslatef((float)we.posX, (float)we.posY, (float)we.posZ);
            //Maybe we should use RenderGlobal.renderEntities ???
            Chunk here = subWorld.getChunkFromBlockCoords(0, 0);
            for (List<Entity> ents : (List<Entity>[]) here.entityLists) {
                for (Entity e : ents) {
                    if (e instanceof WorldEntity && nest >= 2) {
                        continue;
                    }
                    RenderManager.instance.renderEntity(e, partialTicks);
                }
            }
            RenderHelper.enableStandardItemLighting();
            for (TileEntity te : ((Map<ChunkCoordinates, TileEntity>)here.chunkTileEntityMap).values()) {
                TileEntityRenderer.instance.renderTileEntity(te, partialTicks);
            }
            glPopMatrix();
            
            
            checkGLError("FZWE after render");
        } finally {
            nest--;
            if (nest == 0) {
                Core.profileEnd();
            }
        }
    }

    @Override
    public void tickStart(EnumSet<TickType> type, Object... tickData) {
        megatickCount++;
    }

    @Override
    public void tickEnd(EnumSet<TickType> type, Object... tickData) {
        Set<Entry<WorldEntity, Long>> entrySet = displayListUseTracker.entrySet();
        Iterator<Entry<WorldEntity, Long>> it = entrySet.iterator();
        while (it.hasNext()) {
            Entry<WorldEntity, Long> entry = it.next();
            if (entry.getValue() != megatickCount) {
                WorldEntity we = entry.getKey();
                we.discardRenderer();
                removeOldWorldRender(we);
                it.remove();
            }
        }
    }

    EnumSet<TickType> renderTicks = EnumSet.of(TickType.RENDER);
    @Override
    public EnumSet<TickType> ticks() {
        return renderTicks;
    }

    @Override
    public String getLabel() {
        return "fzweRenderDealloc";
    }

    @Override
    public int nextTickSpacing() {
        return 20*5; //every 5 seconds
    }
}
