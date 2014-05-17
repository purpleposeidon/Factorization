package factorization.docs;

import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import factorization.api.Coord;
import factorization.shared.Core;
import factorization.shared.FzUtil;
import factorization.weird.TileEntityDayBarrel;

public class FigurePage extends AbstractPage {
    DocWorld figure;
    double rotationX = 90+45, rotationY = 45;
    int display_list = -1;
    
    FigurePage(DocWorld figure) {
        this.figure = figure;
        eyeball = FzUtil.makePlayer(new Coord(figure, 0, 0, 0), "drawFigurePage");
    }

    double origRotationX, origRotationY;
    
    @Override
    void mouseDragStart() {
        origRotationX = rotationX;
        origRotationY = rotationY;
    }
    
    @Override
    void mouseDrag(int dx, int dy) {
        rotationX = origRotationX + dy;
        rotationY = origRotationY - dx;
    }
    
    
    WorldRenderer wr = null;
    
    EntityLivingBase eyeball;
    
    @Override
    void draw(DocViewer doc, int ox, int oy) {
        FzUtil.checkGLError("FigurePage -- before render");
        if (wr == null) {
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            wr = new WorldRenderer(figure, new ArrayList(), 0, 0, 0, getRenderList());
            wr.needsUpdate = true;
            wr.updateRenderer(eyeball);
            GL11.glPopAttrib();
            FzUtil.checkGLError("FigurePage -- update worldrenderer");
        }
        wr.isInFrustum = true;
        doc.mc.renderEngine.bindTexture(Core.blockAtlas);
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glPushMatrix();
        GL11.glTranslatef(ox, oy, 200);
        
        GL11.glTranslated(doc.getPageWidth(0)/2, doc.getPageHeight(0)/2, 0);
        
        float diag = figure.diagonal;
        float s = doc.getPageWidth(0)/2/diag;
        GL11.glScalef(s, s, s);
        
        
        GL11.glScalef(1, -1, 1);
        GL11.glRotatef(180, 0, 0, 1);
        
        GL11.glRotated(rotationX, 1, 0, 0);
        GL11.glRotated(rotationY, 0, 1, 0);
        
        s = -diag/2;
        GL11.glTranslated(s, s, s);
        
        if (Minecraft.isAmbientOcclusionEnabled()) {
            GL11.glShadeModel(GL11.GL_SMOOTH);
        }
        
        
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glDisable(GL11.GL_LIGHTING);
        for (int i = 0; i < 2; i++) {
            if (i == 1) {
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glEnable(GL11.GL_BLEND);
            }
            GL11.glCallList(wr.getGLCallListForPass(i));
        }
        GL11.glPopAttrib();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        TileEntityRendererDispatcher ter = TileEntityRendererDispatcher.instance;
        ter.staticPlayerX = ter.staticPlayerY = ter.staticPlayerZ = 0;
        for (TileEntity te : figure.tileEntities) {
            ter.renderTileEntityAt(te, te.xCoord, te.yCoord, te.zCoord, 0);
        }
        GL11.glPopAttrib();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        RenderManager rm = RenderManager.instance;
        //rm.renderPosX = rm.renderPosY = rm.renderPosZ = 0;
        for (Entity ent : figure.entities) {
            double x = ent.posX - figure.orig.x;
            double y = ent.posY - figure.orig.y;
            double z = ent.posZ - figure.orig.z;
            rm.renderPosX = ent.posX;
            rm.renderPosY = ent.posY;
            rm.renderPosZ = ent.posZ;
            GL11.glPushMatrix();
            GL11.glTranslated(x, y, z);
            //GL11.glTranslated(ent.posX, ent.posY, ent.posZ);
            rm.renderEntitySimple(ent, 0);
            GL11.glPopMatrix();
        }
        GL11.glPopAttrib();
        GL11.glPopMatrix();
        FzUtil.checkGLError("FigurePage -- after rendering everything");
    }
    
    int getRenderList() {
        if (display_list == -1) {
            display_list = GLAllocation.generateDisplayLists(3);
            if (display_list == -1) {
                Core.logWarning("GL display list allocation failed!");
            }
        }
        return display_list;
    }
    
    @Override
    void closed() {
        if (display_list == -1) {
            return;
        }
        GLAllocation.deleteDisplayLists(display_list);
        display_list = -1;
    }
    
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        TileEntityDayBarrel.addFinalizedDisplayList(display_list);
    }
}
