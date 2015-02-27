package factorization.ceramics;

import java.util.Random;

import factorization.common.ResourceType;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import factorization.ceramics.TileEntityGreenware.ClayLump;
import factorization.ceramics.TileEntityGreenware.ClayState;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.BlockRenderHelper;
import factorization.shared.Core;
import factorization.shared.FactorizationBlockRender;

public class BlockRenderGreenware extends FactorizationBlockRender {
    static BlockRenderGreenware instance;
    
    public BlockRenderGreenware() {
        instance = this;
        setup();
    }
    
    private boolean texture_init = false;
    public void setup() {
        if (texture_init) {
            return;
        }
    }
    
    private static TileEntityGreenware loader = new TileEntityGreenware();
    
    @Override
    public boolean render(RenderBlocks rb) {
        if (!world_mode) {
            if (is == null) {
                return false;
            }
            GL11.glPushMatrix();
            boolean stand = true;
            boolean rescale = false;
            if (is.hasTagCompound()) {
                loader.loadFromStack(is);
                int minX = 32, minY = 32, minZ = 32;
                int maxX = 0, maxY = 32, maxZ = 32;
                for (ClayLump cl : loader.parts) {
                    minX = Math.min(minX, cl.minX);
                    minY = Math.min(minY, cl.minY);
                    minZ = Math.min(minZ, cl.minZ);
                    maxX = Math.max(maxX, cl.maxX);
                    maxY = Math.max(maxY, cl.maxY);
                    maxZ = Math.max(maxZ, cl.maxZ);
                    int min = Math.min(Math.min(minX, minY), minZ);
                    int max = Math.max(Math.max(maxX, maxY), maxZ);
                    if (min < 16 || max > 32) {
                        rescale = true;
                        break;
                    }
                }
                if (rescale) {
                    float scale = 1F/3F;
                    GL11.glScalef(scale, scale, scale);
                }
                GL11.glPushAttrib(GL11.GL_LIGHTING_BIT);
                GL11.glDisable(GL11.GL_LIGHTING);
                float d = -0.5F; // Odd.
                Tessellator.instance.setTranslation(d, d, d);
                renderDynamic(loader);
                Tessellator.instance.setTranslation(0, 0, 0);
                GL11.glPopAttrib();
                ClayState cs = loader.getState();
                stand = cs == ClayState.WET;
            } else {
                setupRenderGenericLump().renderForInventory(rb);
            }
            GL11.glPopMatrix();
            if (stand) {
                setupRenderStand().renderForInventory(rb);
            }
            return false;
        }
        TileEntityGreenware gw = (TileEntityGreenware) te;
        if (gw == null) {
            return false;
        }
        if (world_mode) {
            Tessellator.instance.setBrightness(Core.registry.factory_block.getMixedBrightnessForBlock(w, x, y, z));
        }
        ClayState state = gw.getState();
        if (state == ClayState.WET) {
            BlockRenderHelper block = setupRenderStand();
            block.render(rb, x, y, z);
        }
        if (!gw.canEdit()) {
            renderStatic(gw);
        }
        gw.shouldRenderTesr = state == ClayState.WET;
        return true;
    }
    
    private static Random rawMimicRandom = new Random();
    
    int getColor(ClayLump rc) {
        if (rc.raw_color == -1) {
            if (rc.icon_id == Core.registry.resource_block && rc.icon_md == ResourceType.BISQUE.md) {
                rc.raw_color = 0;
                return rc.raw_color;
            }
            //Get the raw color, possibly making something up
            rawMimicRandom.setSeed(rc.icon_id.getUnlocalizedName().hashCode() << 4 + rc.icon_md);
            int c = 0;
            for (int i = 0; i < 3; i++) {
                c <<= 8;
                c += (rawMimicRandom.nextInt(0x80) + 40);
            }
            rc.raw_color = c;
        }
        return rc.raw_color;
    }
    
    private boolean spammed = false;
    
    void renderToTessellator(TileEntityGreenware greenware) {
        BlockRenderHelper block = BlockRenderHelper.instance;
        ClayState state = greenware.getState();
        if (state != ClayState.HIGHFIRED) {
            switch (state) {
            case WET: block.useTexture(Blocks.clay.getBlockTextureFromSide(0)); break;
            case DRY: block.useTexture(BlockIcons.ceramics$dry); break;
            case BISQUED: block.useTexture(BlockIcons.ceramics$bisque); break;
            case UNFIRED_GLAZED: block.useTexture(BlockIcons.ceramics$rawglaze); break;
            default: block.useTexture(BlockIcons.error); break;
            }
        }
        boolean colors_changed = false;
        int total = greenware.parts.size();
        double d = 1.0/(4096.0*total);
        int offset = -total/2;
        int rci = -1;
        for (ClayLump rc : greenware.parts) {
            rci++;
            if (state == ClayState.HIGHFIRED) {
                Block it = rc.icon_id;
                if (it == null || it == Blocks.air) {
                    block.useTexture(BlockIcons.error);
                } else {
                    for (int i = 0; i < 6; i++) {
                        int useIIcon = i;
                        if (rc.icon_side == -1) {
                            block.setTexture(i, it.getIcon(useIIcon, rc.icon_md));
                        } else {
                            useIIcon = rc.icon_side;
                            block.useTexture(it.getIcon(useIIcon, rc.icon_md));
                        }
                        int color = 0xFFFFFF; 
                        if (greenware.getWorldObj() != null) {
                            try {
                                color = it.colorMultiplier(greenware.getWorldObj(), greenware.xCoord, greenware.yCoord, greenware.zCoord);
                            } catch (Throwable t) {
                                if (!spammed) {
                                    spammed = true;
                                    Core.logWarning("%s: could not get a Blocks.colorMultiplier from %s", greenware.getCoord(), it);
                                    t.printStackTrace();
                                }
                            }
                        } else {
                            color = it.getRenderColor(useIIcon);
                        }
                        if (color != 0xFFFFFF) {
                            colors_changed = true;
                            if (rc.icon_side == -1) {
                                block.setColor(i, color);
                            } else {
                                block.setColor(color);
                            }
                        }
                        if (rc.icon_side != -1) {
                            break;
                        }
                    }
                }
            }
            if (state == ClayState.UNFIRED_GLAZED) {
                int color = getColor(rc);
                if (color == 0) {
                    block.useTexture(BlockIcons.ceramics$bisque);
                    block.setColor(0xFFFFFFFF);
                } else {
                    block.useTexture(BlockIcons.ceramics$rawglaze);
                    block.setColor(color);
                }
                colors_changed = true;
            }
            rc.toBlockBounds(block);
            block.beginWithMirroredUVs();
            block.rotateMiddle(rc.quat);
            if (greenware.front != ForgeDirection.UNKNOWN && greenware.rotation > 0) {
                block.rotateCenter(greenware.rotation_quat);
            }
            float o = (float) ((offset + rci)*d);
            block.translate(o, o, o);
            block.renderRotated(Tessellator.instance, x, y, z);
        }
        if (colors_changed) {
            block.resetColors();
        }
    }
    
    void renderDynamic(TileEntityGreenware greenware) {
        Tessellator.instance.startDrawingQuads();
        renderToTessellator(greenware);
        Tessellator.instance.draw();
    }
    
    void renderStatic(TileEntityGreenware greenware) {
        renderToTessellator(greenware);
    }
    
    BlockRenderHelper setupRenderStand() {
        BlockRenderHelper block = BlockRenderHelper.instance;
        block.useTexture(BlockIcons.ceramics$stand);
        block.setBlockBounds(0, 0, 0, 1, 1F/8F, 1);
        return block;
    }
    
    BlockRenderHelper setupRenderGenericLump() {
        BlockRenderHelper block = BlockRenderHelper.instance;
        block.useTexture(Blocks.clay.getBlockTextureFromSide(0));
        block.setBlockBounds(3F/16F, 1F/8F, 3F/16F, 13F/16F, 7F/8F, 13F/16F);
        return block;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.CERAMIC;
    }

}
