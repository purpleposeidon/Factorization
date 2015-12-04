package factorization.misc;

import net.minecraft.client.renderer.Tessellator;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class WireframeTessellator extends Tessellator {

    @Override
    protected void reset() {
        super.reset();
        verts = 0;
        faker = false;
    }
    
    boolean faker = false;
    
    @Override
    public void setColorOpaque_I(int val) {
        if (val == -1) {
            faker = true;
        }
        super.setColorOpaque_I(val);
    }

    private double startX, startY, startZ;
    int verts = 0;

    @Override
    public void addVertex(double x, double y, double z) {
        if (drawMode != GL11.GL_QUADS || faker) {
            super.addVertex(x, y, z);
            return;
        }
        int vert = verts % 4;
        verts++;
        super.addVertex(x, y, z);

        if (vert == 0) {
            startX = x;
            startY = y;
            startZ = z;
        } else {
            super.addVertex(x, y, z);
            if (vert == 3) {
                super.addVertex(startX, startY, startZ);
            }
        }
    }
    
    @Override
    public int draw() {
        if (drawMode != GL11.GL_QUADS || faker) return super.draw();
        GL11.glLineWidth(5F);
        drawMode = GL11.GL_LINES;
        return super.draw();
    }
}
