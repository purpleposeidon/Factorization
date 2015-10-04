package factorization.artifact;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;

import java.util.Random;

public class RenderBrokenArtifact implements IItemRenderer {
    int W = 8;
    int S = 16 / W;
    int layerCount = 4;
    int[] shuffle = new int[W * W * 2 * layerCount];
    {
        // Construct an "array of (x, y) pairs".
        int index = 0;
        for (int l = 0; l < layerCount; l++) {
            for (int x = 0; x < W; x++) {
                for (int y = 0; y < W; y++) {
                    shuffle[index++] = x;
                    shuffle[index++] = y;
                }
            }
        }
        int elem = shuffle.length / 2;
        int randCount = shuffle.length / 2;
        Random rand = new Random(9);
        for (int n = 0; n < randCount; n++) {
            int s = rand.nextInt(elem);
            int d = rand.nextInt(elem);
            if (rand.nextInt(3) == 0) {
                int swap0 = shuffle[d * 2 + 0];
                int swap1 = shuffle[d * 2 + 1];
                shuffle[d * 2 + 0] = shuffle[s * 2 + 0];
                shuffle[d * 2 + 1] = shuffle[s * 2 + 1];
                shuffle[s * 2 + 0] = swap0;
                shuffle[s * 2 + 1] = swap1;
            } else{
                shuffle[s * 2 + 0] = -1;
                shuffle[s * 2 + 1] = -1;
            }
        }
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return item.hasTagCompound();
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        if (type != ItemRenderType.ENTITY) return false;
        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        ItemStack orig = ItemBrokenArtifact.get(item);
        if (orig == null) return;
        if (type == ItemRenderType.ENTITY) {
            GL11.glDisable(GL11.GL_CULL_FACE);
        }
        IIcon icon = orig.getIconIndex();
        double d = type == ItemRenderType.INVENTORY ? 1.0 : 1.0/16.0;
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        int index = 0;
        for (int loop = 0; loop < layerCount; loop++) {
            for (int x = 0; x < W; x++) {
                for (int y = 0; y < W; y++) {
                    double iu0 = shuffle[index++] * this.S;
                    double iv0 = shuffle[index++] * this.S;
                    if (iu0 == -1) continue;
                    double iu1 = iu0 + this.S;
                    double iv1 = iv0 + this.S;
                    float u0 = icon.getInterpolatedU(iu0);
                    float v0 = icon.getInterpolatedV(iv0);
                    float u1 = icon.getInterpolatedU(iu1);
                    float v1 = icon.getInterpolatedV(iv1);
                    double x0 = x * this.S;
                    double x1 = x0 + this.S;
                    double y0 = y * this.S;
                    double y1 = y0 + this.S;
                    x0 *= d;
                    x1 *= d;
                    y0 *= d;
                    y1 *= d;
                    tess.addVertexWithUV(x0, y0, 0, u0, v0);
                    tess.addVertexWithUV(x0, y1, 0, u0, v1);
                    tess.addVertexWithUV(x1, y1, 0, u1, v1);
                    tess.addVertexWithUV(x1, y0, 0, u1, v0);
                }
            }
        }
        GL11.glEnable(GL11.GL_BLEND);
        //GL11.glDisable(GL11.GL_BLEND);
        //GL11.glDisable(GL11.GL_TEXTURE_2D);
        tess.draw();
        //GL11.glEnable(GL11.GL_TEXTURE_2D);
        if (type == ItemRenderType.ENTITY) {
            GL11.glEnable(GL11.GL_CULL_FACE);
        }
    }
}
