package factorization.client.render;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_LIGHTING;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glRotatef;
import static org.lwjgl.opengl.GL11.glTranslatef;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.client.model.TexturedQuad;
import net.minecraft.client.renderer.RenderEngine;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;
import net.minecraft.util.Vec3;

import org.lwjgl.opengl.GL11;

import factorization.common.BlockIcons;
import factorization.common.BlockRenderHelper;
import factorization.common.Core;
import factorization.common.TileEntityGrinder;

public class TileEntityGrinderRender extends TileEntitySpecialRenderer {
    static class DiamondModel {
        TexturedQuad quads[] = new TexturedQuad[4];
        Icon diamond = Block.blockDiamond.getBlockTextureFromSide(0);
        
        float near = 2F / 32F, far = 5F / 32F, point = -10F / 32F;
        float u_edge = 0; //diamond.getWidth()/16;
        float v_edge = 0; //diamond.getHeight()/16;
        float du1 = diamond.getU1() + u_edge;
        float du2 = diamond.getU2() - u_edge;
        float dv1 = diamond.getV1() + v_edge;
        float dv2 = diamond.getV2() - v_edge;
        float dum = (du1 + du2)/2;
        float dvm = (dv1 + dv2)/2;
        
        PositionTextureVertex down = new PositionTextureVertex(Vec3.createVectorHelper(0, point, 0), dum, dvm), //the pointy end
                //numpad directions
                v6 = new PositionTextureVertex(Vec3.createVectorHelper(far, 0, 0), du2, dv2),
                v2 = new PositionTextureVertex(Vec3.createVectorHelper(0, 0, -near), du1, dv2),
                v4 = new PositionTextureVertex(Vec3.createVectorHelper(-far, 0, 0), du2, dv2),
                v8 = new PositionTextureVertex(Vec3.createVectorHelper(0, 0, near), du1, dv2);

        TexturedQuad makeQuad(PositionTextureVertex a, PositionTextureVertex b) {
            Vec3 va = a.vector3D, vb = b.vector3D;
            Vec3 mid = Vec3.createVectorHelper((va.xCoord + vb.xCoord) / 2, (va.yCoord + vb.yCoord) / 2, (va.zCoord + vb.zCoord) / 2);
            float tmx = (a.texturePositionX + b.texturePositionX) / 2;
            float tmy = (a.texturePositionY + b.texturePositionY) / 2;
            return new TexturedQuad(new PositionTextureVertex[] { down, a, new PositionTextureVertex(mid, tmx, tmy), b });
        }

        public DiamondModel() {
            quads[0] = makeQuad(v2, v6);
            quads[1] = makeQuad(v6, v8);
            quads[2] = makeQuad(v8, v4);
            quads[3] = makeQuad(v4, v2);
        }

        void render(Tessellator tess, float scale) {
            for (TexturedQuad quad : quads) {
                quad.draw(tess, scale);
            }
        }
    }

    static DiamondModel diamondModel = null;
    
    public static void remakeModel() {
        diamondModel = null;
    }

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partial) {
        Core.profileStartRender("grinder");
        TileEntityGrinder grinder = (TileEntityGrinder) te;
        GL11.glPushMatrix();
        GL11.glTranslatef((float) (x + 0.5), (float) (y + 5F / 16F), (float) (z + 0.5));
        GL11.glRotatef(grinder.rotation / 5.0F, 0, 1, 0);
        renderGrindHead();
        GL11.glPopMatrix();
        Core.profileEndRender();
    }

    static void renderGrindHead() {
        if (diamondModel == null) {
            diamondModel = new DiamondModel();
        }
        
        RenderEngine re = Minecraft.getMinecraft().renderEngine;
        glDisable(GL_LIGHTING);

        re.bindTextureFile(Core.texture_file_block);
        //XXX TODO FIXME Move to somewhere more efficient
        //(Move the vector stuff out too...)
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.setColorOpaque_F(1, 1, 1);
        
        //If you find yourself needing to look at an atlas... (just don't forget our favorite TAG)
//		tess.addVertexWithUV(0, 4, 0, 0, 0);
//		tess.addVertexWithUV(0, 4, 10, 0, 1);
//		tess.addVertexWithUV(10, 4, 10, 1, 1);
//		tess.addVertexWithUV(10, 4, 0, 1, 0);
        
        
        GL11.glTranslatef(0, 2F / 16F, 0);
        BlockRenderHelper block = BlockRenderHelper.instance;
        block.useTexture(Block.blockSteel.getBlockTextureFromSide(0));
        block.setBlockBoundsOffset(1F/8F, 7F/16F, 1F/8F);
        //block.setBlockBoundsOffset(0, 0, 0);
        block.begin();
        block.translate(-0.5F, -0.5F, -0.5F);
        block.renderForTileEntity();
        
        Tessellator.instance.draw();

        for (int i = 0; i < 8; i++) {
            glPushMatrix();
            glRotatef(i * 360 / 8, 0, 1, 0);
            glTranslatef(2.5F / 16F, 0, 0);
            glRotatef(15, 1, 0, 0);
            glRotatef(10, 0, 0, 1);

            diamondModel.render(Tessellator.instance, 1F);
            glPopMatrix();
        }

        glEnable(GL_LIGHTING);
        glDisable(GL_BLEND);
    }
}
