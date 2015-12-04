package factorization.util;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import factorization.shared.Core;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.EnumFacing;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import static org.lwjgl.opengl.GL11.glGetError;

public final class RenderUtil {
    @SideOnly(Side.CLIENT)
    private static RenderBlocks rb;

    @SideOnly(Side.CLIENT)
    public static RenderBlocks getRB() {
        if (rb == null) {
            rb = new RenderBlocks();
        }
        rb.blockAccess = Minecraft.getMinecraft().theWorld;
        return rb;
    }

    @SideOnly(Side.CLIENT)
    public static void rotateForDirection(EnumFacing dir) {
        switch (dir) {
        case WEST:
            break;
        case EAST:
            GL11.glRotatef(180, 0, 1, 0);
            break;
        case NORTH:
            GL11.glRotatef(-90, 0, 1, 0);
            break;
        case SOUTH:
            GL11.glRotatef(90, 0, 1, 0);
            break;
        case UP:
            GL11.glRotatef(-90, 0, 0, 1);
            break;
        case DOWN:
            GL11.glRotatef(90, 0, 0, 1);
            break;
        case UNKNOWN: break;
        }
    }

    @SideOnly(Side.CLIENT)
    public static boolean checkGLError(String op) {
        int errSym = glGetError();
        if (errSym != 0) {
            Core.logSevere("GL Error @ " + op);
            Core.logSevere(errSym + ": " + GLU.gluErrorString(errSym));
            return true;
        }
        return false;
    }
}
