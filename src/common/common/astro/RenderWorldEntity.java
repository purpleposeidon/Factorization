package factorization.common.astro;

import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import net.minecraft.client.Minecraft;
import net.minecraft.src.Entity;
import net.minecraft.src.GLAllocation;
import net.minecraft.src.Render;
import net.minecraft.src.World;
import net.minecraft.src.WorldRenderer;


public class RenderWorldEntity extends Render {
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
    
    @Override
    public void doRender(Entity ent, double x, double y, double z, float yaw, float partialTicks) {
        WorldEntity we = (WorldEntity) ent;
        we.oldWorldRenderer = we.worldRenderer;
        we.worldRenderer = null;
        World subWorld = we.wew;
        WorldRenderer wr = (WorldRenderer) we.worldRenderer;
        checkGLError("Before FZWE render");
        if (we.oldWorldRenderer != null) {
            GLAllocation.deleteDisplayLists(((WorldRenderer)we.oldWorldRenderer).getGLCallListForPass(0));
            we.oldWorldRenderer = null;
        }
        if (wr == null) {
            int chunkDisplayList = GLAllocation.generateDisplayLists(3);
            checkGLError("FZWE list alloc");
            wr = new WorldRenderer(we.wew, subWorld.loadedTileEntityList, 0, 0, 0, chunkDisplayList);
            wr.needsUpdate = true;
            checkGLError("pre-build");
            wr.updateRenderer();
            we.worldRenderer = wr;
            checkGLError("FZWE build");
        }
        glPushMatrix();
        glTranslatef((float)x, (float)y + 1, (float)z);
        //glRotatef(25, 0, 1, 0);
        float s = 1F/8F;
        //glScalef(s, s, s);
        glColor3f(1, 1, 1);
        wr.isInFrustum = true;
        for (int pass = 0; pass < 2; pass++) {
            int displayList = wr.getGLCallListForPass(pass);
            if (displayList >= 0) {
                loadTexture("/terrain.png");
                glCallList(displayList);
            }
        }
        glPopMatrix();
        checkGLError("After FZWE render");
    }

}
