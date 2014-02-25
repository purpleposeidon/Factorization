package factorization.docs;

import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import factorization.shared.Core;
import factorization.shared.FzUtil;
import factorization.weird.TileEntityDayBarrel;

public class FigurePage extends AbstractPage {
    DocWorld figure;
    double rotationX = 90+45, rotationY = 45;
    int display_list = -1;
    
    FigurePage(DocWorld figure) {
        this.figure = figure;
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
    
    @Override
    void draw(DocViewer doc, int ox, int oy) {
        if (wr == null) {
            FzUtil.checkGLError("FigurePage -- before update");
            wr = new WorldRenderer(figure, new ArrayList(), 0, 0, 0, getRenderList());
            wr.needsUpdate = true;
            wr.updateRenderer();
            FzUtil.checkGLError("FigurePage -- update worldrenderer");
        }
        wr.isInFrustum = true;
        doc.mc.renderEngine.bindTexture(Core.blockAtlas);
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
        
        GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT);
        for (int i = 0; i < 2; i++) {
            if (i == 1) {
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glEnable(GL11.GL_BLEND);
            }
            GL11.glCallList(wr.getGLCallListForPass(i));
        }
        GL11.glPopAttrib();
        
        TileEntityRenderer ter = TileEntityRenderer.instance;
        for (TileEntity te : figure.tileEntities) {
            ter.renderTileEntityAt(te, te.xCoord, te.yCoord, te.zCoord, 0);
        }
        
        GL11.glPopMatrix();
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
