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
    int layerCount = 2;
    byte[][] shuffles = new byte[10][];

    {
        for (int i = 0; i < shuffles.length; i++) {
            shuffles[i] = makeShuffle(i);
        }
    }

    byte[] makeShuffle(int seed) {
        byte[] shuffle = new byte[W * W * 2 * layerCount];
        Random rand = new Random(seed);
        // Construct an "array of (x, y) pairs".
        int index = 0;
        for (int layer = 0; layer < layerCount; layer++) {
            int start = index;
            for (byte x = 0; x < W; x++) {
                for (byte y = 0; y < W; y++) {
                    shuffle[index++] = x;
                    shuffle[index++] = y;
                }
            }
            int end = index - 2;
            start /= 2;
            end /= 2;
            int len = (end - start);
            if (layer == 0) {
                int randCount = len / 2;
                for (int n = 0; n < randCount; n++) {
                    int s = start + rand.nextInt(len);
                    shuffle[s * 2 + 0] = -1;
                    shuffle[s * 2 + 1] = -1;
                }
            } else {
                int randCount = len / 2;
                for (int n = 0; n < randCount; n++) {
                    int s = start + rand.nextInt(len);
                    int d = start + rand.nextInt(len);
                    byte swap0 = shuffle[d * 2 + 0];
                    byte swap1 = shuffle[d * 2 + 1];
                    shuffle[d * 2 + 0] = shuffle[s * 2 + 0];
                    shuffle[d * 2 + 1] = shuffle[s * 2 + 1];
                    shuffle[s * 2 + 0] = swap0;
                    shuffle[s * 2 + 1] = swap1;
                }
            }
        }

        return shuffle;
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
        Tessellator tess = Tessellator.instance;
        if (type == ItemRenderType.ENTITY) {
            GL11.glDisable(GL11.GL_CULL_FACE);
        } else if (type == ItemRenderType.EQUIPPED) {
            GL11.glPushMatrix();
            GL11.glScaled(-1, -1, -1);
            float posterForwardHack = 1.1F / 32F;
            GL11.glTranslated(-1, -1, posterForwardHack);
        }
        IIcon icon = orig.getIconIndex();
        double d = type == ItemRenderType.INVENTORY ? 1.0 : 1.0/16.0;
        tess.startDrawingQuads();
        int index = 0;
        int shuffleIndex = Math.abs(item.getItemDamage()) % shuffles.length;
        byte shuffle[] = shuffles[shuffleIndex]; // Or we could generate it on the fly?
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
        GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        tess.draw();
        GL11.glPopAttrib();
        if (type == ItemRenderType.ENTITY) {
            GL11.glEnable(GL11.GL_CULL_FACE);
        } else if (type == ItemRenderType.EQUIPPED) {
            GL11.glPopMatrix();
        }
    }
}
