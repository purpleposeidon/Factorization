package factorization.oreprocessing;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_LIGHTING;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glRotatef;
import static org.lwjgl.opengl.GL11.glTranslatef;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.client.model.TexturedQuad;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.Vec3;

import org.lwjgl.opengl.GL11;

import factorization.shared.BlockRenderHelper;
import factorization.shared.Core;

public class TileEntityGrinderRender extends TileEntitySpecialRenderer {
    public static class DiamondModel {
        TexturedQuad quads[] = new TexturedQuad[4];
        IIcon diamond = Blocks.diamond_block.getBlockTextureFromSide(0);
        
        float near = 2F / 32F, far = 5F / 32F, point = -18F / 32F;
        float u_edge = 0; //diamond.getWidth()/16;
        float v_edge = 0; //diamond.getHeight()/16;
        float du1 = diamond.getInterpolatedU(6);
        float du2 = diamond.getInterpolatedU(16 - 6);
        float dv1 = diamond.getInterpolatedV(0);
        float dv2 = diamond.getInterpolatedV(15.75);
        float dum = (du1 + du2)/2;
        float dvm = (dv1 + dv2)/2;
        
        PositionTextureVertex down = new PositionTextureVertex(Vec3.createVectorHelper(near/2, point, far/-3), dum, dvm), //the pointy end
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

    public static DiamondModel diamondModel = null;
    
    public static void remakeModel() {
        diamondModel = null;
    }
    
    public float interp(float oldValue, float newValue, float partial) {
        return oldValue*(1 - partial) + newValue*partial;
    }

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partial) {
        Core.profileStartRender("grinder");
        TileEntityGrinder grinder = (TileEntityGrinder) te;
        GL11.glPushMatrix();
        GL11.glTranslatef((float) (x + 0.5), (float) (y + 5F / 16F), (float) (z + 0.5));
        GL11.glRotatef(interp(grinder.prev_rotation, grinder.rotation, partial) / 5.0F, 0, 1, 0);
        renderGrindHead();
        GL11.glPopMatrix();
        Core.profileEndRender();
    }

    public static void renderGrindHead() {
        if (diamondModel == null) {
            diamondModel = new DiamondModel();
        }
        
        
        TextureManager re = Minecraft.getMinecraft().renderEngine;
        glDisable(GL_LIGHTING);

        re.bindTexture(Core.blockAtlas);
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
        block.useTexture(Blocks.blockIron.getBlockTextureFromSide(0));
        float e = 1F/8F;
        block.setBlockBounds(e, 7F/16F, e, 1 - e, 8F/16F, 1 - e);
        //block.setBlockBoundsOffset(1F/8F, 7F/16F, 1F/8F);
        block.begin();
        block.translate(-0.5F, -0.5F, -0.5F);
        block.renderForTileEntity();
        
        Tessellator.instance.draw();

        for (int i = 0; i < 8; i++) {
            glPushMatrix();
            glRotatef(i * 360 / 8, 0, 1, 0);
            glTranslatef(3.5F / 16F, -1F/32F, 0);
            diamondModel.render(Tessellator.instance, 1F);
            glPopMatrix();
        }

        glEnable(GL_LIGHTING);
        glDisable(GL_BLEND);
    }
}
