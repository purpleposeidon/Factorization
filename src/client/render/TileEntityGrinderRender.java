package factorization.client.render;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_LIGHTING;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glRotatef;
import static org.lwjgl.opengl.GL11.glTranslatef;
import net.minecraft.client.Minecraft;
import net.minecraft.src.PositionTextureVertex;
import net.minecraft.src.RenderEngine;
import net.minecraft.src.Tessellator;
import net.minecraft.src.TexturedQuad;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntitySpecialRenderer;
import net.minecraft.src.Vec3;

import org.lwjgl.opengl.GL11;

import factorization.common.Core;
import factorization.common.RenderingCube;
import factorization.common.RenderingCube.Vector;
import factorization.common.TileEntityGrinder;

public class TileEntityGrinderRender extends TileEntitySpecialRenderer {
    static class DiamondModel {
        TexturedQuad quads[] = new TexturedQuad[4];
        final float near = 2F / 32F, far = 5F / 32F, point = -10F / 32F;
        final float ul = 8F / 16F, vl = 1F / 16F, w = 1F / 16F, h = w / 2F;
        final PositionTextureVertex down = new PositionTextureVertex(Vec3.createVectorHelper(0, point, 0), ul + h, vl + h), //the pointy end
                center = new PositionTextureVertex(Vec3.createVectorHelper(0, 0, 0), 0, 0),
                //numpad directions
                v6 = new PositionTextureVertex(Vec3.createVectorHelper(far, 0, 0), ul + w, vl + w),
                v2 = new PositionTextureVertex(Vec3.createVectorHelper(0, 0, -near), ul, vl + w),
                v4 = new PositionTextureVertex(Vec3.createVectorHelper(-far, 0, 0), ul + w, vl + w),
                v8 = new PositionTextureVertex(Vec3.createVectorHelper(0, 0, near), ul, vl + w);

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

    static DiamondModel diamondModel = new DiamondModel();

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
        RenderEngine re = Minecraft.getMinecraft().renderEngine;
        glDisable(GL_LIGHTING);

        re.bindTexture(re.getTexture("/terrain.png"));
        //XXX TODO FIXME Move to somewhere more efficient
        //(Move the vector stuff out too...)
        RenderingCube frame = new RenderingCube(16 + 6, new RenderingCube.Vector(5, 1, 5), null);
        Tessellator.instance.startDrawingQuads();
        Tessellator.instance.setColorOpaque_F(1, 1, 1);
        GL11.glTranslatef(0, 2F / 16F, 0);
        for (int face = 0; face < 6; face++) {
            for (Vector v : frame.faceVerts(face)) {
                Tessellator.instance.addVertexWithUV(
                        v.x / 16F,
                        v.y / 16F,
                        v.z / 16F,
                        frame.ul + v.u / 256F, frame.vl + v.v / 256F);
            }
        }
        Tessellator.instance.draw();

        for (int i = 0; i < 8; i++) {
            glPushMatrix();
            glRotatef(i * 360 / 8, 0, 1, 0);
            glTranslatef(2.5F / 16F, 0, 0);
            glRotatef(15, 1, 0, 0);
            glRotatef(10, 0, 0, 1);

            diamondModel.render(Tessellator.instance, 1F);
            //drawShard();
            glPopMatrix();
        }

        glEnable(GL_LIGHTING);
        glDisable(GL_BLEND);
    }
}
