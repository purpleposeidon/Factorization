package factorization.util;

import factorization.shared.Core;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import static org.lwjgl.opengl.GL11.glGetError;

public final class RenderUtil {
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
        default: break;
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

    public static TextureAtlasSprite getSprite(ItemStack log) {
        Minecraft mc = Minecraft.getMinecraft();
        Block b = DataUtil.getBlock(log);
        if (b == null) {
            ItemModelMesher itemModelMesher = mc.getRenderItem().getItemModelMesher();
            if (log == null) return itemModelMesher.getItemModel(null).getParticleTexture();
            return itemModelMesher.getParticleIcon(log.getItem());
        }
        IBlockState bs = b.getStateFromMeta(log.getItemDamage());
        return mc.getBlockRendererDispatcher().getBlockModelShapes().getTexture(bs);
    }
}
